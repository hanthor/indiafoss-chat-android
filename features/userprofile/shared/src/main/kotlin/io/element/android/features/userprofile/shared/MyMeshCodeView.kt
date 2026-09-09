/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.userprofile.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.qrcode.QrCodeImage

/**
 * The current user's own mesh contact code, shown so someone standing next to them can
 * scan it and start a chat (ADR 0008 Phase 2). The QR carries the full, verifiable
 * identity; the short code below it is only a human echo to eyeball against.
 */
@Composable
fun MyMeshCodeView(
    qrCodeData: String,
    shortCode: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.screen_user_profile_my_mesh_code_title),
            textAlign = TextAlign.Center,
            style = ElementTheme.typography.fontHeadingSmMedium,
            color = ElementTheme.colors.textPrimary,
        )
        QrCodeImage(
            modifier = Modifier.size(220.dp),
            data = qrCodeData,
        )
        if (shortCode != null) {
            Text(
                text = shortCode,
                textAlign = TextAlign.Center,
                style = ElementTheme.typography.fontHeadingMdBold,
                color = ElementTheme.colors.textPrimary,
            )
        }
        Text(
            text = stringResource(R.string.screen_user_profile_my_mesh_code_caption),
            textAlign = TextAlign.Center,
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
        )
    }
}

@PreviewsDayNight
@Composable
internal fun MyMeshCodeViewPreview() = ElementPreview {
    MyMeshCodeView(
        qrCodeData = "matrix:u/n:845aa456897e",
        shortCode = "897E·845A",
    )
}
