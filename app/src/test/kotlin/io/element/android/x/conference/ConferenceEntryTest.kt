/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.conference

import android.app.Application
import androidx.core.net.toUri
import com.google.common.truth.Truth.assertThat
import io.element.android.x.conference.ConferenceLinkDispatch.CompanionRoute
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Unit tests need Android's Uri, not the production app and native SDK startup.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ConferenceEntryTest {
    private val nativeRoute = CompanionRoute(
        companionUri = "indiafoss://activity/keynote".toUri(),
        pwaUrl = ConferenceLinks.COMPANION_URL + "activity/keynote",
    )
    private val pwaOnlyRoute = CompanionRoute(
        companionUri = null,
        pwaUrl = ConferenceLinks.COMPANION_URL + "explore/booths",
    )

    @Test
    fun `installed companion takes a route it answers to`() {
        assertThat(ConferenceEntry.companionAction(nativeRoute, isCompanionInstalled = true))
            .isEqualTo(CompanionAction.OpenNative(nativeRoute))
    }

    @Test
    fun `installed companion opens at home for a PWA-only route`() {
        assertThat(ConferenceEntry.companionAction(pwaOnlyRoute, isCompanionInstalled = true))
            .isEqualTo(CompanionAction.OpenNative(ConferenceLinks.HOME))
    }

    @Test
    fun `without the companion the same route opens in the browser`() {
        assertThat(ConferenceEntry.companionAction(nativeRoute, isCompanionInstalled = false))
            .isEqualTo(CompanionAction.OpenInBrowser(nativeRoute.pwaUrl))
        assertThat(ConferenceEntry.companionAction(pwaOnlyRoute, isCompanionInstalled = false))
            .isEqualTo(CompanionAction.OpenInBrowser(pwaOnlyRoute.pwaUrl))
    }

    @Test
    fun `the home route is the companion home on both targets`() {
        assertThat(ConferenceEntry.companionAction(ConferenceLinks.HOME, isCompanionInstalled = true))
            .isEqualTo(CompanionAction.OpenNative(ConferenceLinks.HOME))
        assertThat(ConferenceEntry.companionAction(ConferenceLinks.HOME, isCompanionInstalled = false))
            .isEqualTo(CompanionAction.OpenInBrowser(ConferenceLinks.COMPANION_URL))
    }
}
