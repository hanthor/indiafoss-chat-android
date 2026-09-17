/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.invite.impl

import io.element.android.features.invite.api.KnownContactsStore
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.coroutines.CoroutineScope

interface KnownContactsStoreFactory {
    fun getOrCreate(
        sessionId: SessionId,
        sessionCoroutineScope: CoroutineScope,
    ): KnownContactsStore
}
