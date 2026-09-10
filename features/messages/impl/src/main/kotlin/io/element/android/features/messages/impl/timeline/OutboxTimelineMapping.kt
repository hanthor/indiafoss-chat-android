/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline

import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.outbox.api.OutboxObservation
import io.element.android.libraries.outbox.api.OutboxRecord
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** Our own events as the live timeline shows them, for [io.element.android.libraries.outbox.api.Outbox.reconcile]. */
internal fun List<MatrixTimelineItem>.toOutboxObservations(sessionId: SessionId): List<OutboxObservation> = mapNotNull { item ->
    val event = (item as? MatrixTimelineItem.Event)?.event?.takeIf { it.isOwn } ?: return@mapNotNull null
    OutboxObservation(
        transactionId = event.transactionId,
        eventId = event.eventId,
        localSendState = event.localSendState,
        readBy = event.receipts.map { it.userId }.filter { it != sessionId },
    )
}

/** Attach each of our own timeline events to its durable outbox record, when one exists. */
internal fun ImmutableList<TimelineItem>.withOutboxSummaries(records: List<OutboxRecord>): ImmutableList<TimelineItem> {
    if (records.isEmpty()) return this
    val byTransaction = records.mapNotNull { record -> record.matrixTransactionId?.let { it to record } }.toMap()
    val byEvent = records.mapNotNull { record -> record.state.eventIdOrNull?.let { it to record } }.toMap()
    return map { item ->
        if (item is TimelineItem.Event && item.isMine) {
            val record = item.transactionId?.let { byTransaction[it] } ?: item.eventId?.let { byEvent[it] }
            if (record != null) item.copy(outbox = record.summary()) else item
        } else {
            item
        }
    }.toImmutableList()
}
