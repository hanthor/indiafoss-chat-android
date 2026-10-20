/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.di

import android.app.Application
import android.content.Context
import androidx.preference.PreferenceManager
import com.google.common.truth.Truth.assertThat
import io.element.android.features.enterprise.api.EnterpriseService
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.meta.BuildType
import io.element.android.libraries.recentemojis.impl.DefaultEmojibaseProvider
import io.element.android.x.BuildConfig
import io.element.android.x.R
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.cancel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

// Unit tests need Android resources, not the production app and native SDK startup.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppModuleTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun `provides base directory below application files directory`() {
        assertThat(AppModule.providesBaseDirectory(context)).isEqualTo(File(context.filesDir, "sessions"))
    }

    @Test
    fun `provides cache directory from application context`() {
        assertThat(AppModule.providesCacheDirectory(context)).isSameInstanceAs(context.cacheDir)
    }

    @Test
    fun `provides resources from application context`() {
        assertThat(AppModule.providesResources(context)).isSameInstanceAs(context.resources)
    }

    @Test
    fun `provides debug build type for debug unit tests`() {
        assertThat(AppModule.providesBuildType()).isEqualTo(BuildType.DEBUG)
    }

    @Test
    fun `provides shared preferences from application context`() {
        assertThat(AppModule.providesSharedPreferences(context))
            .isSameInstanceAs(PreferenceManager.getDefaultSharedPreferences(context))
    }

    @Test
    fun `provides default coroutine dispatchers`() {
        assertThat(AppModule.providesCoroutineDispatchers()).isEqualTo(CoroutineDispatchers.Default)
    }

    @Test
    fun `provides application coroutine scope with stable name`() {
        val scope = AppModule.providesAppCoroutineScope()

        try {
            assertThat(scope.coroutineContext[CoroutineName]?.name).isEqualTo("ElementX Scope")
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `provides emojibase provider with application context`() {
        val provider = AppModule.providesEmojibaseProvider(context)

        assertThat(provider).isInstanceOf(DefaultEmojibaseProvider::class.java)
        assertThat((provider as DefaultEmojibaseProvider).context).isSameInstanceAs(context)
    }

    @Test
    fun `provides OSS build metadata`() {
        val metadata = AppModule.providesBuildMeta(
            context = context,
            buildType = BuildType.DEBUG,
            enterpriseService = enterpriseService(isEnterpriseBuild = false),
        )

        assertThat(metadata.buildType).isEqualTo(BuildType.DEBUG)
        assertThat(metadata.isDebuggable).isEqualTo(BuildConfig.DEBUG)
        assertThat(metadata.applicationName).isEqualTo(context.getString(R.string.app_name))
        assertThat(metadata.productionApplicationName).isEqualTo("IndiaFOSS Chat")
        assertThat(metadata.desktopApplicationName).isEqualTo("Element")
        assertThat(metadata.applicationId).isEqualTo(BuildConfig.APPLICATION_ID)
        assertThat(metadata.isEnterpriseBuild).isFalse()
        assertThat(metadata.lowPrivacyLoggingEnabled).isFalse()
        assertThat(metadata.versionName).isEqualTo(BuildConfig.VERSION_NAME)
        assertThat(metadata.versionCode).isEqualTo(
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode,
        )
        assertThat(metadata.gitRevision).isEqualTo(BuildConfig.GIT_REVISION)
        assertThat(metadata.gitBranchName).isEqualTo(BuildConfig.GIT_BRANCH_NAME)
        assertThat(metadata.flavorDescription).isEqualTo(BuildConfig.FLAVOR_DESCRIPTION)
        assertThat(metadata.flavorShortDescription).isEqualTo(BuildConfig.SHORT_FLAVOR_DESCRIPTION)
    }

    @Test
    fun `provides enterprise build metadata using application name for branded names`() {
        val metadata = AppModule.providesBuildMeta(
            context = context,
            buildType = BuildType.RELEASE,
            enterpriseService = enterpriseService(isEnterpriseBuild = true),
        )

        assertThat(metadata.buildType).isEqualTo(BuildType.RELEASE)
        assertThat(metadata.applicationName).isEqualTo(context.getString(R.string.app_name))
        assertThat(metadata.productionApplicationName).isEqualTo(metadata.applicationName)
        assertThat(metadata.desktopApplicationName).isEqualTo(metadata.applicationName)
        assertThat(metadata.isEnterpriseBuild).isTrue()
    }

    private fun enterpriseService(isEnterpriseBuild: Boolean): EnterpriseService {
        return mockk<EnterpriseService> {
            every { this@mockk.isEnterpriseBuild } returns isEnterpriseBuild
        }
    }
}
