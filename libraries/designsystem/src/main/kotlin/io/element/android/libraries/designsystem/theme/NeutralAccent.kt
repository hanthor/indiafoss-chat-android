/*
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.theme

import androidx.compose.ui.graphics.Color
import io.element.android.compound.tokens.generated.SemanticColors

/**
 * The IndiaFOSS Chat identity: no brand colour of its own. Element's green
 * accent is remapped to monochrome, so the app is either *uncolored* (this)
 * or *dynamic* (Material You from the wallpaper) — never a third party's
 * green. Applied to the Compound [SemanticColors] before they reach the
 * theme, which is also the source the Material 3 scheme is derived from, so
 * one transform recolours both.
 *
 * Light theme accents become near-black (ink on paper); dark theme accents
 * become near-white. Success/badge greens are left alone — those carry
 * meaning (delivered, online) rather than brand.
 */
internal fun SemanticColors.withNeutralAccent(): SemanticColors {
    val ink = if (isLight) Color(0xFF1B1B1B) else Color(0xFFE6E6E6)
    val inkHovered = if (isLight) Color(0xFF000000) else Color(0xFFFFFFFF)
    val inkSubtle = if (isLight) Color(0x14000000) else Color(0x1FFFFFFF)
    return copy(
        bgAccentRest = ink,
        bgAccentHovered = inkHovered,
        bgAccentPressed = inkHovered,
        bgAccentSelected = inkSubtle,
        textActionAccent = ink,
        iconAccentPrimary = ink,
        iconAccentTertiary = ink,
        borderAccentPrimary = ink,
        borderAccentSubtle = inkSubtle,
    )
}
