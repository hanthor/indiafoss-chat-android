/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.api

/**
 * A transport a logical send can be dispatched on. Each route owns its own idempotency key
 * (for Matrix, the transaction id chosen by the SDK send queue), so a retry on a route re-uses
 * that route's key and never produces a duplicate on that route.
 *
 * Only the SDK send queue exists today. Mesh (Neutrino) sends go through the very same queue,
 * against the embedded homeserver, so they are not a distinct route from the outbox's point of view.
 */
enum class OutboxRoute {
    MATRIX_SEND_QUEUE,
}
