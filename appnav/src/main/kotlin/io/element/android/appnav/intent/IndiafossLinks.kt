/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appnav.intent

import android.net.Uri
import androidx.core.net.toUri
import io.element.android.libraries.core.data.tryOrNull

/**
 * Translates the IndiaFOSS Companion's reserved `indiafoss://` payloads into
 * `matrix.to` permalinks so the existing permalink navigation (user profile
 * with "Send message", room join) handles them. Nothing is sent or joined
 * automatically: a user link lands on the profile screen and a room link on
 * the join confirmation, exactly like a scanned matrix.to QR code.
 *
 * Supported payloads (shared contract with the PWA, see docs/messaging.md there):
 * - `indiafoss://chat?dm=@user:server`      → user permalink
 * - `indiafoss://chat?join=#alias:server`   → room permalink (aliases or room ids)
 * - `indiafoss://friend?v=1&matrix_id=…`    → user permalink; when only a
 *   `neutrino_server_name` is present the P2P address `@n:<server_name>` is used.
 */
object IndiafossLinks {
    private const val SCHEME = "indiafoss"
    private val USER_ID = Regex("^@[^:\\s]+:[^\\s]+$")
    private val ROOM_TARGET = Regex("^[#!][^:\\s]+:[^\\s]+$")
    private val NEUTRINO_SERVER_NAME = Regex("^[0-9a-fA-F]{64}$")

    /** `matrix.to` permalink for a supported payload, or `null` when this is not one. */
    fun toMatrixTo(uriString: String): String? {
        val uri = tryOrNull { uriString.toUri() } ?: return null
        if (uri.scheme?.lowercase() != SCHEME) return null
        return when (uri.host?.lowercase()) {
            "chat" -> chatTarget(uri)
            "friend" -> friendTarget(uri)
            else -> null
        }?.let { "https://matrix.to/#/${Uri.encode(it)}" }
    }

    private fun chatTarget(uri: Uri): String? {
        uri.getQueryParameter("dm")?.takeIf { USER_ID.matches(it) }?.let { return it }
        uri.getQueryParameter("join")?.takeIf { ROOM_TARGET.matches(it) }?.let { return it }
        return null
    }

    private fun friendTarget(uri: Uri): String? {
        if (uri.getQueryParameter("v") != "1") return null
        uri.getQueryParameter("matrix_id")?.takeIf { USER_ID.matches(it) }?.let { return it }
        uri.getQueryParameter("neutrino_server_name")
            ?.takeIf { NEUTRINO_SERVER_NAME.matches(it) }
            ?.let { return "@n:${it.lowercase()}" }
        return null
    }
}
