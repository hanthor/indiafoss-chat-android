/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.matrix.api.core.displayId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.ui.strings.CommonStrings

fun MatrixUser.getAvatarData(size: AvatarSize) = AvatarData(
    id = userId.value,
    name = displayName,
    url = avatarUrl,
    size = size,
)

fun MatrixUser.getBestName(): String {
    // On the mesh a nameless user's id is a 64-hex node id; show the short
    // code instead of the raw hex (ADR 0008).
    return displayName?.takeIf { it.isNotEmpty() } ?: userId.displayId
}

@Composable
fun MatrixUser.getFullName(): String {
    return displayName.let { name ->
        if (name.isNullOrBlank()) {
            userId.displayId
        } else {
            stringResource(CommonStrings.common_name_and_id, name, userId.displayId)
        }
    }
}
