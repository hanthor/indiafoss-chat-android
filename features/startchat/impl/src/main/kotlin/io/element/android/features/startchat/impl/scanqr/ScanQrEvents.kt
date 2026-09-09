/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.startchat.impl.scanqr

sealed interface ScanQrEvents {
    data class QrCodeScanned(val code: ByteArray) : ScanQrEvents
    data object RequestCameraPermission : ScanQrEvents
    data object TryAgain : ScanQrEvents
    data object CancelStartDM : ScanQrEvents
    data object Dismiss : ScanQrEvents
}
