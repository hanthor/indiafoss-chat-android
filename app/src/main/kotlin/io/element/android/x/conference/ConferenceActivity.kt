/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.conference

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import io.element.android.compound.theme.ElementTheme
import io.element.android.x.conference.ConferenceLinkDispatch.CompanionRoute

/**
 * Hosts the IndiaFOSS Companion PWA (schedule, ranking, itinerary, venue map,
 * contact cards) inside the chat app. The companion is an offline-first web
 * app; once loaded it keeps working without connectivity through its service
 * worker.
 *
 * When the native companion is installed, conference routes it understands are
 * handed to it instead and this screen closes. Otherwise an event-identity
 * header sits above the PWA with an "Open Companion" action decided by
 * [ConferenceEntry]. Links met inside the WebView are classified by
 * [ConferenceLinks] and acted on by [ConferenceLinkDispatcher]: `matrix:` and
 * `matrix.to` links and `indiafoss://chat…` / `friend…` cards go to this app's
 * messenger, anything outside the companion origin goes to the matching system
 * app, and unsupported schemes are dropped.
 */
class ConferenceActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var dispatcher: ConferenceLinkDispatcher
    private var route: CompanionRoute = ConferenceLinks.HOME
    private var companionAction by mutableStateOf<CompanionAction>(CompanionAction.OpenInBrowser(ConferenceLinks.COMPANION_URL))

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    return dispatcher.dispatch(ConferenceLinks.classify(request.url))
                }
            }
        }
        dispatcher = ConferenceLinkDispatcher(this, loadInWebView = { webView.loadUrl(it) })
        setContent {
            // Light status-bar icons in both themes: they sit on the ink header.
            ElementTheme(lightStatusBar = false) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ElementTheme.colors.bgCanvasDefault),
                ) {
                    ConferenceHeader(
                        state = ConferenceHeaderState(hasNativeCompanion = companionAction is CompanionAction.OpenNative),
                        onOpenCompanionClick = ::onOpenCompanionClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AndroidView(
                        factory = { webView },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .navigationBarsPadding(),
                    )
                }
            }
        }
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) webView.goBack() else finish()
                }
            }
        )
        if (savedInstanceState == null) {
            open(intent)
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        open(intent)
    }

    override fun onResume() {
        super.onResume()
        // The Companion may have been installed or removed while this screen was in the background.
        refreshCompanionAction()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    /** `indiafoss://conference/<route>`: the native companion when it takes the route, else that route on the PWA. */
    private fun open(intent: Intent?) {
        route = ConferenceLinks.entry(intent?.data)
        if (dispatcher.openInCompanion(route)) {
            finish()
        } else {
            refreshCompanionAction()
            webView.loadUrl(route.pwaUrl)
        }
    }

    private fun refreshCompanionAction() {
        companionAction = ConferenceEntry.companionAction(route, isCompanionInstalled())
    }

    /** Native Companion first; the browser when it is absent or declines, so the tap always lands somewhere. */
    private fun onOpenCompanionClick() {
        val action = companionAction
        if (action is CompanionAction.OpenNative && dispatcher.openInCompanion(action.route)) return
        val url = (action as? CompanionAction.OpenInBrowser)?.url ?: route.pwaUrl
        dispatcher.dispatch(ConferenceLinkDispatch.ExternalWeb(url.toUri()))
    }
}
