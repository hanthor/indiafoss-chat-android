/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.impl

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SendHandle
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.TransactionId
import io.element.android.libraries.matrix.api.room.SendQueueUpdate
import io.element.android.libraries.matrix.api.timeline.item.event.LocalEventSendState
import io.element.android.libraries.outbox.api.Outbox
import io.element.android.libraries.outbox.api.OutboxContentRef
import io.element.android.libraries.outbox.api.OutboxId
import io.element.android.libraries.outbox.api.OutboxObservation
import io.element.android.libraries.outbox.api.OutboxRecord
import io.element.android.libraries.outbox.api.OutboxRetryResult
import io.element.android.libraries.outbox.api.OutboxRoute
import io.element.android.libraries.outbox.api.OutboxSignal
import io.element.android.libraries.outbox.api.OutboxState
import io.element.android.libraries.outbox.api.OutboxStateMachine
import io.element.android.libraries.outbox.api.OutboxStore
import io.element.android.services.toolbox.api.systemclock.SystemClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.UUID

/**
 * Grace period during which a pending record with no matching SDK echo is not judged: the send may
 * simply not have produced its echo yet.
 */
const val OUTBOX_RECONCILE_GRACE_MILLIS = 10_000L

/**
 * The SDK chooses the transaction id and only reports it through `NewLocalEvent`. A queued record
 * older than this is no longer allowed to claim an echo, so an unrelated later send (a reaction, a
 * poll) cannot be mistaken for it.
 */
