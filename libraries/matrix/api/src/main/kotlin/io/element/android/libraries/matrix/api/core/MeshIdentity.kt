/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.core

/**
 * Mesh identity helpers (ADR 0008).
 *
 * On the venue mesh a user's server-name is the embedded node's 64-hex iroh
 * node id (the ed25519 public key, hex-encoded), so a mesh user id looks like
 * `@n:845aa456…897e`. That full id is the real, addressed, verifiable identity
 * and must never be trusted in an abbreviated form. These helpers only make it
 * *legible*: a short code the eye can match and a stable seed for a badge/colour.
 *
 * The localpart is deliberately ignored — it is host-stamped ("n" by the medium,
 * "alice" by the bare server) and carries no identity. Detection keys off the
 * server-name shape alone.
 */

/** A 64-character lowercase-hex server-name is a mesh node id. */
private val MESH_SERVER_NAME = Regex("^[0-9a-f]{64}$")

/**
 * The server-name when this id belongs to a mesh node, else null. Never empty.
 */
val UserId.meshNodeId: String?
    get() = domainName?.takeIf { MESH_SERVER_NAME.matches(it) }

/** True when this user id is a mesh identity (`@<localpart>:<64-hex>`). */
val UserId.isMeshUser: Boolean
    get() = meshNodeId != null

/**
 * A short, human-readable echo of a mesh identity: the last 8 hex of the node
 * id, upper-cased and grouped `ABCD·EF01`.
 *
 * This is a *recognition and echo* aid only — 32 bits is trivially collidable,
 * so it is a check against an identity you already hold in full ("their screen
 * shows the same code"), never a way to add or establish one. Returns null for
 * a non-mesh id.
 */
val UserId.meshShortCode: String?
    get() = meshNodeId?.let { nodeId ->
        val tail = nodeId.takeLast(8).uppercase()
        "${tail.substring(0, 4)}·${tail.substring(4, 8)}"
    }

/**
 * The id string to show a human: a mesh identity's short code, or the plain
 * user id off the mesh. For list subtitles and any place that would otherwise
 * print a raw 64-hex node id.
 */
val UserId.displayId: String
    get() = meshShortCode ?: value
