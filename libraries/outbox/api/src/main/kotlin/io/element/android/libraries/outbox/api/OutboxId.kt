/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.api

/**
 * Client-generated identifier of one logical send. It is stable across restarts, retries and
 * (in later steps of #48) route changes, unlike the per-route transaction ids.
 */
@JvmInline
value class OutboxId(val value: String) {
    override fun toString(): String = value
}
