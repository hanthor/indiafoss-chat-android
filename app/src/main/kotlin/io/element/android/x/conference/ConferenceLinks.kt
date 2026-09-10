/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.conference

import android.net.Uri
import androidx.core.net.toUri
import io.element.android.appnav.intent.IndiafossLinks

/**
 * Where a link tapped inside the conference WebView, or an intent that opened
 * [ConferenceActivity], must go. Produced by [ConferenceLinks.classify], acted
 * on by [ConferenceLinkDispatcher].
 */
sealed interface ConferenceLinkDispatch {
    /** Same trusted companion origin and base path: the WebView keeps navigating. */
    data object InternalConference : ConferenceLinkDispatch

    /**
     * `matrix:` (MSC2312), `https://matrix.to/#/…` or `element://` permalink. The
     * messenger in this app opens it; a `matrix:` URI needs no network at all.
     */
    data class MatrixLink(val uri: Uri) : ConferenceLinkDispatch

    /** `indiafoss://chat…` / `indiafoss://friend…` payload that [IndiafossLinks] accepts: this app's messenger. */
    data class IndiafossHandoff(val uri: Uri) : ConferenceLinkDispatch

    /**
     * A conference route. [companionUri] is the deep link the native companion
     * answers to when it is installed, `null` when only the PWA knows the
     * route; [pwaUrl] is the same route on the hosted companion and is the
     * fallback. Query and fragment are carried on both.
     */
    data class CompanionRoute(val companionUri: Uri?, val pwaUrl: String) : ConferenceLinkDispatch

    /** Any other http(s) page: the system browser. */
    data class ExternalWeb(val uri: Uri) : ConferenceLinkDispatch

    /** `mailto:`, `tel:`, `sms:`: the matching system app. */
    data class Communication(val uri: Uri) : ConferenceLinkDispatch

    /** Unsupported or malformed: nothing is opened. [reason] carries no link content and is safe to log. */
    data class Rejected(val reason: String) : ConferenceLinkDispatch
}

/**
 * Pure classification of the links the conference screen meets. No Android
 * component is touched here, so every branch is unit-testable.
 */
object ConferenceLinks {
    /** Deployed companion PWA (GitHub Pages). Override for self-hosted deployments. */
    const val COMPANION_URL = "https://hanthor.github.io/indiafoss-companion/"

    /** Native IndiaFOSS Companion (`apps/android/native` in hanthor/indiafoss-companion). */
    const val COMPANION_PACKAGE = "org.indiafoss.companion.nativeapp"

    private const val SCHEME = "indiafoss"
    private const val CONFERENCE_HOST = "conference"
    private const val MAX_LENGTH = 2048
    private const val MAX_SEGMENTS = 8

    private val companion = COMPANION_URL.toUri()
    private val companionHost = companion.host!!.lowercase()
    private val companionBasePath = companion.path!!.trimEnd('/')
    private val companionBaseUrl = COMPANION_URL.trimEnd('/')

    /** Same alphabet and length the native companion accepts for a route id. */
    private val ROUTE_ID = Regex("^[A-Za-z0-9._:@#!-]{1,128}$")

    /** MSC2312 entity types, in the order the companion emits them. */
    private val MATRIX_URI_TYPES = listOf("u/", "r/", "roomid/")

    /** `indiafoss://<host>/<id>` links the native companion routes itself (see its `openDeepLink`). */
    private val NATIVE_ROUTE_HOSTS = setOf("activity", "location", "speaker")

    /** All `indiafoss://` hosts the companion PWA answers to besides chat/friend. */
    private val PWA_ROUTE_HOSTS = NATIVE_ROUTE_HOSTS + setOf("event", "booth")

    private val COMMUNICATION_SCHEMES = setOf("mailto", "tel", "sms", "smsto")

    /** The companion home: what [ConferenceActivity] shows when the intent carries no usable route. */
    val HOME = ConferenceLinkDispatch.CompanionRoute(
        companionUri = "$SCHEME://event/".toUri(),
        pwaUrl = COMPANION_URL,
    )

    fun classify(uri: Uri?): ConferenceLinkDispatch {
        if (uri == null) return ConferenceLinkDispatch.Rejected("no uri")
        if (uri.toString().length > MAX_LENGTH) return ConferenceLinkDispatch.Rejected("oversized")
        val scheme = uri.scheme?.lowercase() ?: return ConferenceLinkDispatch.Rejected("no scheme")
        return when (scheme) {
            "https", "http" -> classifyWeb(uri, scheme)
            "matrix" -> if (isMatrixUri(uri)) ConferenceLinkDispatch.MatrixLink(uri) else ConferenceLinkDispatch.Rejected("malformed matrix uri")
            "element" -> when (uri.host) {
                "user", "room" -> ConferenceLinkDispatch.MatrixLink(uri)
                else -> ConferenceLinkDispatch.Rejected("unsupported element link")
            }
            SCHEME -> classifyIndiafoss(uri)
            in COMMUNICATION_SCHEMES -> ConferenceLinkDispatch.Communication(uri)
            else -> ConferenceLinkDispatch.Rejected("unsupported scheme")
        }
    }

    /**
     * Where an intent that opened [ConferenceActivity] lands. Only a valid
     * `indiafoss://` route is honoured; anything else opens the companion home.
     */
    fun entry(data: Uri?): ConferenceLinkDispatch.CompanionRoute {
        return data?.let { classify(it) } as? ConferenceLinkDispatch.CompanionRoute ?: HOME
    }

