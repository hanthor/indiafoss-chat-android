/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.advanced

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.Theme
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.featureflag.api.FeatureFlags
import io.element.android.libraries.featureflag.test.FakeFeatureFlagService
import io.element.android.libraries.matrix.api.media.MediaPreviewValue
import io.element.android.libraries.preferences.api.store.AppPreferencesStore
import io.element.android.libraries.preferences.api.store.VideoCompressionPreset
import io.element.android.libraries.preferences.test.InMemoryAppPreferencesStore
import io.element.android.libraries.preferences.test.InMemorySessionPreferencesStore
import io.element.android.services.neutrino.api.DiscoverabilityNotAppliedException
import io.element.android.services.neutrino.api.DiscoverableResult
import io.element.android.services.neutrino.api.NeutrinoService
import io.element.android.services.neutrino.test.FakeNeutrinoService
import io.element.android.tests.testutils.WarmUpRule
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AdvancedSettingsPresenterTest {
    @get:Rule
    val warmUpRule = WarmUpRule()

    @Test
    fun `present - initial state`() = runTest {
        val presenter = createAdvancedSettingsPresenter()
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            with(awaitItem()) {
                assertThat(isDeveloperModeEnabled).isFalse()
                assertThat(isSharePresenceEnabled).isTrue()
                assertThat(mediaOptimizationState).isNull()
                assertThat(theme).isEqualTo(ThemeOption.System)
                assertThat(availableThemeOptions).isEqualTo(
                    listOf(ThemeOption.System, ThemeOption.Light, ThemeOption.Dark).toImmutableList()
                )
                assertThat(mediaPreviewConfigState.hideInviteAvatars).isFalse()
                assertThat(mediaPreviewConfigState.timelineMediaPreviewValue).isEqualTo(MediaPreviewValue.On)
                assertThat(mediaPreviewConfigState.setHideInviteAvatarsAction).isEqualTo(AsyncAction.Uninitialized)
                assertThat(mediaPreviewConfigState.setTimelineMediaPreviewAction).isEqualTo(AsyncAction.Uninitialized)
            }

            // After the initial state, we expect the media optimization state to be set
            with(awaitItem()) {
                assertThat(mediaOptimizationState).isInstanceOf(MediaOptimizationState.AllMedia::class.java)
                assertThat((mediaOptimizationState as MediaOptimizationState.AllMedia).isEnabled).isTrue()
            }
        }
    }

    @Test
    fun `present - developer mode on off`() = runTest {
        val presenter = createAdvancedSettingsPresenter()
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            // Skip until the initial data it loaded
            skipItems(1)

            with(awaitItem()) {
                assertThat(isDeveloperModeEnabled).isFalse()
                eventSink(AdvancedSettingsEvents.SetDeveloperModeEnabled(true))
            }
            with(awaitItem()) {
                assertThat(isDeveloperModeEnabled).isTrue()
                eventSink(AdvancedSettingsEvents.SetDeveloperModeEnabled(false))
            }
            with(awaitItem()) {
                assertThat(isDeveloperModeEnabled).isFalse()
            }
        }
    }

    @Test
    fun `present - share presence off on`() = runTest {
        val presenter = createAdvancedSettingsPresenter()
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            // Skip until the initial data it loaded
            skipItems(1)

            with(awaitItem()) {
                assertThat(isSharePresenceEnabled).isTrue()
                eventSink(AdvancedSettingsEvents.SetSharePresenceEnabled(false))
            }
            with(awaitItem()) {
                assertThat(isSharePresenceEnabled).isFalse()
                eventSink(AdvancedSettingsEvents.SetSharePresenceEnabled(true))
            }
            with(awaitItem()) {
                assertThat(isSharePresenceEnabled).isTrue()
            }
        }
    }

    @Test
    fun `present - compress media off on`() = runTest {
        val presenter = createAdvancedSettingsPresenter()
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            // Skip until the initial data it loaded
            skipItems(1)

            with(awaitItem()) {
                assertThat((mediaOptimizationState as MediaOptimizationState.AllMedia).isEnabled).isTrue()
                eventSink(AdvancedSettingsEvents.SetCompressMedia(false))
            }
            with(awaitItem()) {
                assertThat((mediaOptimizationState as MediaOptimizationState.AllMedia).isEnabled).isFalse()
                eventSink(AdvancedSettingsEvents.SetCompressMedia(true))
            }
            with(awaitItem()) {
                assertThat((mediaOptimizationState as MediaOptimizationState.AllMedia).isEnabled).isTrue()
            }
        }
    }

    @Test
    fun `present - compress images off on`() = runTest {
        val presenter = createAdvancedSettingsPresenter(
            featureFlagService = FakeFeatureFlagService().apply {
                setFeatureEnabled(FeatureFlags.SelectableMediaQuality, true)
            }
        )
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            // Skip until the initial data it loaded
            skipItems(1)

            with(awaitItem()) {
                assertThat((mediaOptimizationState as MediaOptimizationState.Split).compressImages).isTrue()
                eventSink(AdvancedSettingsEvents.SetCompressImages(false))
            }
            with(awaitItem()) {
                assertThat((mediaOptimizationState as MediaOptimizationState.Split).compressImages).isFalse()
                eventSink(AdvancedSettingsEvents.SetCompressImages(true))
            }
            with(awaitItem()) {
                assertThat((mediaOptimizationState as MediaOptimizationState.Split).compressImages).isTrue()
            }
        }
    }

    @Test
    fun `present - video upload quality selector`() = runTest {
        val presenter = createAdvancedSettingsPresenter(
            featureFlagService = FakeFeatureFlagService().apply {
                setFeatureEnabled(FeatureFlags.SelectableMediaQuality, true)
            }
        )
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            // Skip until the initial data it loaded
            skipItems(1)

            with(awaitItem()) {
                assertThat((mediaOptimizationState as MediaOptimizationState.Split).videoPreset).isEqualTo(VideoCompressionPreset.STANDARD)
                eventSink(AdvancedSettingsEvents.SetVideoUploadQuality(VideoCompressionPreset.LOW))
            }
            with(awaitItem()) {
                assertThat((mediaOptimizationState as MediaOptimizationState.Split).videoPreset).isEqualTo(VideoCompressionPreset.LOW)
                eventSink(AdvancedSettingsEvents.SetVideoUploadQuality(VideoCompressionPreset.HIGH))
            }
            with(awaitItem()) {
                assertThat((mediaOptimizationState as MediaOptimizationState.Split).videoPreset).isEqualTo(VideoCompressionPreset.HIGH)
            }
        }
    }

    @Test
    fun `present - dynamic colors default on and toggles`() = runTest {
        val presenter = createAdvancedSettingsPresenter()
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)
            with(awaitItem()) {
                // Default on: a phone that themes every app to its wallpaper
                // should theme this one too.
                assertThat(isDynamicColorsEnabled).isTrue()
                eventSink(AdvancedSettingsEvents.SetDynamicColorsEnabled(false))
            }
            with(awaitItem()) {
                assertThat(isDynamicColorsEnabled).isFalse()
                eventSink(AdvancedSettingsEvents.SetDynamicColorsEnabled(true))
            }
            with(awaitItem()) {
                assertThat(isDynamicColorsEnabled).isTrue()
            }
        }
    }

    @Test
    fun `present - change theme`() = runTest {
        val presenter = createAdvancedSettingsPresenter()
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            // Skip until the initial data it loaded
            skipItems(1)

            with(awaitItem()) {
                assertThat(theme).isEqualTo(ThemeOption.System)
                eventSink(AdvancedSettingsEvents.SetTheme(ThemeOption.Dark))
            }
            with(awaitItem()) {
                assertThat(theme).isEqualTo(ThemeOption.Dark)
                eventSink(AdvancedSettingsEvents.SetTheme(ThemeOption.Light))
            }
            with(awaitItem()) {
                assertThat(theme).isEqualTo(ThemeOption.Light)
                eventSink(AdvancedSettingsEvents.SetTheme(ThemeOption.System))
            }
            with(awaitItem()) {
                assertThat(theme).isEqualTo(ThemeOption.System)
            }
        }
    }

    @Test
    fun `present - exposes live location minimum distance from app preferences`() = runTest {
        val appPreferencesStore = InMemoryAppPreferencesStore(
            liveLocationMinimumDistanceUpdate = 50,
        )
        val presenter = createAdvancedSettingsPresenter(appPreferencesStore = appPreferencesStore)

        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)

            with(awaitItem()) {
                assertThat(liveLocationMinimumDistanceUpdate).isEqualTo(50)
            }
        }
    }

    @Test
    fun `present - saving live location minimum distance updates app preferences`() = runTest {
        val appPreferencesStore = InMemoryAppPreferencesStore(
            liveLocationMinimumDistanceUpdate = 10,
        )
        val presenter = createAdvancedSettingsPresenter(appPreferencesStore = appPreferencesStore)

        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)

            with(awaitItem()) {
                assertThat(liveLocationMinimumDistanceUpdate).isEqualTo(10)
                eventSink(AdvancedSettingsEvents.SetLiveLocationMinimumDistanceUpdate(42))
            }
            with(awaitItem()) {
                assertThat(liveLocationMinimumDistanceUpdate).isEqualTo(42)
            }
        }
    }

    @Test
    fun `present - black theme option shown when feature flag enabled`() = runTest {
        val presenter = createAdvancedSettingsPresenter(
            featureFlagService = FakeFeatureFlagService().apply {
                setFeatureEnabled(FeatureFlags.AllowBlackTheme, true)
            }
        )
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)

            with(awaitItem()) {
                assertThat(availableThemeOptions).contains(ThemeOption.Black)
                assertThat(availableThemeOptions).isEqualTo(ThemeOption.entries.toImmutableList())
            }
        }
    }

    @Test
    fun `present - stored black theme falls back to dark when feature flag disabled`() = runTest {
        val appPreferencesStore = InMemoryAppPreferencesStore().apply {
            setTheme(Theme.Black.name)
        }
        val presenter = createAdvancedSettingsPresenter(appPreferencesStore = appPreferencesStore)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)

            with(awaitItem()) {
                assertThat(theme).isEqualTo(ThemeOption.Dark)
            }
        }
    }

    @Test
    fun `present - hide invite avatars`() = runTest {
        val mediaPreviewStore = FakeMediaPreviewConfigStateStore()
        val presenter = createAdvancedSettingsPresenter(mediaPreviewConfigStateStore = mediaPreviewStore)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            // Skip until the initial data it loaded
            skipItems(1)

            with(awaitItem()) {
                assertThat(mediaPreviewConfigState.hideInviteAvatars).isFalse()
                eventSink(AdvancedSettingsEvents.SetHideInviteAvatars(true))
            }
            with(awaitItem()) {
                assertThat(mediaPreviewConfigState.hideInviteAvatars).isTrue()
                eventSink(AdvancedSettingsEvents.SetHideInviteAvatars(false))
            }
            with(awaitItem()) {
                assertThat(mediaPreviewConfigState.hideInviteAvatars).isFalse()
            }
        }
        assertThat(mediaPreviewStore.getSetHideInviteAvatarsEvents()).isEqualTo(listOf(true, false))
    }

    @Test
    fun `present - timeline media preview value`() = runTest {
        val mediaPreviewStore = FakeMediaPreviewConfigStateStore()
        val presenter = createAdvancedSettingsPresenter(mediaPreviewConfigStateStore = mediaPreviewStore)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            // Skip until the initial data it loaded
            skipItems(1)

            with(awaitItem()) {
                assertThat(mediaPreviewConfigState.timelineMediaPreviewValue).isEqualTo(MediaPreviewValue.On)
                eventSink(AdvancedSettingsEvents.SetTimelineMediaPreviewValue(MediaPreviewValue.Off))
            }
            with(awaitItem()) {
                assertThat(mediaPreviewConfigState.timelineMediaPreviewValue).isEqualTo(MediaPreviewValue.Off)
                eventSink(AdvancedSettingsEvents.SetTimelineMediaPreviewValue(MediaPreviewValue.Private))
            }
            with(awaitItem()) {
                assertThat(mediaPreviewConfigState.timelineMediaPreviewValue).isEqualTo(MediaPreviewValue.Private)
            }
        }
        assertThat(mediaPreviewStore.getSetTimelineMediaPreviewValueEvents()).isEqualTo(
            listOf(MediaPreviewValue.Off, MediaPreviewValue.Private)
        )
    }

    @Test
    fun `present - media preview state with custom initial values`() = runTest {
        val mediaPreviewStore = FakeMediaPreviewConfigStateStore(
            hideInviteAvatarsValue = true,
            timelineMediaPreviewValue = MediaPreviewValue.Private
        )
        val presenter = createAdvancedSettingsPresenter(mediaPreviewConfigStateStore = mediaPreviewStore)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            // Skip until the initial data it loaded
            skipItems(1)

            with(awaitItem()) {
                assertThat(mediaPreviewConfigState.hideInviteAvatars).isTrue()
                assertThat(mediaPreviewConfigState.timelineMediaPreviewValue).isEqualTo(MediaPreviewValue.Private)
            }
        }
    }

    @Test
    fun `present - async actions state`() = runTest {
        val mediaPreviewStore = FakeMediaPreviewConfigStateStore(
            setHideInviteAvatarsActionValue = AsyncAction.Loading,
            setTimelineMediaPreviewActionValue = AsyncAction.Success(Unit)
        )
        val presenter = createAdvancedSettingsPresenter(mediaPreviewConfigStateStore = mediaPreviewStore)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            // Skip until the initial data it loaded
            skipItems(1)

            with(awaitItem()) {
                assertThat(mediaPreviewConfigState.setHideInviteAvatarsAction).isEqualTo(AsyncAction.Loading)
                assertThat(mediaPreviewConfigState.setTimelineMediaPreviewAction).isEqualTo(AsyncAction.Success(Unit))
            }
        }
    }

    @Test
    fun `present - discoverable off applies to the node then persists`() = runTest {
        val neutrinoService = FakeNeutrinoService()
        val sessionPreferencesStore = InMemorySessionPreferencesStore()
        val presenter = createAdvancedSettingsPresenter(
            sessionPreferencesStore = sessionPreferencesStore,
            neutrinoService = neutrinoService,
        )
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)
            with(awaitItem()) {
                assertThat(isDiscoverable).isTrue()
                assertThat(isDiscoverabilityControlAvailable).isTrue()
                assertThat(setDiscoverableAction).isEqualTo(AsyncAction.Uninitialized)
                eventSink(AdvancedSettingsEvents.SetDiscoverable(false))
            }
            with(awaitSettled()) {
                assertThat(setDiscoverableAction).isEqualTo(AsyncAction.Success(Unit))
                assertThat(isDiscoverable).isFalse()
            }
            assertThat(neutrinoService.setDiscoverableCalls).containsExactly(false)
            assertThat(sessionPreferencesStore.isDiscoverable().first()).isFalse()
        }
    }

    @Test
    fun `present - discoverable off is not persisted when the node refuses`() = runTest {
        val neutrinoService = FakeNeutrinoService(
            setDiscoverableResult = { DiscoverableResult.Failed("Neutrino is not running") },
        )
        val sessionPreferencesStore = InMemorySessionPreferencesStore()
        val presenter = createAdvancedSettingsPresenter(
            sessionPreferencesStore = sessionPreferencesStore,
            neutrinoService = neutrinoService,
        )
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)
            awaitItem().eventSink(AdvancedSettingsEvents.SetDiscoverable(false))
            with(awaitSettled()) {
                val error = (setDiscoverableAction as AsyncAction.Failure).error
                assertThat(error).isInstanceOf(DiscoverabilityNotAppliedException::class.java)
                assertThat(error).hasMessageThat().isEqualTo("Neutrino is not running")
                assertThat(isDiscoverable).isTrue()
                eventSink(AdvancedSettingsEvents.ClearDiscoverableError)
            }
            assertThat(awaitItem().setDiscoverableAction).isEqualTo(AsyncAction.Uninitialized)
            assertThat(sessionPreferencesStore.isDiscoverable().first()).isTrue()
        }
    }

    @Test
    fun `present - discoverability control unavailable in this build`() = runTest {
        val neutrinoService = FakeNeutrinoService(discoverabilityControlAvailable = false)
        val sessionPreferencesStore = InMemorySessionPreferencesStore()
        val presenter = createAdvancedSettingsPresenter(
            sessionPreferencesStore = sessionPreferencesStore,
            neutrinoService = neutrinoService,
        )
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            skipItems(1)
            with(awaitItem()) {
                assertThat(isDiscoverabilityControlAvailable).isFalse()
                // Even if something sends the event, hiding is refused and never recorded.
                eventSink(AdvancedSettingsEvents.SetDiscoverable(false))
            }
            with(awaitSettled()) {
                val error = (setDiscoverableAction as AsyncAction.Failure).error as DiscoverabilityNotAppliedException
                assertThat(error.result).isEqualTo(DiscoverableResult.Unavailable)
                assertThat(isDiscoverable).isTrue()
            }
            assertThat(sessionPreferencesStore.isDiscoverable().first()).isTrue()
        }
    }

    // The fake service and in-memory store complete without suspending, so the
    // Loading emission may be collapsed into the terminal state (and the store's
    // own emission may or may not coincide with it).
    private suspend fun ReceiveTurbine<AdvancedSettingsState>.awaitSettled(): AdvancedSettingsState {
        var state = awaitItem()
        while (state.setDiscoverableAction is AsyncAction.Loading) {
            state = awaitItem()
        }
        return state
    }

    private fun CoroutineScope.createAdvancedSettingsPresenter(
        appPreferencesStore: AppPreferencesStore = InMemoryAppPreferencesStore(),
        sessionPreferencesStore: InMemorySessionPreferencesStore = InMemorySessionPreferencesStore(),
        mediaPreviewConfigStateStore: MediaPreviewConfigStateStore = FakeMediaPreviewConfigStateStore(),
        featureFlagService: FakeFeatureFlagService = FakeFeatureFlagService(),
        neutrinoService: NeutrinoService = FakeNeutrinoService(),
    ) = AdvancedSettingsPresenter(
        appPreferencesStore = appPreferencesStore,
        sessionPreferencesStore = sessionPreferencesStore,
        mediaPreviewConfigStateStore = mediaPreviewConfigStateStore,
        featureFlagService = featureFlagService,
        neutrinoService = neutrinoService,
        sessionCoroutineScope = this,
    )
}
