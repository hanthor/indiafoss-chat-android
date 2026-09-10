/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.conference

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

internal class ConferenceHeaderStateProvider : PreviewParameterProvider<ConferenceHeaderState> {
    override val values: Sequence<ConferenceHeaderState>
        get() = sequenceOf(
            ConferenceHeaderState(hasNativeCompanion = true),
            ConferenceHeaderState(hasNativeCompanion = false),
        )
}
