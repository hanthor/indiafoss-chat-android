/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.impl

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.room.SendQueueUpdate
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.A_SESSION_ID
import io.element.android.libraries.matrix.test.A_TRANSACTION_ID
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.room.FakeJoinedRoom
import io.element.android.libraries.outbox.api.OutboxState
import io.element.android.libraries.outbox.test.FakeOutbox
import io.element.android.libraries.outbox.test.InMemoryOutboxStore
import io.element.android.libraries.outbox.test.anOutboxRecord
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultOutboxRoomTrackerTest {
    @Test
    fun `track forwards send queue updates to the outbox once per room`() = runTest {
        val room = FakeJoinedRoom()
        val outbox = FakeOutbox()
        val tracker = DefaultOutboxRoomTracker(
            matrixClient = FakeMatrixClient(),
            outbox = outbox,
            outboxStore = InMemoryOutboxStore(),
            sessionCoroutineScope = backgroundScope,
        )
        tracker.track(room)
        tracker.track(room)
        advanceUntilIdle()
        room.givenSendQueueUpdate(SendQueueUpdate.NewLocalEvent(A_TRANSACTION_ID))
        advanceUntilIdle()
        assertThat(outbox.sendQueueUpdates).containsExactly(SendQueueUpdate.NewLocalEvent(A_TRANSACTION_ID))
    }

    @Test
    fun `launchIn re-subscribes rooms that still have unsettled sends`() = runTest {
        val room = FakeJoinedRoom()
        val store = InMemoryOutboxStore().apply {
            insert(anOutboxRecord(sessionId = A_SESSION_ID, roomId = A_ROOM_ID, state = OutboxState.Uncertain))
        }
        val outbox = FakeOutbox()
        val tracker = DefaultOutboxRoomTracker(
            matrixClient = FakeMatrixClient(sessionId = A_SESSION_ID).apply { givenGetRoomResult(A_ROOM_ID, room) },
            outbox = outbox,
            outboxStore = store,
            sessionCoroutineScope = backgroundScope,
        )
        tracker.launchIn(backgroundScope)
        advanceUntilIdle()
        room.givenSendQueueUpdate(SendQueueUpdate.SendError(A_TRANSACTION_ID))
        advanceUntilIdle()
        assertThat(outbox.sendQueueUpdates).containsExactly(SendQueueUpdate.SendError(A_TRANSACTION_ID))
    }
}
