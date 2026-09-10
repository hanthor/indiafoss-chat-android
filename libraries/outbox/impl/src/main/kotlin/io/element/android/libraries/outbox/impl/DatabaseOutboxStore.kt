/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.outbox.api.OutboxId
import io.element.android.libraries.outbox.api.OutboxRecord
import io.element.android.libraries.outbox.api.OutboxStore
import io.element.android.libraries.outbox.impl.db.OutboxEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DatabaseOutboxStore(
    private val database: OutboxDatabase,
    private val dispatchers: CoroutineDispatchers,
) : OutboxStore {
    private val queries get() = database.outboxEntryQueries

    override suspend fun insert(record: OutboxRecord) = withContext(dispatchers.io) {
        database.transaction {
            queries.insertEntry(record.toEntry())
            record.toTransactionEntries().forEach(queries::upsertTransaction)
        }
    }

    override suspend fun update(record: OutboxRecord) = withContext(dispatchers.io) {
        database.transaction {
            queries.updateEntry(record.toEntry())
            record.toTransactionEntries().forEach(queries::upsertTransaction)
        }
    }

    override suspend fun get(id: OutboxId): OutboxRecord? = withContext(dispatchers.io) {
        queries.selectById(id.value).executeAsOneOrNull()?.toRecord()
    }

    override suspend fun getAll(sessionId: SessionId, roomId: RoomId): List<OutboxRecord> = withContext(dispatchers.io) {
        queries.selectByRoom(sessionId.value, roomId.value).executeAsList().map { it.toRecord() }
    }

    override suspend fun getUnsettled(sessionId: SessionId): List<OutboxRecord> = withContext(dispatchers.io) {
        queries.selectUnsettled(sessionId.value).executeAsList().map { it.toRecord() }
    }

    override fun observe(sessionId: SessionId, roomId: RoomId): Flow<List<OutboxRecord>> {
        return queries.selectByRoom(sessionId.value, roomId.value)
            .asFlow()
            .mapToList(dispatchers.io)
            .map { entries -> entries.map { it.toRecord() } }
    }

    private fun OutboxEntry.toRecord(): OutboxRecord = toRecord(queries.selectTransactions(id).executeAsList())
}
