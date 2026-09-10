/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.conference

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import com.google.common.truth.Truth.assertThat
import io.element.android.x.conference.ConferenceLinkDispatch.CompanionRoute
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

// Unit tests need Android resources, not the production app and native SDK startup.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ConferenceLinkDispatcherTest {
    private val loadedUrls = mutableListOf<String>()

    private fun dispatcher(activity: Activity, companionInstalled: Boolean = false) = ConferenceLinkDispatcher(
        activity = activity,
        loadInWebView = { loadedUrls += it },
        isCompanionInstalled = { companionInstalled },
    )

    private fun activity(): Activity = Robolectric.buildActivity(Activity::class.java).setup().get()

    private fun noHandlerActivity(): Activity = Robolectric.buildActivity(NoHandlerActivity::class.java).setup().get()

    @Test
    fun `internal navigation is left to the WebView`() {
        val activity = activity()
        assertThat(dispatcher(activity).dispatch(ConferenceLinkDispatch.InternalConference)).isFalse()
        assertThat(shadowOf(activity).nextStartedActivity).isNull()
        assertThat(loadedUrls).isEmpty()
    }

    @Test
    fun `matrix links are sent to this app only`() {
        val activity = activity()
        val uri = "matrix:r/indiafoss:matrix.org?action=join".toUri()
        assertThat(dispatcher(activity).dispatch(ConferenceLinkDispatch.MatrixLink(uri))).isTrue()
        val started = shadowOf(activity).nextStartedActivity
        assertThat(started.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(started.data).isEqualTo(uri)
        assertThat(started.`package`).isEqualTo(activity.packageName)
    }

    @Test
    fun `handoff cards are sent to this app only`() {
        val activity = activity()
        val uri = "indiafoss://chat?dm=%40alice%3Amatrix.org".toUri()
        assertThat(dispatcher(activity).dispatch(ConferenceLinkDispatch.IndiafossHandoff(uri))).isTrue()
        val started = shadowOf(activity).nextStartedActivity
        assertThat(started.data).isEqualTo(uri)
        assertThat(started.`package`).isEqualTo(activity.packageName)
    }

    @Test
    fun `external web and communication links go to any matching app`() {
        val activity = activity()
        val web = "https://fossunited.org/".toUri()
        val mail = "mailto:hello@fossunited.org".toUri()
        assertThat(dispatcher(activity).dispatch(ConferenceLinkDispatch.ExternalWeb(web))).isTrue()
        val browser = shadowOf(activity).nextStartedActivity
        assertThat(browser.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(browser.data).isEqualTo(web)
        assertThat(browser.`package`).isNull()
        assertThat(dispatcher(activity).dispatch(ConferenceLinkDispatch.Communication(mail))).isTrue()
        assertThat(shadowOf(activity).nextStartedActivity.data).isEqualTo(mail)
    }

    @Test
    fun `rejected links open nothing and block the WebView`() {
        val activity = activity()
        assertThat(dispatcher(activity).dispatch(ConferenceLinkDispatch.Rejected("unsupported scheme"))).isTrue()
        assertThat(shadowOf(activity).nextStartedActivity).isNull()
        assertThat(loadedUrls).isEmpty()
    }

    @Test
    fun `companion route opens the native companion when installed`() {
        val activity = activity()
        val route = CompanionRoute("indiafoss://activity/session-1".toUri(), "https://hanthor.github.io/indiafoss-companion/activity/session-1")
        assertThat(dispatcher(activity, companionInstalled = true).dispatch(route)).isTrue()
        val started = shadowOf(activity).nextStartedActivity
        assertThat(started.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(started.data).isEqualTo(route.companionUri)
        assertThat(started.`package`).isEqualTo(ConferenceLinks.COMPANION_PACKAGE)
        assertThat(loadedUrls).isEmpty()
    }

    @Test
    fun `companion route falls back to the PWA when the companion is missing or cannot show it`() {
        val activity = activity()
        val route = CompanionRoute("indiafoss://activity/session-1".toUri(), "https://hanthor.github.io/indiafoss-companion/activity/session-1")
        assertThat(dispatcher(activity, companionInstalled = false).dispatch(route)).isTrue()
        assertThat(shadowOf(activity).nextStartedActivity).isNull()
        assertThat(loadedUrls).containsExactly(route.pwaUrl)

        val pwaOnly = CompanionRoute(null, "https://hanthor.github.io/indiafoss-companion/booth/b1")
        assertThat(dispatcher(activity, companionInstalled = true).openInCompanion(pwaOnly)).isFalse()
        assertThat(dispatcher(activity, companionInstalled = true).dispatch(pwaOnly)).isTrue()
        assertThat(shadowOf(activity).nextStartedActivity).isNull()
        assertThat(loadedUrls).containsExactly(route.pwaUrl, pwaOnly.pwaUrl)
    }

    @Test
    fun `companion that refuses the intent falls back to the PWA without a toast`() {
        val activity = noHandlerActivity()
        val route = CompanionRoute("indiafoss://activity/session-1".toUri(), "https://hanthor.github.io/indiafoss-companion/activity/session-1")
        assertThat(dispatcher(activity, companionInstalled = true).openInCompanion(route)).isFalse()
        assertThat(dispatcher(activity, companionInstalled = true).dispatch(route)).isTrue()
        assertThat(loadedUrls).containsExactly(route.pwaUrl)
        assertThat(ShadowToast.getLatestToast()).isNull()
    }

    @Test
    fun `missing browser mail client or messenger shows an explanation without crashing`() {
        val activity = noHandlerActivity()
        val expected = activity.getString(io.element.android.libraries.androidutils.R.string.error_no_compatible_app_found)
        val dispatcher = dispatcher(activity)

        assertThat(dispatcher.dispatch(ConferenceLinkDispatch.ExternalWeb("https://fossunited.org/".toUri()))).isTrue()
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(expected)
        ShadowToast.reset()

        assertThat(dispatcher.dispatch(ConferenceLinkDispatch.Communication("mailto:hello@fossunited.org".toUri()))).isTrue()
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(expected)
        ShadowToast.reset()

        assertThat(dispatcher.dispatch(ConferenceLinkDispatch.MatrixLink("matrix:u/alice:matrix.org?action=chat".toUri()))).isTrue()
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(expected)
        ShadowToast.reset()

        assertThat(dispatcher.dispatch(ConferenceLinkDispatch.IndiafossHandoff("indiafoss://chat?dm=%40alice%3Amatrix.org".toUri()))).isTrue()
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(expected)
    }

    class NoHandlerActivity : Activity() {
        override fun startActivity(intent: Intent) {
            throw ActivityNotFoundException()
        }
    }
}
