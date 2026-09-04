/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.conference

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toUri

/**
 * Hosts the IndiaFOSS Companion PWA (schedule, ranking, itinerary, venue map,
 * contact cards) inside the chat app. The companion is an offline-first web
 * app; once loaded it keeps working without connectivity through its service
 * worker. `indiafoss://chat…` and `indiafoss://friend…` links inside the
 * companion are handed back to the chat app's intent resolver, and other
 * `indiafoss://` links (location markers, activities) stay in the WebView.
 */
class ConferenceActivity : ComponentActivity() {
    private lateinit var webView: WebView

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
                    val url = request.url
                    return when {
                        url.scheme == "indiafoss" && (url.host == "chat" || url.host == "friend") -> {
                            startActivity(Intent(Intent.ACTION_VIEW, url).setPackage(packageName))
                            true
                        }
                        url.scheme == "https" && url.host == "matrix.to" -> {
                            startActivity(Intent(Intent.ACTION_VIEW, url).setPackage(packageName))
                            true
                        }
                        url.scheme == "https" || url.scheme == "http" -> {
                            // Stay inside the companion origin; everything else opens in the browser.
                            val companion = COMPANION_URL.toUri()
                            if (url.host == companion.host) {
                                false
                            } else {
                                startActivity(Intent(Intent.ACTION_VIEW, url))
                                true
                            }
                        }
                        url.scheme == "mailto" || url.scheme == "tel" || url.scheme == "sms" -> {
                            startActivity(Intent(Intent.ACTION_VIEW, url))
                            true
                        }
                        else -> false
                    }
                }
            }
        }
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
            webView.loadUrl(targetUrl(intent))
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        webView.loadUrl(targetUrl(intent))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    /** `indiafoss://conference/<path>` opens that companion route; anything else opens the home. */
    private fun targetUrl(intent: Intent?): String {
        val data: Uri? = intent?.data
        val path = data?.takeIf { it.scheme == "indiafoss" && it.host == "conference" }?.path.orEmpty()
        val query = data?.encodedQuery?.let { "?$it" }.orEmpty()
        return COMPANION_URL.trimEnd('/') + path + query
    }

    companion object {
        /** Deployed companion PWA (GitHub Pages). Override for self-hosted deployments. */
        const val COMPANION_URL = "https://hanthor.github.io/indiafoss-companion/"
    }
}
