/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.services.neutrino.impl

import android.net.ConnectivityManager
import android.net.Network
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.capture
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class ConnectivityKickerTest {
    private val connectivityManager = mockk<ConnectivityManager>()
    private val network = mockk<Network>()
    private val callback = slot<ConnectivityManager.NetworkCallback>()

    @Before
    fun setUp() {
        every { connectivityManager.registerDefaultNetworkCallback(capture(callback)) } just Runs
        every { connectivityManager.unregisterNetworkCallback(any()) } just Runs
    }

    @Test
    fun `register is idempotent and reconnect fires only after a loss`() {
        var reconnects = 0
        val kicker = ConnectivityKicker(connectivityManager) { reconnects++ }

        kicker.register()
        kicker.register()
        callback.captured.onAvailable(network)
        callback.captured.onLost(network)
        callback.captured.onAvailable(network)
        callback.captured.onAvailable(network)

        verify(exactly = 1) { connectivityManager.registerDefaultNetworkCallback(any()) }
        assertThat(reconnects).isEqualTo(1)
    }

    @Test
    fun `unregister is safe before registration and idempotent afterward`() {
        val kicker = ConnectivityKicker(connectivityManager) {}

        kicker.unregister()
        kicker.register()
        kicker.unregister()
        kicker.unregister()

        verify(exactly = 1) { connectivityManager.registerDefaultNetworkCallback(any()) }
        verify(exactly = 1) { connectivityManager.unregisterNetworkCallback(any()) }
    }
}
