/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.startchat.impl.scanqr

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.element.android.features.startchat.StartChatNavigator
import io.element.android.features.startchat.api.StartDMAction
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.core.data.tryOrNull
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.core.parseMeshContactUserId
import io.element.android.libraries.matrix.api.core.toRoomIdOrAlias
import io.element.android.libraries.matrix.api.permalink.PermalinkData
import io.element.android.libraries.matrix.api.permalink.PermalinkParser
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.permissions.api.PermissionsEvent
import io.element.android.libraries.permissions.api.PermissionsPresenter
import kotlinx.coroutines.launch

@AssistedInject
class ScanQrPresenter(
    @Assisted private val navigator: StartChatNavigator,
    private val permalinkParser: PermalinkParser,
    private val startDMAction: StartDMAction,
    permissionsPresenterFactory: PermissionsPresenter.Factory,
) : Presenter<ScanQrState> {
    @AssistedFactory
    interface Factory {
        fun create(navigator: StartChatNavigator): ScanQrPresenter
    }

    private val cameraPermissionPresenter: PermissionsPresenter = permissionsPresenterFactory.create(Manifest.permission.CAMERA)

    @Composable
    override fun present(): ScanQrState {
        val cameraPermissionState = cameraPermissionPresenter.present()
        val coroutineScope = rememberCoroutineScope()
        var scanResult by remember { mutableStateOf<ScanResult>(ScanResult.Scanning) }
        val startDmActionState: MutableState<AsyncAction<RoomId>> = remember { mutableStateOf(AsyncAction.Uninitialized) }

        // Ask for camera access as soon as we land here, if it is not granted yet.
        LaunchedEffect(Unit) {
            if (!cameraPermissionState.permissionGranted) {
                cameraPermissionState.eventSink(PermissionsEvent.RequestPermissions)
            }
        }

        // Leave the flow and open the conversation once the DM is ready.
        val startDmAction = startDmActionState.value
        LaunchedEffect(startDmAction) {
            if (startDmAction is AsyncAction.Success) {
                navigator.onDismissScanQrCode()
                navigator.onRoomCreated(startDmAction.data.toRoomIdOrAlias(), emptyList())
            }
        }

        val isScanning = cameraPermissionState.permissionGranted &&
            scanResult is ScanResult.Scanning &&
            startDmActionState.value is AsyncAction.Uninitialized

        fun onQrCodeScanned(code: ByteArray) {
            // The analyzer keeps firing; ignore anything that arrives while we are not scanning.
            if (!isScanning) return
            val userId = resolveContact(code.decodeToString())
            if (userId == null) {
                scanResult = ScanResult.NotRecognized
                return
            }
            coroutineScope.launch {
                startDMAction.execute(
                    matrixUser = MatrixUser(userId = userId),
                    createIfDmDoesNotExist = true,
                    actionState = startDmActionState,
                )
            }
        }

        fun handleEvent(event: ScanQrEvents) {
            when (event) {
                is ScanQrEvents.QrCodeScanned -> onQrCodeScanned(event.code)
                ScanQrEvents.RequestCameraPermission -> cameraPermissionState.eventSink(PermissionsEvent.RequestPermissions)
                ScanQrEvents.TryAgain -> {
                    scanResult = ScanResult.Scanning
                    startDmActionState.value = AsyncAction.Uninitialized
                }
                ScanQrEvents.CancelStartDM -> startDmActionState.value = AsyncAction.Uninitialized
                ScanQrEvents.Dismiss -> navigator.onDismissScanQrCode()
            }
        }

        return ScanQrState(
            cameraPermissionState = cameraPermissionState,
            isScanning = isScanning,
            scanResult = scanResult,
            startDmAction = startDmActionState.value,
            eventSink = ::handleEvent,
        )
    }

    // Try the standard permalink first (matrix.to / matrix: URIs), then the mesh contact
    // payloads it does not handle (a raw mesh MXID or the companion's mesh vCard line).
    private fun resolveContact(raw: String): UserId? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val fromPermalink = (tryOrNull { permalinkParser.parse(text) } as? PermalinkData.UserLink)?.userId
        return fromPermalink ?: parseMeshContactUserId(text)
    }
}
