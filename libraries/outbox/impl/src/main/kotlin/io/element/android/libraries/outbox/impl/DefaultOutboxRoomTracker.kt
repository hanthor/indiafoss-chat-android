/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.room.JoinedRoom
import io.element.android.libraries.outbox.api.Outbox
import io.element.android.libraries.outbox.api.OutboxRoomTracker
import io.element.android.libraries.outbox.api.OutboxStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

@SingleIn(SessionScope::class)
@ContributesBinding(SessionScope::class)
class DefaultOutboxRoomTracker(
    private val matrixClient: MatrixClient,
    private val outbox: Outbox,
    private val outboxStore: OutboxStore,
    @SessionCoroutineScope private val sessionCoroutineScope: CoroutineScope,
) : OutboxRoomTracker {
    private val tag = "OutboxRoomTracker"
    private val tracked = mutableSetOf<RoomId>()

    override fun track(room: JoinedRoom) {
        synchronized(tracked) {
            if (!tracked.add(room.roomId)) return
        }
        Timber.tag(tag).d("Following send queue of ${room.roomId}")
        room.subscribeToSendQueueUpdates()
            .onEach { update -> outbox.onSendQueueUpdate(room.sessionId, room.roomId, update) }
            .launchIn(sessionCoroutineScope)
    }

    override fun launchIn(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            val rooms = outboxStore.getUnsettled(matrixClient.sessionId).map { it.roomId }.toSet()
            Timber.tag(tag).d("${rooms.size} room(s) with unsettled sends after restart")
            rooms.forEach { roomId ->
                matrixClient.getJoinedRoom(roomId)?.let(::track)
            }
        }
    }
}
