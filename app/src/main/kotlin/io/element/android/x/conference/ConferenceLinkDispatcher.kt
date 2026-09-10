/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.conference

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import io.element.android.libraries.androidutils.R
import io.element.android.libraries.androidutils.system.toast
import timber.log.Timber

/**
 * Acts on a [ConferenceLinkDispatch]. Every `startActivity` is guarded: a
 * missing handler (no browser, no mail client, restricted profile) is explained
 * with the same toast the rest of the app uses instead of crashing.
 */
internal class ConferenceLinkDispatcher(
    private val activity: Activity,
    private val loadInWebView: (String) -> Unit,
    private val isCompanionInstalled: () -> Boolean = { activity.isCompanionInstalled() },
) {
    /** @return `true` when the WebView must not navigate to the link itself. */
    fun dispatch(link: ConferenceLinkDispatch): Boolean {
        when (link) {
            ConferenceLinkDispatch.InternalConference -> return false
            is ConferenceLinkDispatch.MatrixLink -> openOrExplain(Intent(Intent.ACTION_VIEW, link.uri).setPackage(activity.packageName))
            is ConferenceLinkDispatch.IndiafossHandoff -> openOrExplain(Intent(Intent.ACTION_VIEW, link.uri).setPackage(activity.packageName))
            is ConferenceLinkDispatch.CompanionRoute -> if (!openInCompanion(link)) loadInWebView(link.pwaUrl)
            is ConferenceLinkDispatch.ExternalWeb -> openOrExplain(Intent(Intent.ACTION_VIEW, link.uri))
            is ConferenceLinkDispatch.Communication -> openOrExplain(Intent(Intent.ACTION_VIEW, link.uri))
            is ConferenceLinkDispatch.Rejected -> Timber.w("Conference link blocked: ${link.reason}")
        }
        return true
    }

    /**
     * Hands [route] to the installed native companion. `false` when it is not
     * installed, cannot take this route, or refuses the intent: the caller then
     * shows the PWA route instead, so the user always lands somewhere.
     */
    fun openInCompanion(route: ConferenceLinkDispatch.CompanionRoute): Boolean {
        val target = route.companionUri ?: return false
        if (!isCompanionInstalled()) return false
        return try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, target).setPackage(ConferenceLinks.COMPANION_PACKAGE))
            true
        } catch (e: ActivityNotFoundException) {
            Timber.w(e, "Native companion declined the route, falling back to the PWA")
            false
        }
    }

    private fun openOrExplain(intent: Intent) {
        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Timber.w(e, "No app can open a ${intent.data?.scheme} link")
            activity.toast(R.string.error_no_compatible_app_found)
        }
    }
}

/** Needs the `<queries>` entry in the manifest to be visible on Android 11+. */
internal fun Context.isCompanionInstalled(): Boolean {
    return packageManager.getLaunchIntentForPackage(ConferenceLinks.COMPANION_PACKAGE) != null
}
