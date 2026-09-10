/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.ftue.impl.discovery

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.preferences.test.InMemorySessionPreferencesStore
import io.element.android.services.neutrino.api.DiscoverabilityNotAppliedException
import io.element.android.services.neutrino.api.DiscoverableResult
import io.element.android.services.neutrino.test.FakeNeutrinoService
import io.element.android.tests.testutils.WarmUpRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class DiscoveryOptInPresenterTest {
    @get:Rule
    val warmUpRule = WarmUpRule()

    @Test
    fun `initial state offers hiding when the build can hide`() = runTest {
        val presenter = createPresenter()
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            val state = awaitItem()
            assertThat(state.isHideAvailable).isTrue()
            assertThat(state.submitAction).isEqualTo(AsyncAction.Uninitialized)
        }
    }

    @Test
    fun `initial state reports hiding unavailable when the build lacks the binding`() = runTest {
        val neutrinoService = FakeNeutrinoService(discoverabilityControlAvailable = false)
        val presenter = createPresenter(neutrinoService = neutrinoService)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            assertThat(awaitItem().isHideAvailable).isFalse()
        }
    }

    @Test
    fun `choosing hidden applies it to the node, persists it and advances`() = runTest {
        val neutrinoService = FakeNeutrinoService()
        val preferences = InMemorySessionPreferencesStore()
        val callback = RecordingCallback()
        val presenter = createPresenter(neutrinoService, preferences, callback)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            awaitItem().eventSink(DiscoveryOptInEvents.Choose(discoverable = false))
            assertThat(awaitItem().submitAction).isEqualTo(AsyncAction.Loading)
            assertThat(awaitItem().submitAction).isEqualTo(AsyncAction.Success(Unit))
            assertThat(neutrinoService.setDiscoverableCalls).containsExactly(false)
            assertThat(preferences.isDiscoverable().first()).isFalse()
            assertThat(preferences.isDiscoveryPromptCompleted().first()).isTrue()
            assertThat(callback.choiceMadeCount).isEqualTo(1)
        }
    }

    @Test
    fun `choosing discoverable on a build without the binding still completes the prompt`() = runTest {
        val neutrinoService = FakeNeutrinoService(discoverabilityControlAvailable = false)
        val preferences = InMemorySessionPreferencesStore()
        val callback = RecordingCallback()
        val presenter = createPresenter(neutrinoService, preferences, callback)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            awaitItem().eventSink(DiscoveryOptInEvents.Choose(discoverable = true))
            assertThat(awaitItem().submitAction).isEqualTo(AsyncAction.Loading)
            assertThat(awaitItem().submitAction).isEqualTo(AsyncAction.Success(Unit))
            assertThat(preferences.isDiscoverable().first()).isTrue()
            assertThat(preferences.isDiscoveryPromptCompleted().first()).isTrue()
            assertThat(callback.choiceMadeCount).isEqualTo(1)
        }
    }

    @Test
    fun `choosing hidden on a build without the binding fails and records nothing`() = runTest {
        val neutrinoService = FakeNeutrinoService(discoverabilityControlAvailable = false)
        val preferences = InMemorySessionPreferencesStore()
        val callback = RecordingCallback()
        val presenter = createPresenter(neutrinoService, preferences, callback)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            awaitItem().eventSink(DiscoveryOptInEvents.Choose(discoverable = false))
            assertThat(awaitItem().submitAction).isEqualTo(AsyncAction.Loading)
            val failure = awaitItem().submitAction
            assertThat(failure).isInstanceOf(AsyncAction.Failure::class.java)
            val error = (failure as AsyncAction.Failure).error
            assertThat(error).isInstanceOf(DiscoverabilityNotAppliedException::class.java)
            assertThat((error as DiscoverabilityNotAppliedException).result).isEqualTo(DiscoverableResult.Unavailable)
            // Nothing claims "hidden": the preference keeps its default and the prompt is not marked done.
            assertThat(preferences.isDiscoverable().first()).isTrue()
            assertThat(preferences.isDiscoveryPromptCompleted().first()).isFalse()
            assertThat(callback.choiceMadeCount).isEqualTo(0)
        }
    }

    @Test
    fun `a failed node call surfaces the reason and can be cleared`() = runTest {
        val neutrinoService = FakeNeutrinoService(
            setDiscoverableResult = { DiscoverableResult.Failed("Neutrino is not running") },
        )
        val preferences = InMemorySessionPreferencesStore()
        val callback = RecordingCallback()
        val presenter = createPresenter(neutrinoService, preferences, callback)
        moleculeFlow(RecompositionMode.Immediate) {
            presenter.present()
        }.test {
            awaitItem().eventSink(DiscoveryOptInEvents.Choose(discoverable = false))
            assertThat(awaitItem().submitAction).isEqualTo(AsyncAction.Loading)
            val state = awaitItem()
            val error = (state.submitAction as AsyncAction.Failure).error
            assertThat(error).hasMessageThat().isEqualTo("Neutrino is not running")
            assertThat(preferences.isDiscoverable().first()).isTrue()
            assertThat(preferences.isDiscoveryPromptCompleted().first()).isFalse()
            assertThat(callback.choiceMadeCount).isEqualTo(0)
            state.eventSink(DiscoveryOptInEvents.ClearError)
            assertThat(awaitItem().submitAction).isEqualTo(AsyncAction.Uninitialized)
        }
    }

    private fun createPresenter(
        neutrinoService: FakeNeutrinoService = FakeNeutrinoService(),
        sessionPreferencesStore: InMemorySessionPreferencesStore = InMemorySessionPreferencesStore(),
        callback: DiscoveryOptInNode.Callback = RecordingCallback(),
    ) = DiscoveryOptInPresenter(
        callback = callback,
        sessionPreferencesStore = sessionPreferencesStore,
        neutrinoService = neutrinoService,
    )

    private class RecordingCallback : DiscoveryOptInNode.Callback {
        var choiceMadeCount = 0

        override fun onDiscoveryChoiceMade() {
            choiceMadeCount++
        }
    }
}
