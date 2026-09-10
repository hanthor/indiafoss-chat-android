/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.conference

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.CommonDrawables
import io.element.android.x.R

/**
 * Event-identity strip above the hosted Companion with the one action the
 * issue asks for: open the native Companion when it is installed, else the
 * same route in the browser.
 */
@Composable
fun ConferenceHeader(
    state: ConferenceHeaderState,
    onOpenCompanionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasNativeCompanion = state.hasNativeCompanion
    Column(
        modifier = modifier
            .background(ConferenceIdentity.Ink)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                resourceId = CommonDrawables.ic_notification,
                contentDescription = null,
                tint = ConferenceIdentity.Mint,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.screen_conference_header_title),
                    style = ElementTheme.typography.fontHeadingSmMedium,
                    color = Color.White,
                )
                Text(
                    text = stringResource(R.string.screen_conference_header_subtitle),
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ConferenceIdentity.PaleGreen,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                onClick = onOpenCompanionClick,
                shape = RoundedCornerShape(20.dp),
                color = if (hasNativeCompanion) ConferenceIdentity.Mint else ConferenceIdentity.PaleGreen,
                contentColor = ConferenceIdentity.Ink,
            ) {
                Text(
                    text = stringResource(
                        if (hasNativeCompanion) R.string.action_open_companion else R.string.action_open_in_browser
                    ),
                    style = ElementTheme.typography.fontBodyMdMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(ConferenceIdentity.Mint),
        )
    }
}

@PreviewsDayNight
@Composable
internal fun ConferenceHeaderPreview(
    @PreviewParameter(ConferenceHeaderStateProvider::class) state: ConferenceHeaderState,
) = ElementPreview {
    ConferenceHeader(
        state = state,
        onOpenCompanionClick = {},
    )
}
