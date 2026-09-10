/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.api

sealed interface OutboxRetryResult {
    /** The route accepted a retry with its existing transaction id. */
    data object Retried : OutboxRetryResult

    /** The route asked for a retry and refused; the record state is unchanged. */
    data class RouteRefused(val reason: String) : OutboxRetryResult

    /**
     * The route has no trace of the send any more, so a retry would be a new send and could deliver
     * twice. Only an explicit continuation (step 2 of #48) may proceed from here.
     */
    data object NoRouteHandle : OutboxRetryResult

    /** The record is not in a retryable state. */
    data object NotRetryable : OutboxRetryResult

    data object NotFound : OutboxRetryResult
}
