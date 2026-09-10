/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.api

import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.TransactionId

/**
 * The durable record of one logical send. Written before any network call and updated on every
 * observation, so it survives process death with its true state.
 */
data class OutboxRecord(
    val id: OutboxId,
    /** The account that sends. Two accounts are two senders; this never changes for a record. */
    val sessionId: SessionId,
    val roomId: RoomId,
    val contentRef: OutboxContentRef,
    /** One idempotency key per route attempted. A retry on a route re-uses that route's key. */
    val transactionIds: Map<OutboxRoute, TransactionId>,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    /** Number of dispatch attempts observed (initial send plus retries). */
    val attempts: Int,
    val state: OutboxState,
) {
    val matrixTransactionId: TransactionId?
        get() = transactionIds[OutboxRoute.MATRIX_SEND_QUEUE]

    fun summary(): OutboxSummary = OutboxSummary(id = id, state = state)
}
