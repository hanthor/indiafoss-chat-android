/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.test

import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.outbox.api.OutboxId
import io.element.android.libraries.outbox.api.OutboxRecord
import io.element.android.libraries.outbox.api.OutboxState
import io.element.android.libraries.outbox.api.OutboxStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** A store that behaves like the database one, so "process death" is a new [io.element.android.libraries.outbox.api.Outbox] over the same store. */
class InMemoryOutboxStore : OutboxStore {
    private val records = MutableStateFlow<Map<OutboxId, OutboxRecord>>(emptyMap())

    val all: List<OutboxRecord>
        get() = records.value.values.sortedBy { it.createdAtMillis }

    override suspend fun insert(record: OutboxRecord) {
        records.value = records.value + (record.id to record)
    }

    override suspend fun update(record: OutboxRecord) {
        records.value = records.value + (record.id to record)
    }

    override suspend fun get(id: OutboxId): OutboxRecord? = records.value[id]

    override suspend fun getAll(sessionId: SessionId, roomId: RoomId): List<OutboxRecord> {
        return all.filter { it.sessionId == sessionId && it.roomId == roomId }
    }

    override suspend fun getUnsettled(sessionId: SessionId): List<OutboxRecord> {
        return all.filter { it.sessionId == sessionId && (it.state.isPending || it.state == OutboxState.Uncertain) }
    }

    override fun observe(sessionId: SessionId, roomId: RoomId): Flow<List<OutboxRecord>> {
        return records.map { map ->
            map.values.filter { it.sessionId == sessionId && it.roomId == roomId }.sortedBy { it.createdAtMillis }
        }
    }
}
