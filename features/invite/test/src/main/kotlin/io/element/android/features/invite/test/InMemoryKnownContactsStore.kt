/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.invite.test

import io.element.android.features.invite.api.KnownContactsStore
import io.element.android.libraries.matrix.api.core.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryKnownContactsStore(
    initialUserIds: Set<UserId> = emptySet(),
) : KnownContactsStore {
    private val userIds = MutableStateFlow(initialUserIds)

    override fun knownUserIds(): Flow<Set<UserId>> = userIds

    override suspend fun markKnown(userId: UserId) {
        userIds.value += userId
    }

    override suspend fun clear() {
        userIds.value = emptySet()
    }
}
