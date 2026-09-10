/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.api

import androidx.compose.runtime.Immutable
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.UserId

/**
 * The delivery state of one logical send. No state is ever inferred from another one:
 * - [ServerAccepted] is a route taking custody of the event. It says nothing about the recipient.
 * - [Delivered] requires a recipient acknowledgement. No route provides one today, so it is never reached.
 * - [Read] requires a read receipt from another user. It is a distinct fact and not an upgrade of delivery.
 * - [Uncertain] means the route may or may not have accepted the event (the acknowledgement was lost).
 */
@Immutable
sealed interface OutboxState {
    /** Persisted locally, not yet handed to the route. */
    data object Queued : OutboxState

    /** The route (SDK send queue) holds a local echo and will try to send it. */
    data object LocallyAccepted : OutboxState

    /** The homeserver returned an event id. Not recipient decryption, not delivery. */
    data class ServerAccepted(val eventId: EventId) : OutboxState

    /** A recipient acknowledged reception. Requires a route that supports it; none does today. */
    data class Delivered(val eventId: EventId, val recipientId: UserId) : OutboxState

    /** Another user's read receipt points at this event. Optional and distinct from delivery. */
    data class Read(val eventId: EventId, val readerId: UserId) : OutboxState

    /** The route rejected the send, or it never reached the route. */
    data class Failed(val reason: String, val retryable: Boolean) : OutboxState

    /** The route was asked to send but the outcome is unknown (ACK lost, process death). */
    data object Uncertain : OutboxState

    /** The user stopped further attempts. Cannot retract an event the server already accepted. */
    data object Cancelled : OutboxState

    /** True while the outcome can still change on its own (without a user action). */
    val isPending: Boolean
        get() = this is Queued || this is LocallyAccepted

    /** True when a user-triggered retry on the same route is meaningful. */
    val isRetryable: Boolean
        get() = this is Uncertain || (this is Failed && retryable)

    val eventIdOrNull: EventId?
        get() = when (this) {
            is ServerAccepted -> eventId
            is Delivered -> eventId
            is Read -> eventId
            else -> null
        }
}
