/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.api

import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.TransactionId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.LocalEventSendState

/**
 * What the live timeline shows for one of our own events, used to reconcile records with the SDK's
 * queue instead of resending blindly.
 */
data class OutboxObservation(
    val transactionId: TransactionId?,
    val eventId: EventId?,
    val localSendState: LocalEventSendState?,
    /** Users other than the sender whose read receipt is positioned at this event. */
    val readBy: List<UserId>,
)
