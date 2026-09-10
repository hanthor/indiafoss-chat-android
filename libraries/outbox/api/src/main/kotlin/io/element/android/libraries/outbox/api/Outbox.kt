/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.api

import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SendHandle
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.SendQueueUpdate
import kotlinx.coroutines.flow.Flow

/**
 * The durable logical outbox (hanthor/indiafoss-chat-android#48, step 1).
 *
 * It records intent before dispatch, follows the SDK send queue afterwards, and reconciles against
 * the SDK's own persisted queue on restart. It never dispatches on its own and never selects a route.
 */
interface Outbox {
    /**
     * Persist a [OutboxState.Queued] record, then run [dispatch] (which hands the content to the SDK
     * send queue). A failing dispatch leaves a retryable [OutboxState.Failed] record; a successful one
     * leaves the record waiting for the route's local echo.
     */
    suspend fun send(
        sessionId: SessionId,
        roomId: RoomId,
        contentRef: OutboxContentRef,
        dispatch: suspend () -> Result<Unit>,
    ): OutboxRecord

    /** Feed a route update for [roomId]. Updates that do not belong to a record are ignored. */
    suspend fun onSendQueueUpdate(sessionId: SessionId, roomId: RoomId, update: SendQueueUpdate)

    /**
     * Compare unsettled records with what the live timeline shows. Pending records the SDK has no trace
     * of become [OutboxState.Uncertain] (after a short grace period so a send in flight is not judged
     * before its echo appears). Nothing is ever resent from here.
     */
    suspend fun reconcile(
        sessionId: SessionId,
        roomId: RoomId,
        observations: List<OutboxObservation>,
        nowMillis: Long,
    )

    /** Retry on the same route, re-using its transaction id, through the route's own [SendHandle]. */
    suspend fun retry(id: OutboxId, sendHandle: SendHandle?): OutboxRetryResult

    fun observe(sessionId: SessionId, roomId: RoomId): Flow<List<OutboxRecord>>
}
