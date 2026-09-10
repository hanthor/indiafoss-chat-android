/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.receipt

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.outbox.api.OutboxState

/** Every outbox state the indicator can show, so accepted, delivered and read are visibly distinct. */
class ReadReceiptViewStateOutboxProvider : PreviewParameterProvider<ReadReceiptViewState> {
    override val values: Sequence<ReadReceiptViewState>
        get() = sequenceOf(
            aReadReceiptViewState(outboxState = OutboxState.Queued),
            aReadReceiptViewState(outboxState = OutboxState.LocallyAccepted),
            aReadReceiptViewState(outboxState = OutboxState.ServerAccepted(anEventId())),
            aReadReceiptViewState(outboxState = OutboxState.Delivered(anEventId(), aReader())),
            aReadReceiptViewState(outboxState = OutboxState.Read(anEventId(), aReader())),
            aReadReceiptViewState(outboxState = OutboxState.Uncertain),
            aReadReceiptViewState(outboxState = OutboxState.Failed(reason = "unknown", retryable = false)),
            aReadReceiptViewState(outboxState = OutboxState.Cancelled),
        )
}

private fun anEventId() = EventId("\$eventId")

private fun aReader() = UserId("@bob:domain")
