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

/**
 * Hosts the IndiaFOSS Companion PWA (schedule, ranking, itinerary, venue map,
 * contact cards) inside the chat app. The companion is an offline-first web
 * app; once loaded it keeps working without connectivity through its service
 * worker.
 *
 * When the native companion is installed, conference routes it understands are
 * handed to it instead and this screen closes. Links met inside the WebView are
 * classified by [ConferenceLinks] and acted on by [ConferenceLinkDispatcher]:
 * `matrix:` and `matrix.to` links and `indiafoss://chat…` / `friend…` cards go to
 * this app's messenger, anything outside the companion origin goes to the
 * matching system app, and unsupported schemes are dropped.
 */
class ConferenceActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var dispatcher: ConferenceLinkDispatcher

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        setContentView(webView)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    /** `indiafoss://conference/<route>`: the native companion when it takes the route, else that route on the PWA. */
    private fun open(intent: Intent?) {
        val route = ConferenceLinks.entry(intent?.data)
        if (dispatcher.openInCompanion(route)) {
            finish()
        } else {
            webView.loadUrl(route.pwaUrl)
        }
    }
}
