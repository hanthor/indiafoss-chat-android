/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.startchat.impl.scanqr

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import com.bumble.appyx.core.plugin.plugins
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import io.element.android.annotations.ContributesNode
import io.element.android.features.startchat.StartChatNavigator
import io.element.android.libraries.di.SessionScope

@ContributesNode(SessionScope::class)
@AssistedInject
class ScanQrNode(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    presenterFactory: ScanQrPresenter.Factory,
) : Node(buildContext, plugins = plugins) {
    private val navigator = plugins<StartChatNavigator>().first()
    private val presenter = presenterFactory.create(navigator)

    @Composable
    override fun View(modifier: Modifier) {
        val state = presenter.present()
        ScanQrView(
            state = state,
            onBackClick = { state.eventSink(ScanQrEvents.Dismiss) },
            modifier = modifier,
        )
    }
}
