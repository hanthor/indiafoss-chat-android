/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.impl

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.test.AN_EVENT_ID
import io.element.android.libraries.matrix.test.AN_EVENT_ID_2
import io.element.android.libraries.matrix.test.A_USER_ID_2
import io.element.android.libraries.outbox.api.OutboxSignal
import io.element.android.libraries.outbox.api.OutboxState
import io.element.android.libraries.outbox.api.OutboxStateMachine
import org.junit.Test

class OutboxStateMachineTest {
    private val accepted = OutboxState.ServerAccepted(AN_EVENT_ID)

    @Test
    fun `happy path goes queued, locally accepted, server accepted, read`() {
        var state: OutboxState = OutboxState.Queued
        state = OutboxStateMachine.next(state, OutboxSignal.RouteAccepted)
        assertThat(state).isEqualTo(OutboxState.LocallyAccepted)
        state = OutboxStateMachine.next(state, OutboxSignal.RouteSent(AN_EVENT_ID))
        assertThat(state).isEqualTo(accepted)
        state = OutboxStateMachine.next(state, OutboxSignal.ReadReceipt(AN_EVENT_ID, A_USER_ID_2))
        assertThat(state).isEqualTo(OutboxState.Read(AN_EVENT_ID, A_USER_ID_2))
    }

    @Test
    fun `server acceptance is never delivery`() {
        val state = OutboxStateMachine.next(OutboxState.LocallyAccepted, OutboxSignal.RouteSent(AN_EVENT_ID))
        assertThat(state).isEqualTo(accepted)
        assertThat(state).isNotInstanceOf(OutboxState.Delivered::class.java)
    }

    @Test
    fun `a read receipt never promotes anything below server accepted`() {
        val receipt = OutboxSignal.ReadReceipt(AN_EVENT_ID, A_USER_ID_2)
        assertThat(OutboxStateMachine.next(OutboxState.Queued, receipt)).isEqualTo(OutboxState.Queued)
        assertThat(OutboxStateMachine.next(OutboxState.LocallyAccepted, receipt)).isEqualTo(OutboxState.LocallyAccepted)
        assertThat(OutboxStateMachine.next(OutboxState.Uncertain, receipt)).isEqualTo(OutboxState.Uncertain)
    }

    @Test
    fun `a receipt for another event does not apply`() {
        val receipt = OutboxSignal.ReadReceipt(AN_EVENT_ID_2, A_USER_ID_2)
        assertThat(OutboxStateMachine.next(accepted, receipt)).isEqualTo(accepted)
    }

    @Test
    fun `delivery requires a recipient receipt and read does not downgrade`() {
        val delivered = OutboxStateMachine.next(accepted, OutboxSignal.DeliveryReceipt(AN_EVENT_ID, A_USER_ID_2))
        assertThat(delivered).isEqualTo(OutboxState.Delivered(AN_EVENT_ID, A_USER_ID_2))
        val read = OutboxStateMachine.next(delivered, OutboxSignal.ReadReceipt(AN_EVENT_ID, A_USER_ID_2))
        assertThat(read).isEqualTo(OutboxState.Read(AN_EVENT_ID, A_USER_ID_2))
        assertThat(OutboxStateMachine.next(read, OutboxSignal.DeliveryReceipt(AN_EVENT_ID, A_USER_ID_2))).isEqualTo(read)
    }

    @Test
    fun `a lost acknowledgement is uncertain, not failed`() {
        assertThat(OutboxStateMachine.next(OutboxState.LocallyAccepted, OutboxSignal.AckLost)).isEqualTo(OutboxState.Uncertain)
        assertThat(OutboxStateMachine.next(OutboxState.Queued, OutboxSignal.AckLost)).isEqualTo(OutboxState.Uncertain)
        assertThat(OutboxStateMachine.next(accepted, OutboxSignal.AckLost)).isEqualTo(accepted)
    }

    @Test
    fun `a non definitive route error after dispatch is uncertain, a definitive one is failed`() {
        val network = OutboxSignal.RouteError(reason = "network", definitive = false)
        val rejected = OutboxSignal.RouteError(reason = "unverified", definitive = true)
        assertThat(OutboxStateMachine.next(OutboxState.LocallyAccepted, network)).isEqualTo(OutboxState.Uncertain)
        assertThat(OutboxStateMachine.next(OutboxState.LocallyAccepted, rejected)).isEqualTo(OutboxState.Failed("unverified", retryable = false))
        assertThat(OutboxStateMachine.next(OutboxState.Uncertain, rejected)).isEqualTo(OutboxState.Failed("unverified", retryable = false))
    }

    @Test
    fun `an error before the route took the send is a retryable failure`() {
        val state = OutboxStateMachine.next(OutboxState.Queued, OutboxSignal.RouteError(reason = "boom", definitive = true))
        assertThat(state).isEqualTo(OutboxState.Failed("boom", retryable = true))
        assertThat(state.isRetryable).isTrue()
    }

    @Test
    fun `a late acknowledgement is the truth`() {
        assertThat(OutboxStateMachine.next(OutboxState.Uncertain, OutboxSignal.RouteSent(AN_EVENT_ID))).isEqualTo(accepted)
        assertThat(OutboxStateMachine.next(OutboxState.Failed("x", retryable = false), OutboxSignal.RouteSent(AN_EVENT_ID))).isEqualTo(accepted)
    }

    @Test
    fun `retrying moves back to locally accepted`() {
        assertThat(OutboxStateMachine.next(OutboxState.Uncertain, OutboxSignal.RetryRequested)).isEqualTo(OutboxState.LocallyAccepted)
        assertThat(OutboxStateMachine.next(OutboxState.Failed("x", retryable = true), OutboxSignal.RouteRetrying)).isEqualTo(OutboxState.LocallyAccepted)
        assertThat(OutboxStateMachine.next(accepted, OutboxSignal.RetryRequested)).isEqualTo(accepted)
    }

    @Test
    fun `a route that still holds the echo after uncertainty is locally accepted`() {
        assertThat(OutboxStateMachine.next(OutboxState.Uncertain, OutboxSignal.RouteAccepted)).isEqualTo(OutboxState.LocallyAccepted)
    }

    @Test
    fun `cancel stops pending sends but cannot unsend`() {
        assertThat(OutboxStateMachine.next(OutboxState.Queued, OutboxSignal.RouteCancelled)).isEqualTo(OutboxState.Cancelled)
        assertThat(OutboxStateMachine.next(OutboxState.Uncertain, OutboxSignal.RouteCancelled)).isEqualTo(OutboxState.Cancelled)
        assertThat(OutboxStateMachine.next(accepted, OutboxSignal.RouteCancelled)).isEqualTo(accepted)
    }

    @Test
    fun `terminal states ignore everything else`() {
        val read = OutboxState.Read(AN_EVENT_ID, A_USER_ID_2)
        listOf(
            OutboxSignal.RouteAccepted,
            OutboxSignal.AckLost,
            OutboxSignal.RouteError("x", definitive = false),
            OutboxSignal.RetryRequested,
        ).forEach { signal ->
            assertThat(OutboxStateMachine.next(read, signal)).isEqualTo(read)
            assertThat(OutboxStateMachine.next(OutboxState.Cancelled, signal)).isEqualTo(OutboxState.Cancelled)
        }
    }
}
