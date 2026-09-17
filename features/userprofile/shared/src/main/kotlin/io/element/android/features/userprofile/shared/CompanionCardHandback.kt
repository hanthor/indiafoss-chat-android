/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.userprofile.shared

import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.core.meshNodeId
import java.net.URLEncoder

/**
 * The link that puts this account's chat id on the attendee's Companion card.
 *
 * Reading a 64-hex node id off this screen and typing it into the Companion is
 * not something anyone does at a booth, so "My code" hands it over instead:
 * `indiafoss://conference/connect?mesh=<node id>` for a mesh session, or
 * `?matrix=<@user:server>` for an internet account. [ConferenceActivity] opens
 * that route in the Companion (native app or PWA), which asks before writing
 * anything to the card. Only the public address the card would carry anyway
 * ever leaves this app.
 */
object CompanionCardHandback {
    private const val ROUTE = "indiafoss://conference/connect"

    fun uriFor(userId: UserId): String {
        val nodeId = userId.meshNodeId
        return if (nodeId != null) {
            "$ROUTE?mesh=$nodeId"
        } else {
            "$ROUTE?matrix=" + URLEncoder.encode(userId.value, Charsets.UTF_8.name())
        }
    }
}
