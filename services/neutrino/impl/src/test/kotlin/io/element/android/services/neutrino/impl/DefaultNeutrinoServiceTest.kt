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
import io.element.android.services.neutrino.api.DiscoverableResult
import io.element.android.services.neutrino.api.NetworkAddressProvider
import io.element.neutrino.NeutrinoHandle
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import io.element.neutrino.DiscoveredPeer as NativeDiscoveredPeer

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
        val fakeHandle = mockk<NeutrinoHandle>()
        val nativePeer = NativeDiscoveredPeer(
            serverName = "peer.local",
            displayName = "Peer Node",
            lastSeenMs = 123456789UL,
        )
        every { fakeHandle.discoveredPeers() } returns listOf(nativePeer)
        service.handle = fakeHandle

        val peers = service.discoveredPeers()

        assertThat(peers).hasSize(1)
        assertThat(peers[0].serverName).isEqualTo("peer.local")
        assertThat(peers[0].displayName).isEqualTo("Peer Node")
        assertThat(peers[0].lastSeenMs).isEqualTo(123_456_789L)
    }

    @Test
    fun `serverName and lastError delegate to handle when present`() {
        val service = DefaultNeutrinoService(context, networkAddressProvider)
        val fakeHandle = mockk<NeutrinoHandle>()
        every { fakeHandle.serverName() } returns "node.local"
        every { fakeHandle.lastError() } returns "connection lost"
        service.handle = fakeHandle

        assertThat(service.serverName()).isEqualTo("node.local")
        assertThat(service.lastError()).isEqualTo("connection lost")
    }

    @Test
    fun `isDiscoverabilityControlAvailable reflects the binding`() {
        val service = DefaultNeutrinoService(context, networkAddressProvider)
        service.discoverableBinding = FakeDiscoverableBinding(isAvailable = false)
        assertThat(service.isDiscoverabilityControlAvailable()).isFalse()
        service.discoverableBinding = FakeDiscoverableBinding(isAvailable = true)
        assertThat(service.isDiscoverabilityControlAvailable()).isTrue()
    }

    @Test
    fun `setDiscoverable returns Unavailable and does not call the binding when the build lacks it`() = runTest {
        val service = DefaultNeutrinoService(context, networkAddressProvider)
        val binding = FakeDiscoverableBinding(isAvailable = false)
        service.discoverableBinding = binding
        service.handle = mockk<NeutrinoHandle>()

        assertThat(service.setDiscoverable(false)).isEqualTo(DiscoverableResult.Unavailable)
        assertThat(binding.calls).isEmpty()
    }

    @Test
    fun `setDiscoverable returns Failed when the node is not running`() = runTest {
        val service = DefaultNeutrinoService(context, networkAddressProvider)
        val binding = FakeDiscoverableBinding(isAvailable = true)
        service.discoverableBinding = binding

        val result = service.setDiscoverable(false)

        assertThat(result).isEqualTo(DiscoverableResult.Failed("Neutrino is not running"))
        assertThat(binding.calls).isEmpty()
    }

    @Test
    fun `setDiscoverable calls the native binding and returns Applied`() = runTest {
        val service = DefaultNeutrinoService(context, networkAddressProvider)
        val binding = FakeDiscoverableBinding(isAvailable = true)
        service.discoverableBinding = binding
        service.handle = mockk<NeutrinoHandle>()

        assertThat(service.setDiscoverable(false)).isEqualTo(DiscoverableResult.Applied)
        assertThat(service.setDiscoverable(true)).isEqualTo(DiscoverableResult.Applied)
        assertThat(binding.calls).containsExactly(false, true).inOrder()
    }

    @Test
    fun `setDiscoverable surfaces a throwing binding as Failed`() = runTest {
        val service = DefaultNeutrinoService(context, networkAddressProvider)
        service.discoverableBinding = FakeDiscoverableBinding(isAvailable = true, failure = IllegalStateException("adapter off"))
        service.handle = mockk<NeutrinoHandle>()

        assertThat(service.setDiscoverable(false)).isEqualTo(DiscoverableResult.Failed("adapter off"))
    }

    @Test
    fun `reflective binding is unavailable when the facade class is missing`() {
        val binding = ReflectiveDiscoverableBinding(className = "io.element.neutrino.ble.DoesNotExistKt")
        assertThat(binding.isAvailable).isFalse()
    }

    @Test
    fun `reflective binding resolves and invokes a static setDiscoverable(boolean)`() {
        val binding = ReflectiveDiscoverableBinding(className = SetDiscoverableFixture::class.java.name)
        assertThat(binding.isAvailable).isTrue()

        binding.setDiscoverable(false)

        assertThat(SetDiscoverableFixture.calls).containsExactly(false)
    }

    @Test
    fun `reflective binding rethrows what the entry point throws`() {
        val binding = ReflectiveDiscoverableBinding(className = ThrowingSetDiscoverableFixture::class.java.name)
        assertThat(binding.isAvailable).isTrue()

        val thrown = try {
            binding.setDiscoverable(true)
            null
        } catch (e: IllegalStateException) {
            e
        }

        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
        assertThat(thrown).hasMessageThat().isEqualTo("ffi boom")
    }
}

private class FakeDiscoverableBinding(
    override val isAvailable: Boolean,
    private val failure: Throwable? = null,
) : DiscoverableBinding {
    val calls = mutableListOf<Boolean>()

    override fun setDiscoverable(discoverable: Boolean) {
        failure?.let { throw it }
        calls += discoverable
    }
}

/** Stands in for the uniffi-generated `Neutrino_bleKt.setDiscoverable(boolean)` facade. */
object SetDiscoverableFixture {
    val calls = mutableListOf<Boolean>()

    @JvmStatic
    fun setDiscoverable(discoverable: Boolean) {
        calls += discoverable
    }
}

object ThrowingSetDiscoverableFixture {
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun setDiscoverable(discoverable: Boolean) {
        error("ffi boom")
    }
}
