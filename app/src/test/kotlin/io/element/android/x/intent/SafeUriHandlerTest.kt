/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.intent

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SafeUriHandlerTest {

    @Test
    fun `openUri opens intent with view action`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val safeUriHandler = SafeUriHandler(activity)

        safeUriHandler.openUri("https://matrix.to/#/#test:example.org")

        val nextStartedActivity = shadowOf(activity).nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.data.toString()).isEqualTo("https://matrix.to/#/#test:example.org")
    }
}
