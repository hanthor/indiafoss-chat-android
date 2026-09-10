/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.conference

import androidx.compose.ui.graphics.Color

/**
 * IndiaFOSS 2026 event colours, fixed in both themes. They live only on this
 * conference entry and the launch assets: conversations, dialogs and status
 * colours stay with Compound semantics. Source values are the official 2026
 * stylesheet primaries; see docs/indiafoss/branding-2026.md.
 */
internal object ConferenceIdentity {
    /** `hsl(144 92% 37%)`: the Companion's Material seed. */
    val Mint = Color(0xFF0FB556)

    /** `hsl(145 63% 18%)`: the launcher icon ground. */
    val Ink = Color(0xFF114B29)

    /** `hsl(145 92% 86%)`. */
    val PaleGreen = Color(0xFFBAFCD6)
}
