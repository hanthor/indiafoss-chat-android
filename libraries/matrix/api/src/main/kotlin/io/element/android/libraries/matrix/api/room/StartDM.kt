/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.room

import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.core.isMeshUser

/**
 * Try to find an existing DM with the given user, or create one if none exists and [createIfDmDoesNotExist] is true.
 */
suspend fun MatrixClient.startDM(
    userId: UserId,
    createIfDmDoesNotExist: Boolean,
): StartDMResult {
    return findDM(userId)
        .fold(
            onSuccess = { existingDM ->
                if (existingDM != null) {
                    StartDMResult.Success(existingDM, isNew = false)
                } else if (createIfDmDoesNotExist) {
                    // On a mesh session, refuse to *create* an encrypted DM with a
                    // user whose homeserver the mesh cannot reach. The room event
                    // would eventually cross via a gateway's copy, but the Megolm
                    // key share is a to-device EDU — origin to destination only,
                    // no relay — so the invitee would sit on "waiting for the
                    // key" forever. A clear refusal now beats a hung padlock at
                    // the venue; opening an *existing* DM stays allowed.
                    if (dmWouldBeKeyDead(sessionId, userId)) {
                        return@fold StartDMResult.Failure(MeshUnreachableDmException(userId))
                    }
                    createDM(userId).fold(
                        { StartDMResult.Success(it, isNew = true) },
                        { StartDMResult.Failure(it) }
                    )
                } else {
                    StartDMResult.DmDoesNotExist
                }
            },
            onFailure = { error ->
                StartDMResult.Failure(error)
            }
        )
}

sealed interface StartDMResult {
    data class Success(val roomId: RoomId, val isNew: Boolean) : StartDMResult
    data object DmDoesNotExist : StartDMResult
    data class Failure(val throwable: Throwable) : StartDMResult
}

/**
 * True when this session is a mesh node and [invitee] lives on a server the
 * mesh cannot deliver key material to. A mesh server name is the node's ed25519
 * public key as 64 lowercase hex characters (see `isMeshServerName`); everything
 * else is an internet homeserver the mesh has no route to.
 *
 * DMs are created encrypted, and encryption needs to-device key shares, which —
 * unlike room events — have no store-and-forward path through a gateway. Pure
 * and internal so the rule is testable without a client; see the companion
 * project's issue #176 for the seam this guards.
 */
internal fun dmWouldBeKeyDead(localUserId: UserId, invitee: UserId): Boolean =
    localUserId.isMeshUser && !invitee.isMeshUser

/** Refused before creating: the key share could never be delivered. */
class MeshUnreachableDmException(invitee: UserId) : Exception(
    "Cannot start an encrypted chat with ${invitee.value} from the mesh: " +
        "their server is only reachable over the internet, and message keys " +
        "cannot be relayed. Try again when this phone is online."
)
