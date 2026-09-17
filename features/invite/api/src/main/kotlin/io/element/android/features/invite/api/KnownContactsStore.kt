/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.invite.api

import io.element.android.libraries.matrix.api.core.UserId
import kotlinx.coroutines.flow.Flow

/**
 * People this session has met on purpose: a card scanned in the app, or an
 * invite the user accepted. Mesh identities are visible to anyone nearby, so
 * an invite from a mesh user who is not in here is treated as a contact
 * request rather than a chat. Nothing on the wire writes to this store.
 */
interface KnownContactsStore {
    fun knownUserIds(): Flow<Set<UserId>>

    suspend fun markKnown(userId: UserId)

    suspend fun clear()
}
