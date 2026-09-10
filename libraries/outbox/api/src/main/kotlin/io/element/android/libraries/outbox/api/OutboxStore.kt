/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.api

import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.coroutines.flow.Flow

/** Durable storage for [OutboxRecord]s. Every write is atomic for one record. */
interface OutboxStore {
    suspend fun insert(record: OutboxRecord)
    suspend fun update(record: OutboxRecord)
    suspend fun get(id: OutboxId): OutboxRecord?
    suspend fun getAll(sessionId: SessionId, roomId: RoomId): List<OutboxRecord>

    /** Records whose outcome can still change: [OutboxState.isPending] or [OutboxState.Uncertain]. */
    suspend fun getUnsettled(sessionId: SessionId): List<OutboxRecord>
    fun observe(sessionId: SessionId, roomId: RoomId): Flow<List<OutboxRecord>>
}
