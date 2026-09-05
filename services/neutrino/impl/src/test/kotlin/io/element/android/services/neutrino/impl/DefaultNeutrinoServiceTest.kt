/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.services.neutrino.impl

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.element.android.services.neutrino.api.CaptureResult
import io.element.android.services.neutrino.api.NetworkAddressProvider
import io.element.neutrino.DiscoveredPeer as NativeDiscoveredPeer
import io.mockk.mockk
import org.junit.Test

class DefaultNeutrinoServiceTest {
    private val context = mockk<Context>(relaxed = true)
    private val networkAddressProvider = mockk<NetworkAddressProvider>(relaxed = true)

    @Test
    fun `when uninitialized isRunning returns false`() {
        val service = DefaultNeutrinoService(context, networkAddressProvider)
        assertThat(service.isRunning()).isFalse()
    }

    @Test
    fun `when uninitialized serverName returns null`() {
        val service = DefaultNeutrinoService(context, networkAddressProvider)
        assertThat(service.serverName()).isNull()
    }

    @Test
    fun `when uninitialized lastError returns null`() {
        val service = DefaultNeutrinoService(context, networkAddressProvider)
        assertThat(service.lastError()).isNull()
    }

    @Test
    fun `when uninitialized discoveredPeers returns empty list`() {
        val service = DefaultNeutrinoService(context, networkAddressProvider)
        assertThat(service.discoveredPeers()).isEmpty()
    }

    @Test
    fun `when uninitialized isCapturing returns false`() {
        val service = DefaultNeutrinoService(context, networkAddressProvider)
        assertThat(service.isCapturing()).isFalse()
    }

    @Test
    fun `when uninitialized startCapture returns Failed`() {
        val service = DefaultNeutrinoService(context, networkAddressProvider)
        val result = service.startCapture()
        assertThat(result).isInstanceOf(CaptureResult.Failed::class.java)
        assertThat((result as CaptureResult.Failed).reason).isEqualTo("Neutrino is not running")
    }

    @Test
    fun `when uninitialized stopCapture returns null`() {
        val service = DefaultNeutrinoService(context, networkAddressProvider)
        assertThat(service.stopCapture()).isNull()
    }

    @Test
    fun `discoveredPeers converts native peers correctly`() {
        val service = DefaultNeutrinoService(context, networkAddressProvider)
        // Set handle directly for testing mapping without native startup
        val fakeHandle = mockk<io.element.neutrino.NeutrinoHandle>()
        val nativePeer = NativeDiscoveredPeer(
            serverName = "peer.local",
            displayName = "Peer Node",
            lastSeenMs = 123456789UL,
        )
        io.mockk.every { fakeHandle.discoveredPeers() } returns listOf(nativePeer)
        service.handle = fakeHandle

        val peers = service.discoveredPeers()

        assertThat(peers).hasSize(1)
        assertThat(peers[0].serverName).isEqualTo("peer.local")
        assertThat(peers[0].displayName).isEqualTo("Peer Node")
        assertThat(peers[0].lastSeenMs).isEqualTo(123456789L)
    }

    @Test
    fun `serverName and lastError delegate to handle when present`() {
        val service = DefaultNeutrinoService(context, networkAddressProvider)
        val fakeHandle = mockk<io.element.neutrino.NeutrinoHandle>()
        io.mockk.every { fakeHandle.serverName() } returns "node.local"
        io.mockk.every { fakeHandle.lastError() } returns "connection lost"
        service.handle = fakeHandle

        assertThat(service.serverName()).isEqualTo("node.local")
        assertThat(service.lastError()).isEqualTo("connection lost")
    }
}
