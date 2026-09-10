/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.services.neutrino.test

import io.element.android.services.neutrino.api.CaptureResult
import io.element.android.services.neutrino.api.DiscoverableResult
import io.element.android.services.neutrino.api.DiscoveredPeer
import io.element.android.services.neutrino.api.NeutrinoService

class FakeNeutrinoService(
    private val serverNameResult: String? = "a1b2c3d4",
    private val lastErrorResult: String? = null,
    private val discoveredPeersResult: () -> List<DiscoveredPeer> = { emptyList() },
    private val startCaptureResult: () -> CaptureResult = { CaptureResult.Started(A_CAPTURE_PATH) },
    // The location stopCapture reports (the real service copies to Downloads, so
    // this need not match the started path).
    private val stopCaptureResult: String? = "Download/$A_CAPTURE_NAME",
    // Whether this "build" carries the set_discoverable binding. When false,
    // setDiscoverable answers Unavailable regardless of setDiscoverableResult.
    private val discoverabilityControlAvailable: Boolean = true,
    private val setDiscoverableResult: (Boolean) -> DiscoverableResult = { DiscoverableResult.Applied },
) : NeutrinoService {
    private var capturing = false

    /** The `discoverable` argument of each [start] call, in order. */
    val startCalls = mutableListOf<Boolean>()

    /** The argument of each [setDiscoverable] call, in order. */
    val setDiscoverableCalls = mutableListOf<Boolean>()

    override fun start(discoverable: Boolean) {
        startCalls += discoverable
    }

    override suspend fun awaitReady(timeoutMs: Long) = Unit

    override fun isRunning(): Boolean = false

    override fun serverName(): String? = serverNameResult

    override fun lastError(): String? = lastErrorResult

    override fun discoveredPeers(): List<DiscoveredPeer> = discoveredPeersResult()

    override fun startCapture(): CaptureResult = startCaptureResult().also { result ->
        if (result is CaptureResult.Started) {
            capturing = true
        }
    }

    override fun stopCapture(): String? {
        if (!capturing) return null
        capturing = false
        return stopCaptureResult
    }

    override fun isCapturing(): Boolean = capturing

    override fun isDiscoverabilityControlAvailable(): Boolean = discoverabilityControlAvailable

    override suspend fun setDiscoverable(discoverable: Boolean): DiscoverableResult {
        setDiscoverableCalls += discoverable
        return if (discoverabilityControlAvailable) setDiscoverableResult(discoverable) else DiscoverableResult.Unavailable
    }
}

private const val A_CAPTURE_NAME = "neutrino-fed.pcap"
private const val A_CAPTURE_PATH = "/storage/emulated/0/Android/data/pkg/files/$A_CAPTURE_NAME"
