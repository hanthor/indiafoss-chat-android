/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.impl

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.TransactionId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.SendQueueUpdate
import io.element.android.libraries.matrix.api.timeline.item.event.LocalEventSendState
import io.element.android.libraries.matrix.test.AN_EVENT_ID
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.A_SESSION_ID
import io.element.android.libraries.matrix.test.A_TRANSACTION_ID
import io.element.android.libraries.matrix.test.A_USER_ID_2
import io.element.android.libraries.matrix.test.core.FakeSendHandle
import io.element.android.libraries.outbox.api.OutboxContentKind
import io.element.android.libraries.outbox.api.OutboxContentRef
import io.element.android.libraries.outbox.api.OutboxId
import io.element.android.libraries.outbox.api.OutboxObservation
import io.element.android.libraries.outbox.api.OutboxRecord
import io.element.android.libraries.outbox.api.OutboxRetryResult
import io.element.android.libraries.outbox.api.OutboxRoute
import io.element.android.libraries.outbox.api.OutboxState
import io.element.android.libraries.outbox.test.InMemoryOutboxStore
import io.element.android.services.toolbox.test.systemclock.FakeSystemClock
import io.element.android.tests.testutils.lambda.lambdaRecorder
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultOutboxTest {
    private val store = InMemoryOutboxStore()
    private val clock = FakeSystemClock(epochMillisResult = 1_000L)
    private val textRef = OutboxContentRef(OutboxContentKind.TEXT)
    private val anotherTransactionId = TransactionId("anotherTransactionId")

    private fun createOutbox(store: InMemoryOutboxStore = this.store) = DefaultOutbox(store = store, clock = clock)

    private suspend fun DefaultOutbox.sendText(): OutboxRecord = send(A_SESSION_ID, A_ROOM_ID, textRef) { Result.success(Unit) }

    private fun echo(
        transactionId: TransactionId? = A_TRANSACTION_ID,
        eventId: EventId? = null,
        sendState: LocalEventSendState? = LocalEventSendState.Sending.Event,
        readBy: List<UserId> = emptyList(),
    ) = OutboxObservation(transactionId = transactionId, eventId = eventId, localSendState = sendState, readBy = readBy)

    @Test
    fun `send persists intent before dispatch and records origin account and room`() = runTest {
        val outbox = createOutbox()
        var seenAtDispatch: OutboxRecord? = null
        val record = outbox.send(A_SESSION_ID, A_ROOM_ID, textRef) {
            seenAtDispatch = store.all.single()
            Result.success(Unit)
        }
        assertThat(seenAtDispatch?.state).isEqualTo(OutboxState.Queued)
        assertThat(record.sessionId).isEqualTo(A_SESSION_ID)
        assertThat(record.roomId).isEqualTo(A_ROOM_ID)
        assertThat(record.contentRef).isEqualTo(textRef)
        assertThat(record.createdAtMillis).isEqualTo(1_000L)
        assertThat(record.attempts).isEqualTo(1)
        assertThat(record.transactionIds).isEmpty()
    }

    @Test
    fun `a dispatch that never reaches the send queue is a retryable failure`() = runTest {
        val outbox = createOutbox()
        val record = outbox.send(A_SESSION_ID, A_ROOM_ID, textRef) { Result.failure(IllegalStateException("boom")) }
        assertThat(record.state).isEqualTo(OutboxState.Failed(reason = "IllegalStateException", retryable = true))
        assertThat(store.get(record.id)?.state).isEqualTo(record.state)
    }

    @Test
    fun `the send queue echo attaches the route transaction id and locally accepts`() = runTest {
        val outbox = createOutbox()
        val record = outbox.sendText()
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.NewLocalEvent(A_TRANSACTION_ID))
        val updated = store.get(record.id)!!
        assertThat(updated.state).isEqualTo(OutboxState.LocallyAccepted)
        assertThat(updated.transactionIds).containsExactly(OutboxRoute.MATRIX_SEND_QUEUE, A_TRANSACTION_ID)
    }

    @Test
    fun `echoes are claimed in order and never by an unrelated later send`() = runTest {
        val outbox = createOutbox()
        val first = outbox.sendText()
        clock.epochMillisResult = 2_000L
        val second = outbox.sendText()
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.NewLocalEvent(A_TRANSACTION_ID))
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.NewLocalEvent(anotherTransactionId))
        assertThat(store.get(first.id)?.matrixTransactionId).isEqualTo(A_TRANSACTION_ID)
        assertThat(store.get(second.id)?.matrixTransactionId).isEqualTo(anotherTransactionId)
        // A third echo (a reaction, say) has no record waiting for it and is ignored.
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.NewLocalEvent(TransactionId("reaction")))
        assertThat(store.all).hasSize(2)
    }

    @Test
    fun `a stale queued record does not claim an echo outside the correlation window`() = runTest {
        val outbox = createOutbox()
        val record = outbox.sendText()
        clock.epochMillisResult = 1_000L + OUTBOX_CORRELATION_WINDOW_MILLIS
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.NewLocalEvent(A_TRANSACTION_ID))
        assertThat(store.get(record.id)?.matrixTransactionId).isNull()
        assertThat(store.get(record.id)?.state).isEqualTo(OutboxState.Queued)
    }

    @Test
    fun `sent event is server accepted and a send error is uncertain`() = runTest {
        val outbox = createOutbox()
        val record = outbox.sendText()
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.NewLocalEvent(A_TRANSACTION_ID))
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.SendError(A_TRANSACTION_ID))
        assertThat(store.get(record.id)?.state).isEqualTo(OutboxState.Uncertain)
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.RetrySendingEvent(A_TRANSACTION_ID))
        assertThat(store.get(record.id)?.state).isEqualTo(OutboxState.LocallyAccepted)
        assertThat(store.get(record.id)?.attempts).isEqualTo(2)
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.SentEvent(A_TRANSACTION_ID, AN_EVENT_ID))
        assertThat(store.get(record.id)?.state).isEqualTo(OutboxState.ServerAccepted(AN_EVENT_ID))
    }

    @Test
    fun `cancelling a pending send is explicit and does not unsend an accepted one`() = runTest {
        val outbox = createOutbox()
        val record = outbox.sendText()
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.NewLocalEvent(A_TRANSACTION_ID))
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.SentEvent(A_TRANSACTION_ID, AN_EVENT_ID))
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.CancelledLocalEvent(A_TRANSACTION_ID))
        assertThat(store.get(record.id)?.state).isEqualTo(OutboxState.ServerAccepted(AN_EVENT_ID))
    }

    @Test
    fun `lost ack - process death after dispatch with no echo left ends uncertain, nothing is resent`() = runTest {
        val outbox = createOutbox()
        val record = outbox.sendText()
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.NewLocalEvent(A_TRANSACTION_ID))
        // Process death: a new outbox over the same store; the SDK timeline shows no echo for our transaction.
        val restarted = createOutbox()
        val now = 1_000L + OUTBOX_RECONCILE_GRACE_MILLIS
        restarted.reconcile(A_SESSION_ID, A_ROOM_ID, listOf(echo(transactionId = TransactionId("someone-else"))), now)
        val after = store.get(record.id)!!
        assertThat(after.state).isEqualTo(OutboxState.Uncertain)
        assertThat(after.attempts).isEqualTo(1)
        assertThat(after.transactionIds).containsExactly(OutboxRoute.MATRIX_SEND_QUEUE, A_TRANSACTION_ID)
    }

    @Test
    fun `reconcile does not judge within the grace period nor against an empty timeline`() = runTest {
        val outbox = createOutbox()
        val record = outbox.sendText()
        outbox.reconcile(A_SESSION_ID, A_ROOM_ID, emptyList(), 1_000L + OUTBOX_RECONCILE_GRACE_MILLIS)
        assertThat(store.get(record.id)?.state).isEqualTo(OutboxState.Queued)
        outbox.reconcile(A_SESSION_ID, A_ROOM_ID, listOf(echo(transactionId = TransactionId("other"))), 1_000L + OUTBOX_RECONCILE_GRACE_MILLIS - 1)
        assertThat(store.get(record.id)?.state).isEqualTo(OutboxState.Queued)
    }

    @Test
    fun `process death mid-send - the SDK still holds the echo, the record stays locally accepted`() = runTest {
        val outbox = createOutbox()
        val record = outbox.sendText()
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.NewLocalEvent(A_TRANSACTION_ID))
        val restarted = createOutbox()
        restarted.reconcile(A_SESSION_ID, A_ROOM_ID, listOf(echo()), 1_000L + OUTBOX_RECONCILE_GRACE_MILLIS)
        assertThat(store.get(record.id)?.state).isEqualTo(OutboxState.LocallyAccepted)
        // The SDK then sends it: server accepted, still the same transaction id.
        restarted.reconcile(A_SESSION_ID, A_ROOM_ID, listOf(echo(sendState = LocalEventSendState.Sent(AN_EVENT_ID))), 20_000L)
        assertThat(store.get(record.id)?.state).isEqualTo(OutboxState.ServerAccepted(AN_EVENT_ID))
        assertThat(store.get(record.id)?.matrixTransactionId).isEqualTo(A_TRANSACTION_ID)
    }

    @Test
    fun `a definitive SDK failure seen after restart is failed, an unknown one stays uncertain`() = runTest {
        val outbox = createOutbox()
        val record = outbox.sendText()
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.NewLocalEvent(A_TRANSACTION_ID))
        val restarted = createOutbox()
        restarted.reconcile(A_SESSION_ID, A_ROOM_ID, listOf(echo(sendState = LocalEventSendState.Failed.Unknown("AN_ERROR"))), 20_000L)
        assertThat(store.get(record.id)?.state).isEqualTo(OutboxState.Uncertain)
        restarted.reconcile(A_SESSION_ID, A_ROOM_ID, listOf(echo(sendState = LocalEventSendState.Failed.SendingFromUnverifiedDevice)), 20_000L)
        assertThat(store.get(record.id)?.state).isEqualTo(OutboxState.Failed(reason = "SendingFromUnverifiedDevice", retryable = false))
    }

    @Test
    fun `receipt after restart - a read receipt on the accepted event is read, and only then`() = runTest {
        val outbox = createOutbox()
        val record = outbox.sendText()
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.NewLocalEvent(A_TRANSACTION_ID))
        // A receipt observed while the route has not accepted anything changes nothing.
        outbox.reconcile(A_SESSION_ID, A_ROOM_ID, listOf(echo(readBy = listOf(A_USER_ID_2))), 20_000L)
        assertThat(store.get(record.id)?.state).isEqualTo(OutboxState.LocallyAccepted)
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.SentEvent(A_TRANSACTION_ID, AN_EVENT_ID))
        val restarted = createOutbox()
        // After restart the echo is gone; the remote event carries the receipt.
        restarted.reconcile(
            sessionId = A_SESSION_ID,
            roomId = A_ROOM_ID,
            observations = listOf(echo(transactionId = null, eventId = AN_EVENT_ID, sendState = null, readBy = listOf(A_USER_ID_2))),
            nowMillis = 30_000L,
        )
        assertThat(store.get(record.id)?.state).isEqualTo(OutboxState.Read(AN_EVENT_ID, A_USER_ID_2))
    }

    @Test
    fun `server acceptance without any receipt never becomes delivered or read`() = runTest {
        val outbox = createOutbox()
        val record = outbox.sendText()
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.NewLocalEvent(A_TRANSACTION_ID))
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.SentEvent(A_TRANSACTION_ID, AN_EVENT_ID))
        outbox.reconcile(A_SESSION_ID, A_ROOM_ID, listOf(echo(transactionId = null, eventId = AN_EVENT_ID, sendState = null)), 30_000L)
        assertThat(store.get(record.id)?.state).isEqualTo(OutboxState.ServerAccepted(AN_EVENT_ID))
    }

    @Test
    fun `retry with the same transaction id goes through the route handle`() = runTest {
        val outbox = createOutbox()
        val record = outbox.sendText()
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.NewLocalEvent(A_TRANSACTION_ID))
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.SendError(A_TRANSACTION_ID))
        val retryLambda = lambdaRecorder<Result<Unit>> { Result.success(Unit) }
        val result = outbox.retry(record.id, FakeSendHandle(retryLambda = retryLambda))
        assertThat(result).isEqualTo(OutboxRetryResult.Retried)
        retryLambda.assertions().isCalledOnce()
        val after = store.get(record.id)!!
        assertThat(after.state).isEqualTo(OutboxState.LocallyAccepted)
        assertThat(after.attempts).isEqualTo(2)
        assertThat(after.transactionIds).containsExactly(OutboxRoute.MATRIX_SEND_QUEUE, A_TRANSACTION_ID)
    }

    @Test
    fun `retry without a route handle refuses rather than sending a possible duplicate`() = runTest {
        val outbox = createOutbox()
        val record = outbox.sendText()
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.NewLocalEvent(A_TRANSACTION_ID))
        outbox.reconcile(A_SESSION_ID, A_ROOM_ID, listOf(echo(transactionId = TransactionId("other"))), 20_000L)
        assertThat(outbox.retry(record.id, sendHandle = null)).isEqualTo(OutboxRetryResult.NoRouteHandle)
        assertThat(store.get(record.id)?.state).isEqualTo(OutboxState.Uncertain)
    }

    @Test
    fun `retry is refused for records that are not retryable or unknown`() = runTest {
        val outbox = createOutbox()
        val record = outbox.sendText()
        assertThat(outbox.retry(record.id, FakeSendHandle())).isEqualTo(OutboxRetryResult.NotRetryable)
        assertThat(outbox.retry(OutboxId("nope"), FakeSendHandle())).isEqualTo(OutboxRetryResult.NotFound)
    }

    @Test
    fun `a route refusing the retry leaves the record uncertain`() = runTest {
        val outbox = createOutbox()
        val record = outbox.sendText()
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.NewLocalEvent(A_TRANSACTION_ID))
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.SendError(A_TRANSACTION_ID))
        val result = outbox.retry(record.id, FakeSendHandle(retryLambda = { Result.failure(IllegalStateException("no")) }))
        assertThat(result).isEqualTo(OutboxRetryResult.RouteRefused("IllegalStateException"))
        assertThat(store.get(record.id)?.state).isEqualTo(OutboxState.Uncertain)
        assertThat(store.get(record.id)?.attempts).isEqualTo(1)
    }

    @Test
    fun `updates for other sessions or rooms are ignored`() = runTest {
        val outbox = createOutbox()
        val record = outbox.sendText()
        outbox.onSendQueueUpdate(A_SESSION_ID, RoomId("!other:domain"), SendQueueUpdate.NewLocalEvent(A_TRANSACTION_ID))
        outbox.onSendQueueUpdate(A_USER_ID_2, A_ROOM_ID, SendQueueUpdate.NewLocalEvent(A_TRANSACTION_ID))
        assertThat(store.get(record.id)?.state).isEqualTo(OutboxState.Queued)
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.ReplacedLocalEvent(A_TRANSACTION_ID))
        outbox.onSendQueueUpdate(A_SESSION_ID, A_ROOM_ID, SendQueueUpdate.MediaUpload(A_TRANSACTION_ID, null, 0L, 0f))
        assertThat(store.get(record.id)?.state).isEqualTo(OutboxState.Queued)
    }
}
