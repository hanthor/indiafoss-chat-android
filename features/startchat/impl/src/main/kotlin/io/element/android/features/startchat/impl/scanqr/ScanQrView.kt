/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.startchat.impl.scanqr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.startchat.impl.R
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.permissions.api.PermissionsView
import io.element.android.libraries.qrcode.QrCodeCameraView
import io.element.android.libraries.ui.strings.CommonStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQrView(
    state: ScanQrState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                titleStr = stringResource(R.string.screen_start_chat_scan_qr_title),
                navigationIcon = {
                    BackButton(
                        imageVector = CompoundIcons.Close(),
                        onClick = onBackClick,
                    )
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            if (state.cameraPermissionState.permissionGranted) {
                ScannerContent(state = state)
            } else {
                NoCameraPermissionContent(state = state)
            }
        }
    }

    PermissionsView(
        title = stringResource(R.string.screen_start_chat_scan_qr_camera_permission_title),
        content = stringResource(R.string.screen_start_chat_scan_qr_camera_permission_description),
        icon = { Icon(imageVector = CompoundIcons.TakePhotoSolid(), contentDescription = null) },
        state = state.cameraPermissionState,
    )
}

@Composable
private fun ScannerContent(
    state: ScanQrState,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        QrCodeCameraView(
            modifier = Modifier.fillMaxSize(),
            onScanQrCode = { state.eventSink(ScanQrEvents.QrCodeScanned(it)) },
            isScanning = state.isScanning,
        )
        when {
            state.startDmAction is AsyncAction.Loading -> {
                StatusOverlay {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = stringResource(CommonStrings.common_starting_chat),
                        textAlign = TextAlign.Center,
                        color = ElementTheme.colors.textPrimary,
                        style = ElementTheme.typography.fontBodyMdRegular,
                    )
                }
            }
            state.startDmAction is AsyncAction.Failure -> {
                RetryOverlay(
                    message = stringResource(R.string.screen_start_chat_error_starting_chat),
                    onTryAgain = { state.eventSink(ScanQrEvents.TryAgain) },
                )
            }
            state.scanResult is ScanResult.NotRecognized -> {
                RetryOverlay(
                    message = stringResource(R.string.screen_start_chat_scan_qr_invalid),
                    onTryAgain = { state.eventSink(ScanQrEvents.TryAgain) },
                )
            }
        }
    }
}

@Composable
private fun NoCameraPermissionContent(
    state: ScanQrState,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            imageVector = CompoundIcons.TakePhotoSolid(),
            contentDescription = null,
            tint = ElementTheme.colors.iconSecondary,
        )
        Text(
            text = stringResource(R.string.screen_start_chat_scan_qr_camera_permission_description),
            textAlign = TextAlign.Center,
            color = ElementTheme.colors.textSecondary,
            style = ElementTheme.typography.fontBodyMdRegular,
        )
        Button(
            text = stringResource(R.string.screen_start_chat_scan_qr_grant_permission_action),
            onClick = { state.eventSink(ScanQrEvents.RequestCameraPermission) },
        )
    }
}

@Composable
private fun StatusOverlay(
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ElementTheme.colors.bgCanvasDefault.copy(alpha = 0.9f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
private fun RetryOverlay(
    message: String,
    onTryAgain: () -> Unit,
) {
    StatusOverlay {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = ElementTheme.colors.textCriticalPrimary,
            style = ElementTheme.typography.fontBodyMdMedium,
        )
        Button(
            text = stringResource(R.string.screen_start_chat_scan_qr_try_again),
            onClick = onTryAgain,
        )
    }
}

@PreviewsDayNight
@Composable
internal fun ScanQrViewPreview(@PreviewParameter(ScanQrStateProvider::class) state: ScanQrState) = ElementPreview {
    ScanQrView(
        state = state,
        onBackClick = {},
    )
}
