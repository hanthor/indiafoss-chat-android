/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.test

import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.TransactionId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.outbox.api.OutboxContentKind
import io.element.android.libraries.outbox.api.OutboxContentRef
import io.element.android.libraries.outbox.api.OutboxId
import io.element.android.libraries.outbox.api.OutboxRecord
import io.element.android.libraries.outbox.api.OutboxRoute
import io.element.android.libraries.outbox.api.OutboxState

fun anOutboxRecord(
    id: OutboxId = OutboxId("an-outbox-id"),
    sessionId: SessionId = UserId("@alice:server.org"),
    roomId: RoomId = RoomId("!aRoomId:domain"),
    contentRef: OutboxContentRef = OutboxContentRef(OutboxContentKind.TEXT),
    transactionId: TransactionId? = null,
    createdAtMillis: Long = 0L,
    updatedAtMillis: Long = createdAtMillis,
    attempts: Int = 1,
    state: OutboxState = OutboxState.Queued,
) = OutboxRecord(
    id = id,
    sessionId = sessionId,
    roomId = roomId,
    contentRef = contentRef,
    transactionIds = transactionId?.let { mapOf(OutboxRoute.MATRIX_SEND_QUEUE to it) }.orEmpty(),
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    attempts = attempts,
    state = state,
)
