/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.receipt

import io.element.android.features.messages.impl.timeline.model.ReadReceiptData
import io.element.android.libraries.matrix.api.timeline.item.event.LocalEventSendState
import io.element.android.libraries.outbox.api.OutboxState
import kotlinx.collections.immutable.ImmutableList

data class ReadReceiptViewState(
    val sendState: LocalEventSendState?,
    /** The durable outbox state when known; it takes precedence over [sendState], which cannot express delivery. */
    val outboxState: OutboxState?,
    val isLastOutgoingMessage: Boolean,
    val receipts: ImmutableList<ReadReceiptData>,
)
