/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.intent

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SafeUriHandlerTest {
    @Test
    fun `openUri opens intent with view action`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val safeUriHandler = SafeUriHandler(activity)

        safeUriHandler.openUri("https://matrix.to/#/#test:example.org")

        val nextStartedActivity = shadowOf(activity).nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(nextStartedActivity.data.toString()).isEqualTo("https://matrix.to/#/#test:example.org")
    }

    @Test
    fun `missing browser shows an explanation without crashing`() {
        val activity = Robolectric.buildActivity(NoBrowserActivity::class.java).setup().get()
        SafeUriHandler(activity).openUri("https://matrix.to/#/#test:example.org")
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
            activity.getString(io.element.android.libraries.androidutils.R.string.error_no_compatible_app_found)
        )
    }

    class NoBrowserActivity : Activity() {
        override fun startActivity(intent: Intent) {
            throw ActivityNotFoundException()
        }
    }
}
