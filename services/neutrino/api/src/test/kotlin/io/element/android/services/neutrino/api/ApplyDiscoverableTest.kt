/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.services.neutrino.api

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Test

/**
 * The apply-then-persist rule [NeutrinoService.setDiscoverable] documents:
 * nothing is persisted unless the node's advertising state actually matches
 * what was asked for. Verified here, where the contract lives, rather than
 * re-derived by each calling presenter.
 */
class ApplyDiscoverableTest {
    @Test
    fun `Applied persists the requested state`() = runTest {
        val service = StubNeutrinoService { DiscoverableResult.Applied }
        val persisted = mutableListOf<Boolean>()

        service.applyDiscoverable(false) { persisted += it }

        assertThat(service.calls).containsExactly(false)
        assertThat(persisted).containsExactly(false)
    }

    @Test
    fun `Failed throws and persists nothing`() = runTest {
        val service = StubNeutrinoService { DiscoverableResult.Failed("node is not running") }
        val persisted = mutableListOf<Boolean>()

        try {
            service.applyDiscoverable(false) { persisted += it }
            fail("Expected DiscoverabilityNotAppliedException, but nothing was thrown")
        } catch (e: DiscoverabilityNotAppliedException) {
            assertThat(e.result).isEqualTo(DiscoverableResult.Failed("node is not running"))
        }

        assertThat(persisted).isEmpty()
    }

    @Test
    fun `Unavailable persists a request to be discoverable`() = runTest {
        val service = StubNeutrinoService { DiscoverableResult.Unavailable }
        val persisted = mutableListOf<Boolean>()

        service.applyDiscoverable(true) { persisted += it }

        assertThat(persisted).containsExactly(true)
    }

    @Test
    fun `Unavailable throws on a request to hide, because the node keeps advertising`() = runTest {
        val service = StubNeutrinoService { DiscoverableResult.Unavailable }
        val persisted = mutableListOf<Boolean>()

        try {
            service.applyDiscoverable(false) { persisted += it }
            fail("Expected DiscoverabilityNotAppliedException, but nothing was thrown")
        } catch (e: DiscoverabilityNotAppliedException) {
            assertThat(e.result).isEqualTo(DiscoverableResult.Unavailable)
        }

        assertThat(persisted).isEmpty()
    }

    @Test
    fun `a throwing persist is not swallowed`() = runTest {
        val service = StubNeutrinoService { DiscoverableResult.Applied }

        try {
            service.applyDiscoverable(true) { error("store is gone") }
            fail("Expected IllegalStateException, but nothing was thrown")
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessageThat().isEqualTo("store is gone")
        }
    }
}

/**
 * Only [setDiscoverable] is exercised here; every other member is unreachable
 * from [applyDiscoverable]. This module has no test fake of its own
 * (`services/neutrino/test` depends on it, not the other way round).
 */
private class StubNeutrinoService(
    private val result: (Boolean) -> DiscoverableResult,
) : NeutrinoService {
    val calls = mutableListOf<Boolean>()

    override suspend fun setDiscoverable(discoverable: Boolean): DiscoverableResult {
        calls += discoverable
        return result(discoverable)
    }

    override fun start(discoverable: Boolean) = unused()
    override suspend fun awaitReady(timeoutMs: Long) = unused()
    override fun isRunning(): Boolean = unused()
    override fun serverName(): String? = unused()
    override fun lastError(): String? = unused()
    override fun discoveredPeers(): List<DiscoveredPeer> = unused()
    override fun startCapture(): CaptureResult = unused()
    override fun stopCapture(): String? = unused()
    override fun isCapturing(): Boolean = unused()
    override fun isDiscoverabilityControlAvailable(): Boolean = unused()

    private fun unused(): Nothing = error("not used by applyDiscoverable")
}
