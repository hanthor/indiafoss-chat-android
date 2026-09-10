/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.api

import io.element.android.libraries.matrix.api.room.JoinedRoom
import kotlinx.coroutines.CoroutineScope

/**
 * Keeps [Outbox] subscribed to the send-queue updates of rooms that have, or are about to have,
 * unsettled records, for the lifetime of the session.
 */
interface OutboxRoomTracker {
    /** Subscribe [room] once; further calls for the same room are no-ops. */
    fun track(room: JoinedRoom)

    /** On session start, re-subscribe every room that still has unsettled records. */
    fun launchIn(coroutineScope: CoroutineScope)
}
