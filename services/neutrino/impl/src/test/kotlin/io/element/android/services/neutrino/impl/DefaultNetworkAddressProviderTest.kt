/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.services.neutrino.impl

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Collections

class DefaultNetworkAddressProviderTest {
    @Before
    fun setUp() {
        mockkStatic(NetworkInterface::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(NetworkInterface::class)
    }

    @Test
    fun `currentAddresses returns addresses from up non-loopback interfaces`() {
        val address = InetAddress.getByName("192.168.1.5")
        val interfaceUnderTest = mockk<NetworkInterface>()
        every { interfaceUnderTest.isUp } returns true
        every { interfaceUnderTest.isLoopback } returns false
        every { interfaceUnderTest.inetAddresses } returns Collections.enumeration(listOf(address))
        every { NetworkInterface.getNetworkInterfaces() } returns Collections.enumeration(listOf(interfaceUnderTest))

        val result = DefaultNetworkAddressProvider().currentAddresses()

        assertThat(result).containsExactly(address)
    }

    @Test
    fun `currentAddresses skips down and loopback interfaces`() {
        val address = InetAddress.getByName("192.168.1.5")
        val downInterface = mockk<NetworkInterface>()
        every { downInterface.isUp } returns false
        every { downInterface.isLoopback } returns false
        every { downInterface.inetAddresses } returns Collections.enumeration(listOf(address))
        val loopbackInterface = mockk<NetworkInterface>()
        every { loopbackInterface.isUp } returns true
        every { loopbackInterface.isLoopback } returns true
        every { loopbackInterface.inetAddresses } returns Collections.enumeration(listOf(address))
        every {
            NetworkInterface.getNetworkInterfaces()
        } returns Collections.enumeration(listOf(downInterface, loopbackInterface))

        val result = DefaultNetworkAddressProvider().currentAddresses()

        assertThat(result).isEmpty()
    }

    @Test
    fun `currentAddresses returns empty list when interface query fails`() {
        every { NetworkInterface.getNetworkInterfaces() } throws SocketException("interfaces unavailable")

        val result = DefaultNetworkAddressProvider().currentAddresses()

        assertThat(result).isEmpty()
    }
}
