/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.ftue.impl.discovery

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.libraries.architecture.AsyncAction

open class DiscoveryOptInStateProvider : PreviewParameterProvider<DiscoveryOptInState> {
    override val values: Sequence<DiscoveryOptInState>
        get() = sequenceOf(
            aDiscoveryOptInState(),
            aDiscoveryOptInState(submitAction = AsyncAction.Loading),
        )
}

fun aDiscoveryOptInState(
    submitAction: AsyncAction<Unit> = AsyncAction.Uninitialized,
    eventSink: (DiscoveryOptInEvents) -> Unit = {},
) = DiscoveryOptInState(
    submitAction = submitAction,
    eventSink = eventSink,
)
