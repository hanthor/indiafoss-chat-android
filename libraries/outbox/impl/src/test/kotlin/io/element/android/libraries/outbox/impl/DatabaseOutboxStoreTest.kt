/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.impl

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.matrix.test.AN_EVENT_ID
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.A_SESSION_ID
import io.element.android.libraries.matrix.test.A_TRANSACTION_ID
import io.element.android.libraries.matrix.test.A_USER_ID_2
import io.element.android.libraries.outbox.api.OutboxContentKind
import io.element.android.libraries.outbox.api.OutboxContentRef
import io.element.android.libraries.outbox.api.OutboxId
import io.element.android.libraries.outbox.api.OutboxRoute
import io.element.android.libraries.outbox.api.OutboxState
import io.element.android.libraries.outbox.test.anOutboxRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DatabaseOutboxStoreTest {
    private lateinit var store: DatabaseOutboxStore

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        OutboxDatabase.Schema.create(driver)
        store = DatabaseOutboxStore(
            database = OutboxDatabase(driver),
            dispatchers = CoroutineDispatchers(
                io = UnconfinedTestDispatcher(),
                computation = UnconfinedTestDispatcher(),
                main = UnconfinedTestDispatcher(),
            ),
        )
    }

    @Test
    fun `every state round-trips through the database with its transaction ids`() = runTest {
        val states = listOf(
            OutboxState.Queued,
            OutboxState.LocallyAccepted,
            OutboxState.ServerAccepted(AN_EVENT_ID),
            OutboxState.Delivered(AN_EVENT_ID, A_USER_ID_2),
            OutboxState.Read(AN_EVENT_ID, A_USER_ID_2),
            OutboxState.Failed(reason = "boom", retryable = true),
            OutboxState.Failed(reason = "rejected", retryable = false),
            OutboxState.Uncertain,
            OutboxState.Cancelled,
        )
        states.forEachIndexed { index, state ->
            val record = anOutboxRecord(
                id = OutboxId("id-$index"),
                contentRef = OutboxContentRef(OutboxContentKind.MEDIA, mediaReference = "content://media/$index"),
                transactionId = A_TRANSACTION_ID,
                createdAtMillis = index.toLong(),
                attempts = index + 1,
                state = state,
            )
            store.insert(record)
            assertThat(store.get(record.id)).isEqualTo(record)
        }
        assertThat(store.getAll(A_SESSION_ID, A_ROOM_ID).map { it.state }).isEqualTo(states)
    }

    @Test
    fun `update replaces the record and keeps the route transaction id`() = runTest {
        val record = anOutboxRecord()
        store.insert(record)
        val updated = record.copy(
            state = OutboxState.LocallyAccepted,
            transactionIds = mapOf(OutboxRoute.MATRIX_SEND_QUEUE to A_TRANSACTION_ID),
            attempts = 2,
            updatedAtMillis = 5L,
        )
        store.update(updated)
        assertThat(store.get(record.id)).isEqualTo(updated)
        assertThat(store.getAll(A_SESSION_ID, A_ROOM_ID)).hasSize(1)
    }

    @Test
    fun `unsettled returns pending and uncertain records for the session only`() = runTest {
        store.insert(anOutboxRecord(id = OutboxId("queued"), state = OutboxState.Queued))
        store.insert(anOutboxRecord(id = OutboxId("local"), state = OutboxState.LocallyAccepted, createdAtMillis = 1L))
        store.insert(anOutboxRecord(id = OutboxId("uncertain"), state = OutboxState.Uncertain, createdAtMillis = 2L))
        store.insert(anOutboxRecord(id = OutboxId("accepted"), state = OutboxState.ServerAccepted(AN_EVENT_ID), createdAtMillis = 3L))
        store.insert(anOutboxRecord(id = OutboxId("failed"), state = OutboxState.Failed("x", retryable = true), createdAtMillis = 4L))
        store.insert(anOutboxRecord(id = OutboxId("other-session"), sessionId = A_USER_ID_2, state = OutboxState.Queued, createdAtMillis = 5L))
        assertThat(store.getUnsettled(A_SESSION_ID).map { it.id.value }).containsExactly("queued", "local", "uncertain").inOrder()
    }

    @Test
    fun `observe emits the room records on every change`() = runTest {
        store.observe(A_SESSION_ID, A_ROOM_ID).test {
            assertThat(awaitItem()).isEmpty()
            val record = anOutboxRecord()
            store.insert(record)
            assertThat(awaitItem()).containsExactly(record)
            store.update(record.copy(state = OutboxState.Uncertain))
            assertThat(awaitItem().single().state).isEqualTo(OutboxState.Uncertain)
        }
    }

    @Test
    fun `an unknown state kind written by a newer version reads as uncertain`() = runTest {
        val record = anOutboxRecord()
        store.insert(record)
        val entry = record.toEntry().copy(state = "SOMETHING_NEW")
        assertThat(OutboxStateMapper.toState(entry)).isEqualTo(OutboxState.Uncertain)
    }
}
