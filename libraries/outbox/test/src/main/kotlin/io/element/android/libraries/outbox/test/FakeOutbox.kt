/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.test

import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SendHandle
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.SendQueueUpdate
import io.element.android.libraries.outbox.api.Outbox
import io.element.android.libraries.outbox.api.OutboxContentRef
import io.element.android.libraries.outbox.api.OutboxId
import io.element.android.libraries.outbox.api.OutboxObservation
import io.element.android.libraries.outbox.api.OutboxRecord
import io.element.android.libraries.outbox.api.OutboxRetryResult
import io.element.android.libraries.outbox.api.OutboxState
import io.element.android.tests.testutils.lambda.lambdaError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeOutbox(
    val recordsFlow: MutableStateFlow<List<OutboxRecord>> = MutableStateFlow(emptyList()),
    var retryLambda: (OutboxId, SendHandle?) -> OutboxRetryResult = { _, _ -> lambdaError() },
) : Outbox {
    val sent = mutableListOf<OutboxContentRef>()
    val reconciled = mutableListOf<List<OutboxObservation>>()
    val sendQueueUpdates = mutableListOf<SendQueueUpdate>()

    override suspend fun send(
        sessionId: SessionId,
        roomId: RoomId,
        contentRef: OutboxContentRef,
        dispatch: suspend () -> Result<Unit>,
    ): OutboxRecord {
        sent.add(contentRef)
        val dispatched = dispatch()
        return anOutboxRecord(
            sessionId = sessionId,
            roomId = roomId,
            contentRef = contentRef,
            state = if (dispatched.isSuccess) OutboxState.Queued else OutboxState.Failed("dispatch", retryable = true),
        )
    }

    override suspend fun onSendQueueUpdate(sessionId: SessionId, roomId: RoomId, update: SendQueueUpdate) {
        sendQueueUpdates.add(update)
    }

    override suspend fun reconcile(sessionId: SessionId, roomId: RoomId, observations: List<OutboxObservation>, nowMillis: Long) {
        reconciled.add(observations)
    }

    override suspend fun retry(id: OutboxId, sendHandle: SendHandle?): OutboxRetryResult = retryLambda(id, sendHandle)

    override fun observe(sessionId: SessionId, roomId: RoomId): Flow<List<OutboxRecord>> = recordsFlow
}
