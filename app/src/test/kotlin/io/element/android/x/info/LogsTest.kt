/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.info

import android.app.Application
import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.androidutils.system.getVersionCodeFromManifest
import io.element.android.x.BuildConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import timber.log.Timber

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LogsTest {
    private lateinit var mockContext: Context
    
    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        // Mock context to return a version code
        every { mockContext.getVersionCodeFromManifest() } returns 12345
    }

    @Test
    fun `logApplicationInfo formats version string correctly`() {
        // Capture the logged messages to verify content
        val debugLogs = mutableListOf<String>()
        val originalTree = Timber.asTree().firstOrNull() as? timber.log.Timber.DebugTree
        
        Timber.uprootAll()
        Timber.plant(object : timber.log.Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                debugLogs.add(message ?: "")
            }
        })

        // Call the function under test
        logApplicationInfo(mockContext)

        // Verify the version string is formatted correctly
        val versionLog = debugLogs.find { it.contains("Application version:") }
        assertThat(versionLog).isNotNull()
        assertThat(versionLog).contains(BuildConfig.VERSION_NAME)
        assertThat(versionLog).contains("12345") // Version code from mock
        assertThat(versionLog).contains(BuildConfig.BUILD_TYPE)
        assertThat(versionLog).contains(BuildConfig.FLAVOR)

        // Verify Git SHA is logged
        val gitLog = debugLogs.find { it.contains("Git SHA:") }
        assertThat(gitLog).isNotNull()
        assertThat(gitLog).contains(BuildConfig.GIT_REVISION)

        // Verify SDK version placeholder is logged
        val sdkLog = debugLogs.find { it.contains("SDK version:") }
        assertThat(sdkLog).isNotNull()
        assertThat(sdkLog).contains("TODO")

        // Verify local time is logged
        val timeLog = debugLogs.find { it.contains("Local time:") }
        assertThat(timeLog).isNotNull()
        assertThat(timeLog).matches(Regex(".*\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*"))

        // Verify decorative separator lines
        val separators = debugLogs.filter { it.contains("----------------------------------------------------------------") }
        assertThat(separators).hasSize(4) // Two pairs of separators (opening and closing)

        // Cleanup
        Timber.uprootAll()
        if (originalTree != null) {
            Timber.plant(originalTree)
        }
    }

    @Test
    fun `logApplicationInfo uses context version code`() {
        val debugLogs = mutableListOf<String>()
        
        Timber.uprootAll()
        Timber.plant(object : timber.log.Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                debugLogs.add(message ?: "")
            }
        })

        logApplicationInfo(mockContext)

        // Verify context.getVersionCodeFromManifest() was called
        verify { mockContext.getVersionCodeFromManifest() }
        
        // Verify the returned version code is in the logged string
        val versionLog = debugLogs.find { it.contains("Application version:") }
        assertThat(versionLog).contains("12345")

        Timber.uprootAll()
    }

    @Test
    fun `logApplicationInfo handles timestamp formatting`() {
        val debugLogs = mutableListOf<String>()
        
        Timber.uprootAll()
        Timber.plant(object : timber.log.Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                debugLogs.add(message ?: "")
            }
        })

        logApplicationInfo(mockContext)

        // Verify timestamp format: MM-dd HH:mm:ss.SSSZ
        val timeLog = debugLogs.find { it.contains("Local time:") }
        assertThat(timeLog).isNotNull()
        // Example format: "01-15 14:30:45.123+0530"
        assertThat(timeLog).matches(Regex(".*\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4}.*"))

        Timber.uprootAll()
    }

    @Test
    fun `logApplicationInfo builds complete version string with all components`() {
        val debugLogs = mutableListOf<String>()
        
        Timber.uprootAll()
        Timber.plant(object : timber.log.Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                debugLogs.add(message ?: "")
            }
        })

        logApplicationInfo(mockContext)

        val versionLog = debugLogs.find { it.contains("Application version:") }
        assertThat(versionLog).isNotNull()
        // Verify format: "VERSION_NAME (versionCode) - BUILD_TYPE / FLAVOR"
        assertThat(versionLog).matches(Regex(".*\\(\\d+\\).*-.*/.*/.*"))

        Timber.uprootAll()
    }
}
