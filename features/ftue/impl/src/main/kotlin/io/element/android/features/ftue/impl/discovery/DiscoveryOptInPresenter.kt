/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.ftue.impl.discovery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.architecture.runCatchingUpdatingState
import io.element.android.libraries.preferences.api.store.SessionPreferencesStore
import io.element.android.services.neutrino.api.NeutrinoService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@AssistedInject
class DiscoveryOptInPresenter(
    @Assisted private val callback: DiscoveryOptInNode.Callback,
    private val sessionPreferencesStore: SessionPreferencesStore,
    private val neutrinoService: NeutrinoService,
) : Presenter<DiscoveryOptInState> {
    @AssistedFactory
    interface Factory {
        fun create(callback: DiscoveryOptInNode.Callback): DiscoveryOptInPresenter
    }

    @Composable
    override fun present(): DiscoveryOptInState {
        val submitAction = remember { mutableStateOf<AsyncAction<Unit>>(AsyncAction.Uninitialized) }
        val coroutineScope = rememberCoroutineScope()

        fun handleEvent(event: DiscoveryOptInEvents) {
            when (event) {
                is DiscoveryOptInEvents.Choose -> if (submitAction.value !is AsyncAction.Loading) {
                    coroutineScope.submit(event.discoverable, submitAction)
                }
            }
        }

        return DiscoveryOptInState(
            submitAction = submitAction.value,
            eventSink = ::handleEvent,
        )
    }

    // Persist the choice, tell the embedded node whether to keep advertising over
    // the BLE mesh, then remember that we've prompted so it isn't asked again, and
    // advance the FTUE.
    private fun CoroutineScope.submit(
        discoverable: Boolean,
        action: MutableState<AsyncAction<Unit>>,
    ) = launch {
        suspend {
            sessionPreferencesStore.setDiscoverable(discoverable)
            neutrinoService.setDiscoverable(discoverable)
            sessionPreferencesStore.setDiscoveryPromptCompleted(true)
        }.runCatchingUpdatingState(action)
        if (action.value is AsyncAction.Success) {
            callback.onDiscoveryChoiceMade()
        }
    }
}
