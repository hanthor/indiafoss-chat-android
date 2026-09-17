/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.core

/*
 * Parsing a scanned "add me on the mesh" payload into a UserId (ADR 0008,
 * Phase 2 of the add-by-QR flow).
 *
 * The scanner tries the standard PermalinkParser first — it already turns a
 * matrix.to link or a `matrix:` URI into a UserId. This covers the payloads it
 * does NOT: a raw mesh MXID typed or pasted, and the companion's `/connect`
 * vCard, whose mesh node id rides on an `X-INDIAFOSS-MESH` line (older cards
 * spell it `X-NEUTRINO-SERVER-NAME`). Either way the result is the same 64-hex
 * server-name identity, and — as everywhere in ADR 0008 — the full id is the
 * anchor; nothing here trusts a short code.
 */

// vCard properties (any case) that carry a 64-hex mesh node id as their value.
private val MESH_VCARD_PROPERTIES = setOf("x-indiafoss-mesh", "x-neutrino-server-name")

private val MESH_HEX = Regex("^[0-9a-f]{64}$")

/**
 * A [UserId] parsed from a scanned mesh contact payload that [PermalinkParser]
 * does not handle — a raw mesh MXID or the companion's mesh vCard line — or
 * null if the text carries no mesh identity. Try `PermalinkParser` first (for
 * matrix.to / `matrix:` URIs), this second.
 */
fun parseMeshContactUserId(raw: String): UserId? {
    val text = raw.trim()
    if (text.isEmpty()) return null

    // A raw mesh MXID: `@<localpart>:<64-hex>`. Validate the shape with the same
    // non-throwing check UserId's own init uses (so constructing it can't throw),
    // then keep it only if the server-name is a mesh node id. The localpart is
    // not trusted (host-stamped), only the server-name shape.
    if (!text.contains('\n') && MatrixPatterns.isUserId(text)) {
        val userId = UserId(text)
        if (userId.isMeshUser) return userId
    }

    // A vCard (or any line-based payload): the first line carrying a mesh node
    // id wins. The localpart is always "n" on the mesh.
    return text.lineSequence()
        .firstNotNullOfOrNull { meshNodeIdFromVCardLine(it) }
        ?.let { UserId("@n:$it") }
}

// The 64-hex mesh node id on a vCard `PROPERTY[;params]:value` line, or null if
// the line is not a mesh property or its value is not a node id.
private fun meshNodeIdFromVCardLine(line: String): String? {
    val colon = line.indexOf(':')
    if (colon <= 0) return null
    val property = line.substring(0, colon).substringBefore(';').trim().lowercase()
    if (property !in MESH_VCARD_PROPERTIES) return null
    return line.substring(colon + 1).trim().lowercase().takeIf { MESH_HEX.matches(it) }
}

private const val FRIEND_PREFIX = "indiafoss://friend"
private const val CHAT_PREFIX = "indiafoss://chat"
private const val MATRIX_VCARD_PROPERTY = "x-indiafoss-matrix"

/**
 * A [UserId] from any contact payload the companion can show as a QR that
 * [PermalinkParser] does not handle: a raw MXID, the companion's signed vCard
 * (its `X-INDIAFOSS-MATRIX` line first, then the mesh line), the app-aware
 * `indiafoss://friend?v=1&matrix_id=…|neutrino_server_name=…` card, or an
 * `indiafoss://chat?dm=…` handoff. Nothing here is trusted: the result is an
 * address to preview, never an identity.
 */
fun parseScannedContactUserId(raw: String): UserId? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    if (!text.contains('\n') && MatrixPatterns.isUserId(text)) return UserId(text)
    val lower = text.lowercase()
    if (lower.startsWith(FRIEND_PREFIX) || lower.startsWith(CHAT_PREFIX)) return indiafossLinkUserId(text)
    return text.lineSequence().firstNotNullOfOrNull { matrixIdFromVCardLine(it) }
        ?: parseMeshContactUserId(text)
}

private fun indiafossLinkUserId(link: String): UserId? {
    val query = link.substringAfter('?', missingDelimiterValue = "")
    if (query.isEmpty()) return null
    val params = query.split('&').mapNotNull { pair ->
        val key = pair.substringBefore('=')
        val value = pair.substringAfter('=', missingDelimiterValue = "")
        if (key.isEmpty()) null else key to percentDecode(value)
    }.toMap()
    val isFriend = link.lowercase().startsWith(FRIEND_PREFIX)
    if (isFriend && params["v"] != "1") return null
    val matrixId = if (isFriend) params["matrix_id"] else params["dm"]
    matrixId?.takeIf { MatrixPatterns.isUserId(it) }?.let { return UserId(it) }
    if (!isFriend) return null
    return params["neutrino_server_name"]?.lowercase()?.takeIf { MESH_HEX.matches(it) }?.let { UserId("@n:$it") }
}

// Query-string decoding without exceptions: a stray '%' stays as it is, and the
// id check afterwards rejects it. Only ASCII escapes matter for an MXID.
private fun percentDecode(value: String): String {
    val out = StringBuilder(value.length)
    var i = 0
    while (i < value.length) {
        val c = value[i]
        val hex = if (c == '%' && i + 2 < value.length) value.substring(i + 1, i + 3) else null
        val code = hex?.toIntOrNull(16)
        when {
            code != null -> {
                out.append(code.toChar())
                i += 3
            }
            c == '+' -> {
                out.append(' ')
                i++
            }
            else -> {
                out.append(c)
                i++
            }
        }
    }
    return out.toString()
}

private fun matrixIdFromVCardLine(line: String): UserId? {
    val colon = line.indexOf(':')
    if (colon <= 0) return null
    val property = line.substring(0, colon).substringBefore(';').trim().lowercase()
    if (property != MATRIX_VCARD_PROPERTY) return null
    return line.substring(colon + 1).trim().takeIf { MatrixPatterns.isUserId(it) }?.let { UserId(it) }
}
