/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.ftue.impl.discovery

import io.element.android.libraries.architecture.AsyncAction

/*
 * @param submitAction progress of persisting the discoverability choice.
 * @param eventSink callback used to send [DiscoveryOptInEvents] to the presenter.
 */
data class DiscoveryOptInState(
    val submitAction: AsyncAction<Unit>,
    val eventSink: (DiscoveryOptInEvents) -> Unit,
)
