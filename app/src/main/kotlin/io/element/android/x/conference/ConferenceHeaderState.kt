/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.conference

/** [hasNativeCompanion]: the native Companion is installed, so the action hands off instead of opening the browser. */
data class ConferenceHeaderState(
    val hasNativeCompanion: Boolean,
)
