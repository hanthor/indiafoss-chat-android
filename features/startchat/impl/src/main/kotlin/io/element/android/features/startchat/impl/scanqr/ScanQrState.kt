/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.startchat.impl.scanqr

import androidx.compose.runtime.Immutable
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.permissions.api.PermissionsState

data class ScanQrState(
    val cameraPermissionState: PermissionsState,
    val isScanning: Boolean,
    val scanResult: ScanResult,
    val startDmAction: AsyncAction<RoomId>,
    val eventSink: (ScanQrEvents) -> Unit,
)

@Immutable
sealed interface ScanResult {
    data object Scanning : ScanResult
    data object NotRecognized : ScanResult
}
