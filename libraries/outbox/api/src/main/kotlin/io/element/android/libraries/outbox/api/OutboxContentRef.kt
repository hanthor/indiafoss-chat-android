/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.api

/**
 * What was sent, without the content itself. Message bodies are never stored in the outbox
 * (the SDK send queue already persists them) and never logged.
 */
data class OutboxContentRef(
    val kind: OutboxContentKind,
    /** A reference (path or uri) to media owned by another lifecycle, never a copy. */
    val mediaReference: String? = null,
)

enum class OutboxContentKind {
    TEXT,
    REPLY,
    MEDIA,
    OTHER,
}