    private fun classifyWeb(uri: Uri, scheme: String): ConferenceLinkDispatch {
        val host = uri.host?.lowercase()?.takeIf { it.isNotBlank() } ?: return ConferenceLinkDispatch.Rejected("no host")
        val fragment = uri.fragment.orEmpty()
        return when {
            uri.pathSegments.any { it == ".." } -> ConferenceLinkDispatch.Rejected("path traversal")
            scheme == "https" && isCompanionOrigin(uri, host) -> ConferenceLinkDispatch.InternalConference
            host == "matrix.to" && fragment.startsWith("/") && fragment.length > 1 -> ConferenceLinkDispatch.MatrixLink(uri)
            else -> ConferenceLinkDispatch.ExternalWeb(uri)
        }
    }

    /** The exact host on the default port, at or below the companion's base path. */
    private fun isCompanionOrigin(uri: Uri, host: String): Boolean {
        if (host != companionHost || uri.port != -1) return false
        val path = uri.path.orEmpty()
        return path == companionBasePath || path.startsWith("$companionBasePath/")
    }

    private fun isMatrixUri(uri: Uri): Boolean {
        val body = uri.schemeSpecificPart.orEmpty()
        if (body.startsWith("//") || body.any { it.isWhitespace() }) return false
        return MATRIX_URI_TYPES.any { body.startsWith(it) && body.length > it.length }
    }

    private fun classifyIndiafoss(uri: Uri): ConferenceLinkDispatch {
        val host = uri.host?.lowercase()?.takeIf { it.isNotBlank() } ?: return ConferenceLinkDispatch.Rejected("indiafoss link without host")
        return when (host) {
            "chat", "friend" -> if (IndiafossLinks.toMatrixTo(uri.toString()) != null) {
                ConferenceLinkDispatch.IndiafossHandoff(uri)
            } else {
                ConferenceLinkDispatch.Rejected("unsupported handoff payload")
            }
            CONFERENCE_HOST -> conferenceRoute(uri)
            in PWA_ROUTE_HOSTS -> companionRoute(host, uri)
            else -> ConferenceLinkDispatch.Rejected("unknown indiafoss host")
        }
    }

    /** `indiafoss://conference/<route>`: our own entry link, the route is a PWA path. */
    private fun conferenceRoute(uri: Uri): ConferenceLinkDispatch {
        val segments = uri.pathSegments
        if (segments.isEmpty()) return withSuffix(uri, HOME)
        if (segments.size > MAX_SEGMENTS) return ConferenceLinkDispatch.Rejected("too many path segments")
        if (segments.any { it == "." || it == ".." }) return ConferenceLinkDispatch.Rejected("path traversal")
        val kind = segments.first().lowercase()
        val id = segments.getOrNull(1)
        val companionUri = when {
            kind == "event" && segments.size == 1 -> HOME.companionUri
            kind in NATIVE_ROUTE_HOSTS && segments.size == 2 && isRouteId(id.orEmpty()) -> "$SCHEME://$kind/$id".toUri()
            else -> null
        }
        val path = segments.joinToString(separator = "") { "/" + Uri.encode(it) }
        return withSuffix(uri, ConferenceLinkDispatch.CompanionRoute(companionUri, companionBaseUrl + path))
    }

    /** `indiafoss://activity/<id>` and friends: the same links the companion PWA and native app answer to. */
    private fun companionRoute(host: String, uri: Uri): ConferenceLinkDispatch {
        val segments = uri.pathSegments
        if (segments.size > 1) return ConferenceLinkDispatch.Rejected("unexpected route depth")
        val id = segments.firstOrNull()
        if (id != null && !isRouteId(id)) return ConferenceLinkDispatch.Rejected("malformed route id")
        val encodedId = id?.let { Uri.encode(it) }
        val pwaPath = when (host) {
            "event" -> "/"
            "activity" -> encodedId?.let { "/activity/$it" } ?: "/schedule"
            "booth" -> encodedId?.let { "/booth/$it" } ?: "/explore/booths"
            "location" -> encodedId?.let { "/scan?payload=" + Uri.encode("$SCHEME://location/$it") } ?: "/map"
            "speaker" -> encodedId?.let { "/speaker/$it" } ?: "/schedule"
            else -> return ConferenceLinkDispatch.Rejected("unknown indiafoss host")
        }
        val companionUri = if (host == "event" || (host in NATIVE_ROUTE_HOSTS && id != null)) {
            "$SCHEME://$host/${encodedId.orEmpty()}".toUri()
        } else {
            null
        }
        val pwaUrl = if (pwaPath == "/") COMPANION_URL else companionBaseUrl + pwaPath
        return if (pwaPath.contains('?')) {
            ConferenceLinkDispatch.CompanionRoute(companionUri, pwaUrl)
        } else {
            withSuffix(uri, ConferenceLinkDispatch.CompanionRoute(companionUri, pwaUrl))
        }
    }

    private fun isRouteId(id: String): Boolean = id != "." && id != ".." && ROUTE_ID.matches(id)

    /** Carries the source query and fragment (event context such as `?at=<location>`) onto both targets. */
    private fun withSuffix(source: Uri, route: ConferenceLinkDispatch.CompanionRoute): ConferenceLinkDispatch.CompanionRoute {
        val query = source.encodedQuery?.let { "?$it" }.orEmpty()
        val fragment = source.encodedFragment?.let { "#$it" }.orEmpty()
        if (query.isEmpty() && fragment.isEmpty()) return route
        return ConferenceLinkDispatch.CompanionRoute(
            companionUri = route.companionUri?.let { (it.toString() + query + fragment).toUri() },
            pwaUrl = route.pwaUrl + query + fragment,
        )
    }
}
