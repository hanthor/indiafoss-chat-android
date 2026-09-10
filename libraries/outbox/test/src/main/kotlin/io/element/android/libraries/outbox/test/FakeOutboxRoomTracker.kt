/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.test

import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.room.JoinedRoom
import io.element.android.libraries.outbox.api.OutboxRoomTracker
import kotlinx.coroutines.CoroutineScope

class FakeOutboxRoomTracker : OutboxRoomTracker {
    val trackedRooms = mutableListOf<RoomId>()
    var launched = false

    override fun track(room: JoinedRoom) {
        trackedRooms.add(room.roomId)
    }

    override fun launchIn(coroutineScope: CoroutineScope) {
        launched = true
    }
}
