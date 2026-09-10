/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.api

/**
 * The only place that decides how a record moves between states. Pure, so every rule is unit-testable.
 *
 * Queued -> LocallyAccepted -> ServerAccepted -> Delivered -> Read
 * with Failed, Uncertain and Cancelled explicit and never collapsed into each other.
 *
 * Rules the architecture imposes (companion docs/tasks/X-02):
 * - a read receipt never promotes anything below [OutboxState.ServerAccepted];
 * - server acceptance never becomes [OutboxState.Delivered] without a recipient acknowledgement;
 * - a late acknowledgement (RouteSent after Uncertain or Failed) is the truth and wins;
 * - cancelling after server acceptance does not unsend.
 */
object OutboxStateMachine {
    /** Returns the next state, or the same instance when [signal] does not apply to [current]. */
    fun next(current: OutboxState, signal: OutboxSignal): OutboxState = when (signal) {
        OutboxSignal.RouteAccepted -> when (current) {
            OutboxState.Queued,
            // The route still holds the echo and is trying: that is the truth, whatever we feared.
            OutboxState.Uncertain,
            is OutboxState.Failed -> OutboxState.LocallyAccepted
            else -> current
        }
        is OutboxSignal.RouteSent -> when (current) {
            OutboxState.Queued,
            OutboxState.LocallyAccepted,
            OutboxState.Uncertain,
            is OutboxState.Failed -> OutboxState.ServerAccepted(signal.eventId)
            // Same event already known, or already further along: nothing to change.
            else -> current
        }
        is OutboxSignal.RouteError -> when (current) {
            // Never handed to the route: nothing was dispatched, a retry is safe.
            OutboxState.Queued -> OutboxState.Failed(reason = signal.reason, retryable = true)
            OutboxState.LocallyAccepted,
            OutboxState.Uncertain -> if (signal.definitive) {
                OutboxState.Failed(reason = signal.reason, retryable = false)
            } else {
                OutboxState.Uncertain
            }
            else -> current
        }
        OutboxSignal.RouteRetrying,
        OutboxSignal.RetryRequested -> when (current) {
            OutboxState.Uncertain,
            is OutboxState.Failed -> OutboxState.LocallyAccepted
            else -> current
        }
        OutboxSignal.RouteCancelled -> when (current) {
            OutboxState.Queued,
            OutboxState.LocallyAccepted,
            OutboxState.Uncertain,
            is OutboxState.Failed -> OutboxState.Cancelled
            // Accepted elsewhere already: cancellation cannot retract it.
            else -> current
        }
        OutboxSignal.AckLost -> when (current) {
            OutboxState.Queued,
            OutboxState.LocallyAccepted -> OutboxState.Uncertain
            else -> current
        }
        is OutboxSignal.DeliveryReceipt -> when (current) {
            is OutboxState.ServerAccepted -> if (current.eventId == signal.eventId) {
                OutboxState.Delivered(eventId = signal.eventId, recipientId = signal.recipientId)
            } else {
                current
            }
            else -> current
        }
        is OutboxSignal.ReadReceipt -> when (current) {
            is OutboxState.ServerAccepted -> if (current.eventId == signal.eventId) {
                OutboxState.Read(eventId = signal.eventId, readerId = signal.readerId)
            } else {
                current
            }
            is OutboxState.Delivered -> if (current.eventId == signal.eventId) {
                OutboxState.Read(eventId = signal.eventId, readerId = signal.readerId)
            } else {
                current
            }
            else -> current
        }
    }
}
