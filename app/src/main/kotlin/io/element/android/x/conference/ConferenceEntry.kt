/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.conference

import io.element.android.x.conference.ConferenceLinkDispatch.CompanionRoute

/** What the "Open Companion" action on the conference entry does for the route on screen. */
sealed interface CompanionAction {
    /** The native Companion is installed: hand it [route] (this route when it answers to it, else the Companion home). */
    data class OpenNative(val route: CompanionRoute) : CompanionAction

    /** No native Companion: the same route on the hosted PWA, in the system browser, where it can also be installed. */
    data class OpenInBrowser(val url: String) : CompanionAction
}

/**
 * Pure decision behind the conference entry header. Nothing Android is
 * touched here; [ConferenceLinkDispatcher] performs whichever action is
 * chosen and falls back to the browser when the native Companion declines.
 */
object ConferenceEntry {
    fun companionAction(route: CompanionRoute, isCompanionInstalled: Boolean): CompanionAction {
        if (!isCompanionInstalled) return CompanionAction.OpenInBrowser(route.pwaUrl)
        val nativeRoute = if (route.companionUri != null) route else ConferenceLinks.HOME
        return CompanionAction.OpenNative(nativeRoute)
    }
}