const val OUTBOX_CORRELATION_WINDOW_MILLIS = 30_000L

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultOutbox(
    private val store: OutboxStore,
    private val clock: SystemClock,
) : Outbox {
    private val tag = "Outbox"
    private val mutex = Mutex()

    override suspend fun send(
        sessionId: SessionId,
        roomId: RoomId,
        contentRef: OutboxContentRef,
        dispatch: suspend () -> Result<Unit>,
    ): OutboxRecord {
        val now = clock.epochMillis()
        val record = OutboxRecord(
            id = OutboxId(UUID.randomUUID().toString()),
            sessionId = sessionId,
            roomId = roomId,
            contentRef = contentRef,
            transactionIds = emptyMap(),
            createdAtMillis = now,
            updatedAtMillis = now,
            attempts = 1,
            state = OutboxState.Queued,
        )
        // Written before any network call, so the intent survives process death.
        mutex.withLock { store.insert(record) }
        Timber.tag(tag).d("${record.id} queued in $roomId as $sessionId (${contentRef.kind})")
        return dispatch().fold(
            onSuccess = { store.get(record.id) ?: record },
            onFailure = { cause ->
                Timber.tag(tag).w(cause, "${record.id} never reached the send queue")
                mutex.withLock {
                    val current = store.get(record.id) ?: record
                    applyLocked(current, listOf(OutboxSignal.RouteError(reason = cause.javaClass.simpleName, definitive = true)))
                }
            },
        )
    }

    override suspend fun onSendQueueUpdate(sessionId: SessionId, roomId: RoomId, update: SendQueueUpdate) {
        when (update) {
            is SendQueueUpdate.NewLocalEvent -> claimEcho(sessionId, roomId, update.transactionId)
            is SendQueueUpdate.SentEvent -> applyToTransaction(sessionId, roomId, update.transactionId, OutboxSignal.RouteSent(update.eventId))
            is SendQueueUpdate.SendError -> applyToTransaction(
                sessionId = sessionId,
                roomId = roomId,
                transactionId = update.transactionId,
                // The SDK does not say whether the request reached the server: not definitive.
                signal = OutboxSignal.RouteError(reason = "send_queue_error", definitive = false),
            )
            is SendQueueUpdate.RetrySendingEvent -> applyToTransaction(
                sessionId = sessionId,
                roomId = roomId,
                transactionId = update.transactionId,
                signal = OutboxSignal.RouteRetrying,
                attemptsDelta = 1,
            )
            is SendQueueUpdate.CancelledLocalEvent -> applyToTransaction(sessionId, roomId, update.transactionId, OutboxSignal.RouteCancelled)
            is SendQueueUpdate.ReplacedLocalEvent,
            is SendQueueUpdate.MediaUpload -> Unit
        }
    }

    override suspend fun reconcile(
        sessionId: SessionId,
        roomId: RoomId,
        observations: List<OutboxObservation>,
        nowMillis: Long,
    ) {
        // An empty timeline is most likely one that has not loaded yet: judging against it would
        // wrongly mark everything uncertain.
        if (observations.isEmpty()) return
        val byTransaction = observations.mapNotNull { observation -> observation.transactionId?.let { it.value to observation } }.toMap()
        val byEvent = observations.mapNotNull { observation -> observation.eventId?.let { it.value to observation } }.toMap()
        mutex.withLock {
            store.getAll(sessionId, roomId).forEach { record ->
                val echo = record.matrixTransactionId?.let { byTransaction[it.value] }
                val knownEventId = record.state.eventIdOrNull
                val remoteReaders = knownEventId?.let { byEvent[it.value] }?.readBy
                val signals = when {
                    echo != null -> echo.toSignals()
                    knownEventId != null && remoteReaders != null -> remoteReaders.map { OutboxSignal.ReadReceipt(knownEventId, it) }
                    record.state.isPending && record.createdAtMillis + OUTBOX_RECONCILE_GRACE_MILLIS <= nowMillis -> listOf(OutboxSignal.AckLost)
                    else -> emptyList()
                }
                applyLocked(record, signals, nowMillis)
            }
        }
    }

    override suspend fun retry(id: OutboxId, sendHandle: SendHandle?): OutboxRetryResult {
        val record = store.get(id) ?: return OutboxRetryResult.NotFound
        if (!record.state.isRetryable) return OutboxRetryResult.NotRetryable
        if (sendHandle == null || record.matrixTransactionId == null) {
            Timber.tag(tag).w("$id has no route handle: a retry would be a new send, refusing")
            return OutboxRetryResult.NoRouteHandle
        }
        // The SDK retries the very same queued request, so the transaction id is unchanged.
        return sendHandle.retry().fold(
            onSuccess = {
                mutex.withLock {
                    val current = store.get(id) ?: return OutboxRetryResult.NotFound
                    applyLocked(current, listOf(OutboxSignal.RetryRequested), attemptsDelta = 1)
                }
                OutboxRetryResult.Retried
            },
            onFailure = { cause ->
                Timber.tag(tag).w(cause, "$id retry refused by the route")
                OutboxRetryResult.RouteRefused(cause.javaClass.simpleName)
            },
        )
    }

    override fun observe(sessionId: SessionId, roomId: RoomId): Flow<List<OutboxRecord>> = store.observe(sessionId, roomId)

    /** The oldest recent record still waiting for its echo owns the transaction id the SDK just chose. */
    private suspend fun claimEcho(sessionId: SessionId, roomId: RoomId, transactionId: TransactionId) {
        mutex.withLock {
            val now = clock.epochMillis()
            val waiting = store.getAll(sessionId, roomId).firstOrNull { record ->
                record.state == OutboxState.Queued &&
                    record.matrixTransactionId == null &&
                    record.createdAtMillis + OUTBOX_CORRELATION_WINDOW_MILLIS > now
            } ?: return
            val next = waiting.copy(
                transactionIds = waiting.transactionIds + (OutboxRoute.MATRIX_SEND_QUEUE to transactionId),
                state = OutboxStateMachine.next(waiting.state, OutboxSignal.RouteAccepted),
                updatedAtMillis = now,
            )
            store.update(next)
            Timber.tag(tag).d("${next.id} locally accepted as $transactionId")
        }
    }

    private suspend fun applyToTransaction(
        sessionId: SessionId,
        roomId: RoomId,
        transactionId: TransactionId,
        signal: OutboxSignal,
        attemptsDelta: Int = 0,
    ) {
        mutex.withLock {
            val record = store.getAll(sessionId, roomId).firstOrNull { it.matrixTransactionId == transactionId } ?: return
            applyLocked(record, listOf(signal), attemptsDelta = attemptsDelta)
        }
    }

    /** Must be called with [mutex] held. Persists only when something changed. */
    private suspend fun applyLocked(
        record: OutboxRecord,
        signals: List<OutboxSignal>,
        nowMillis: Long = clock.epochMillis(),
        attemptsDelta: Int = 0,
    ): OutboxRecord {
        val nextState = signals.fold(record.state) { state, signal -> OutboxStateMachine.next(state, signal) }
        if (nextState == record.state && attemptsDelta == 0) {
            return record
        }
        val next = record.copy(
            state = nextState,
            attempts = record.attempts + attemptsDelta,
            updatedAtMillis = nowMillis,
        )
        store.update(next)
        if (nextState != record.state) {
            Timber.tag(tag).d("${record.id} ${OutboxStateMapper.kindOf(record.state)} -> ${OutboxStateMapper.kindOf(nextState)}")
        }
        return next
    }

    private fun OutboxObservation.toSignals(): List<OutboxSignal> = when (val sendState = localSendState) {
        is LocalEventSendState.Sending -> listOf(OutboxSignal.RouteAccepted)
        is LocalEventSendState.Sent -> listOf(OutboxSignal.RouteSent(sendState.eventId)) + readBy.map { OutboxSignal.ReadReceipt(sendState.eventId, it) }
        is LocalEventSendState.Failed -> listOf(sendState.toRouteError())
        null -> {
            val remoteEventId = eventId
            if (remoteEventId != null) {
                listOf(OutboxSignal.RouteSent(remoteEventId)) + readBy.map { OutboxSignal.ReadReceipt(remoteEventId, it) }
            } else {
                listOf(OutboxSignal.RouteAccepted)
            }
        }
    }

    private fun LocalEventSendState.Failed.toRouteError(): OutboxSignal.RouteError = when (this) {
        // The SDK refused locally: the request was never sent, so the outcome is certain.
        LocalEventSendState.Failed.SendingFromUnverifiedDevice,
        is LocalEventSendState.Failed.VerifiedUser,
        is LocalEventSendState.Failed.InvalidMimeType,
        LocalEventSendState.Failed.MissingMediaContent -> OutboxSignal.RouteError(reason = javaClass.simpleName, definitive = true)
        // Anything else may have reached the server: uncertain, not failed.
        is LocalEventSendState.Failed.Unknown -> OutboxSignal.RouteError(reason = "unknown", definitive = false)
    }
}
