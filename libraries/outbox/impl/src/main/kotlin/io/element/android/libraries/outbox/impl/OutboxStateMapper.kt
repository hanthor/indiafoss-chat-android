/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.impl

import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.TransactionId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.outbox.api.OutboxContentKind
import io.element.android.libraries.outbox.api.OutboxContentRef
import io.element.android.libraries.outbox.api.OutboxId
import io.element.android.libraries.outbox.api.OutboxRecord
import io.element.android.libraries.outbox.api.OutboxRoute
import io.element.android.libraries.outbox.api.OutboxState
import io.element.android.libraries.outbox.impl.db.OutboxEntry
import io.element.android.libraries.outbox.impl.db.OutboxRouteTransaction

internal object OutboxStateMapper {
    const val QUEUED = "QUEUED"
    const val LOCALLY_ACCEPTED = "LOCALLY_ACCEPTED"
    const val SERVER_ACCEPTED = "SERVER_ACCEPTED"
    const val DELIVERED = "DELIVERED"
    const val READ = "READ"
    const val FAILED = "FAILED"
    const val UNCERTAIN = "UNCERTAIN"
    const val CANCELLED = "CANCELLED"

    fun kindOf(state: OutboxState): String = when (state) {
        OutboxState.Queued -> QUEUED
        OutboxState.LocallyAccepted -> LOCALLY_ACCEPTED
        is OutboxState.ServerAccepted -> SERVER_ACCEPTED
        is OutboxState.Delivered -> DELIVERED
        is OutboxState.Read -> READ
        is OutboxState.Failed -> FAILED
        OutboxState.Uncertain -> UNCERTAIN
        OutboxState.Cancelled -> CANCELLED
    }

    fun toState(entry: OutboxEntry): OutboxState = when (entry.state) {
        QUEUED -> OutboxState.Queued
        LOCALLY_ACCEPTED -> OutboxState.LocallyAccepted
        SERVER_ACCEPTED -> OutboxState.ServerAccepted(EventId(requireNotNull(entry.eventId)))
        DELIVERED -> OutboxState.Delivered(
            eventId = EventId(requireNotNull(entry.eventId)),
            recipientId = UserId(requireNotNull(entry.counterpartId)),
        )
        READ -> OutboxState.Read(
            eventId = EventId(requireNotNull(entry.eventId)),
            readerId = UserId(requireNotNull(entry.counterpartId)),
        )
        FAILED -> OutboxState.Failed(
            reason = entry.failureReason.orEmpty(),
            retryable = entry.failureRetryable == 1L,
        )
        UNCERTAIN -> OutboxState.Uncertain
        CANCELLED -> OutboxState.Cancelled
        // A state written by a newer version of the app: do not pretend to know the outcome.
        else -> OutboxState.Uncertain
    }
}

internal fun OutboxRecord.toEntry(): OutboxEntry {
    val state = state
    return OutboxEntry(
        id = id.value,
        sessionId = sessionId.value,
        roomId = roomId.value,
        contentKind = contentRef.kind.name,
        mediaReference = contentRef.mediaReference,
        createdAt = createdAtMillis,
        updatedAt = updatedAtMillis,
        attempts = attempts.toLong(),
        state = OutboxStateMapper.kindOf(state),
        eventId = state.eventIdOrNull?.value,
        counterpartId = when (state) {
            is OutboxState.Delivered -> state.recipientId.value
            is OutboxState.Read -> state.readerId.value
            else -> null
        },
        failureReason = (state as? OutboxState.Failed)?.reason,
        failureRetryable = (state as? OutboxState.Failed)?.let { if (it.retryable) 1L else 0L },
    )
}

internal fun OutboxRecord.toTransactionEntries(): List<OutboxRouteTransaction> = transactionIds.map { (route, transactionId) ->
    OutboxRouteTransaction(
        outboxId = id.value,
        route = route.name,
        transactionId = transactionId.value,
    )
}

internal fun OutboxEntry.toRecord(transactions: List<OutboxRouteTransaction>): OutboxRecord = OutboxRecord(
    id = OutboxId(id),
    sessionId = UserId(sessionId),
    roomId = RoomId(roomId),
    contentRef = OutboxContentRef(
        kind = OutboxContentKind.entries.firstOrNull { it.name == contentKind } ?: OutboxContentKind.OTHER,
        mediaReference = mediaReference,
    ),
    transactionIds = transactions.mapNotNull { transaction ->
        OutboxRoute.entries.firstOrNull { it.name == transaction.route }?.let { route ->
            route to TransactionId(transaction.transactionId)
        }
    }.toMap(),
    createdAtMillis = createdAt,
    updatedAtMillis = updatedAt,
    attempts = attempts.toInt(),
    state = OutboxStateMapper.toState(this),
)
