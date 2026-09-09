/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.startchat.impl.scanqr

import android.Manifest
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.permissions.api.PermissionsState
import io.element.android.libraries.permissions.api.aPermissionsState

open class ScanQrStateProvider : PreviewParameterProvider<ScanQrState> {
    override val values: Sequence<ScanQrState>
        get() = sequenceOf(
            aScanQrState(),
            aScanQrState(
                cameraPermissionState = aPermissionsState(
                    showDialog = false,
                    permissionGranted = false,
                    permission = Manifest.permission.CAMERA,
                ),
            ),
            aScanQrState(scanResult = ScanResult.NotRecognized),
            aScanQrState(isScanning = false, startDmAction = AsyncAction.Loading),
            aScanQrState(isScanning = false, startDmAction = AsyncAction.Failure(Exception("Boom"))),
        )
}

fun aScanQrState(
    cameraPermissionState: PermissionsState = aPermissionsState(
        showDialog = false,
        permissionGranted = true,
        permission = Manifest.permission.CAMERA,
    ),
    isScanning: Boolean = true,
    scanResult: ScanResult = ScanResult.Scanning,
    startDmAction: AsyncAction<RoomId> = AsyncAction.Uninitialized,
    eventSink: (ScanQrEvents) -> Unit = {},
) = ScanQrState(
    cameraPermissionState = cameraPermissionState,
    isScanning = isScanning,
    scanResult = scanResult,
    startDmAction = startDmAction,
    eventSink = eventSink,
)
