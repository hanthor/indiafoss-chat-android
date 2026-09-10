/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.features.messages.impl.timeline.aTimelineItemEvent
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.timeline.item.event.LocalEventSendState
import io.element.android.libraries.outbox.api.OutboxId
import io.element.android.libraries.outbox.api.OutboxState
import io.element.android.libraries.outbox.api.OutboxSummary

/** Own events carrying a durable outbox state; the uncertain one is the only state that changes the timestamp row. */
class TimelineItemEventForOutboxTimestampViewProvider : PreviewParameterProvider<TimelineItem.Event> {
    override val values: Sequence<TimelineItem.Event>
        get() = sequenceOf(
            anOutboxTimelineItemEvent(OutboxState.ServerAccepted(EventId("$eventId"))),
            anOutboxTimelineItemEvent(OutboxState.Uncertain),
            anOutboxTimelineItemEvent(
                state = OutboxState.Uncertain,
                sendState = LocalEventSendState.Failed.Unknown("AN_ERROR"),
            ),
        )
}

internal fun anOutboxTimelineItemEvent(
    state: OutboxState,
    sendState: LocalEventSendState? = null,
): TimelineItem.Event = aTimelineItemEvent(
    isMine = true,
    sendState = sendState,
    outbox = OutboxSummary(id = OutboxId("an-outbox-id"), state = state),
)
