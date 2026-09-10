/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.api

import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.UserId

/**
 * An observation that may move a record to another [OutboxState]. Signals are facts reported by a
 * route or by the user; [OutboxStateMachine] decides whether they change anything.
 */
sealed interface OutboxSignal {
    /** The route created its local echo and owns the send from now on. */
    data object RouteAccepted : OutboxSignal

    /** The route reports the homeserver accepted the event. */
    data class RouteSent(val eventId: EventId) : OutboxSignal

    /**
     * The route reports an error. [definitive] is true only when the route knows the request was
     * never accepted (a local rejection such as an unverified device); a network error after
     * dispatch is not definitive, because the server may have accepted the request anyway.
     */
    data class RouteError(val reason: String, val definitive: Boolean) : OutboxSignal

    /** The route is trying again on its own (same transaction id). */
    data object RouteRetrying : OutboxSignal

    /** The route dropped the local echo before it was sent. */
    data object RouteCancelled : OutboxSignal

    /** The user asked for a retry on the same route, and the route accepted the request. */
    data object RetryRequested : OutboxSignal

    /** No route has a trace of this send any more; the outcome is unknown. */
    data object AckLost : OutboxSignal

    /** A recipient acknowledged reception of [eventId]. No route provides this today. */
    data class DeliveryReceipt(val eventId: EventId, val recipientId: UserId) : OutboxSignal

    /** Another user's read receipt is positioned at [eventId]. */
    data class ReadReceipt(val eventId: EventId, val readerId: UserId) : OutboxSignal
}
