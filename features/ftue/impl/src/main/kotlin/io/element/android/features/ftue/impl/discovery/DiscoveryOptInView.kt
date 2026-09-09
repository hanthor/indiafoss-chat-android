/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.ftue.impl.discovery

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.ftue.impl.R
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.atomic.molecules.ButtonColumnMolecule
import io.element.android.libraries.designsystem.atomic.molecules.IconTitleSubtitleMolecule
import io.element.android.libraries.designsystem.atomic.pages.HeaderFooterPage
import io.element.android.libraries.designsystem.background.OnboardingBackground
import io.element.android.libraries.designsystem.components.BigIcon
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.OutlinedButton

@Composable
fun DiscoveryOptInView(
    state: DiscoveryOptInState,
    modifier: Modifier = Modifier,
) {
    val isLoading = state.submitAction is AsyncAction.Loading
    HeaderFooterPage(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize(),
        background = { OnboardingBackground() },
        header = { DiscoveryOptInHeader(modifier = Modifier.padding(top = 60.dp, bottom = 28.dp)) },
        footer = { DiscoveryOptInFooter(state = state, isLoading = isLoading) },
    )
}

@Composable
private fun DiscoveryOptInHeader(
    modifier: Modifier = Modifier,
) {
    IconTitleSubtitleMolecule(
        modifier = modifier,
        title = stringResource(R.string.screen_discovery_opt_in_title),
        subTitle = stringResource(R.string.screen_discovery_opt_in_subtitle),
        iconStyle = BigIcon.Style.Default(CompoundIcons.VisibilityOn()),
    )
}

@Composable
private fun DiscoveryOptInFooter(
    state: DiscoveryOptInState,
    isLoading: Boolean,
) {
    ButtonColumnMolecule {
        Button(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.screen_discovery_opt_in_discoverable_action),
            enabled = !isLoading,
            onClick = { state.eventSink(DiscoveryOptInEvents.Choose(discoverable = true)) },
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.screen_discovery_opt_in_hidden_action),
            enabled = !isLoading,
            onClick = { state.eventSink(DiscoveryOptInEvents.Choose(discoverable = false)) },
        )
    }
}

@PreviewsDayNight
@Composable
internal fun DiscoveryOptInViewPreview(
    @PreviewParameter(DiscoveryOptInStateProvider::class) state: DiscoveryOptInState,
) {
    ElementPreview {
        DiscoveryOptInView(state = state)
    }
}
