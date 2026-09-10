/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.api

import androidx.compose.runtime.Immutable

/** What the timeline needs from a record: which record, and its truthful state. */
@Immutable
data class OutboxSummary(
    val id: OutboxId,
    val state: OutboxState,
)
