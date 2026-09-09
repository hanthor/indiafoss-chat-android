/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.initializer

import android.app.Application
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Unit tests need Android resources, not the production app and native SDK startup.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CacheCleanerInitializerTest {
    @Test
    fun `dependencies returns empty list`() {
        val initializer = CacheCleanerInitializer()
        assertThat(initializer.dependencies()).isEmpty()
    }
}
