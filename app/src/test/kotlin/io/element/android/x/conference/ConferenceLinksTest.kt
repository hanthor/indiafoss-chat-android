/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.conference

import android.app.Application
import androidx.core.net.toUri
import com.google.common.truth.Truth.assertThat
import io.element.android.x.conference.ConferenceLinkDispatch.CompanionRoute
import io.element.android.x.conference.ConferenceLinkDispatch.Communication
import io.element.android.x.conference.ConferenceLinkDispatch.ExternalWeb
import io.element.android.x.conference.ConferenceLinkDispatch.IndiafossHandoff
import io.element.android.x.conference.ConferenceLinkDispatch.InternalConference
import io.element.android.x.conference.ConferenceLinkDispatch.MatrixLink
import io.element.android.x.conference.ConferenceLinkDispatch.Rejected
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Unit tests need android.net.Uri, not the production app and native SDK startup.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ConferenceLinksTest {
    private val base = ConferenceLinks.COMPANION_URL.trimEnd('/')

    private fun classify(link: String) = ConferenceLinks.classify(link.toUri())

    // ---- Internal navigation is limited to the trusted origin and base path ----

    @Test
    fun `companion origin and base path stay in the WebView`() {
        assertThat(classify("https://hanthor.github.io/indiafoss-companion/")).isEqualTo(InternalConference)
        assertThat(classify("https://hanthor.github.io/indiafoss-companion")).isEqualTo(InternalConference)
        assertThat(classify("https://HANTHOR.github.io/indiafoss-companion/schedule?day=2#now")).isEqualTo(InternalConference)
    }

    @Test
    fun `same host outside the companion path, plain http, odd port or lookalike hosts go to the browser`() {
        assertThat(classify("https://hanthor.github.io/other-site/")).isInstanceOf(ExternalWeb::class.java)
        assertThat(classify("https://hanthor.github.io/indiafoss-companion-evil/")).isInstanceOf(ExternalWeb::class.java)
        assertThat(classify("http://hanthor.github.io/indiafoss-companion/")).isInstanceOf(ExternalWeb::class.java)
        assertThat(classify("https://hanthor.github.io:8443/indiafoss-companion/")).isInstanceOf(ExternalWeb::class.java)
        assertThat(classify("https://hanthor.github.io@evil.example/indiafoss-companion/")).isInstanceOf(ExternalWeb::class.java)
        // Cyrillic "і" in the TLD: a unicode lookalike must never count as our origin.
        assertThat(classify("https://hanthor.github.іo/indiafoss-companion/")).isInstanceOf(ExternalWeb::class.java)
        assertThat(classify("https://fossunited.org/indiafoss")).isEqualTo(ExternalWeb("https://fossunited.org/indiafoss".toUri()))
    }

    @Test
    fun `path traversal inside the companion path is rejected`() {
        assertThat(classify("https://hanthor.github.io/indiafoss-companion/../admin/")).isInstanceOf(Rejected::class.java)
        assertThat(classify("https://hanthor.github.io/indiafoss-companion/%2e%2e/admin/")).isInstanceOf(Rejected::class.java)
    }

    // ---- Matrix links open in this app, matrix: needs no redirect website ----

    @Test
    fun `matrix uris the companion emits open in this app`() {
        val room = "matrix:r/indiafoss-2026-session-act-1:matrix.org?action=join"
        val user = "matrix:u/alice:matrix.org?action=chat"
        val roomId = "matrix:roomid/abc:matrix.org?action=join&via=matrix.org"
        assertThat(classify(room)).isEqualTo(MatrixLink(room.toUri()))
        assertThat(classify(user)).isEqualTo(MatrixLink(user.toUri()))
        assertThat(classify(roomId)).isEqualTo(MatrixLink(roomId.toUri()))
    }

    @Test
    fun `malformed matrix uris are rejected`() {
        assertThat(classify("matrix:")).isInstanceOf(Rejected::class.java)
        assertThat(classify("matrix:r/")).isInstanceOf(Rejected::class.java)
        assertThat(classify("matrix:nope/alice:matrix.org")).isInstanceOf(Rejected::class.java)
        assertThat(classify("matrix://u/alice:matrix.org")).isInstanceOf(Rejected::class.java)
        assertThat(classify("matrix:u/alice :matrix.org")).isInstanceOf(Rejected::class.java)
    }

    @Test
    fun `matrix to permalinks and element scheme links open in this app`() {
        val room = "https://matrix.to/#/%23indiafoss%3Amatrix.org"
        val user = "https://matrix.to/#/@alice:matrix.org"
        assertThat(classify(room)).isEqualTo(MatrixLink(room.toUri()))
        assertThat(classify(user)).isEqualTo(MatrixLink(user.toUri()))
        assertThat(classify("element://user/@alice:matrix.org")).isInstanceOf(MatrixLink::class.java)
        assertThat(classify("element://room/#indiafoss:matrix.org")).isInstanceOf(MatrixLink::class.java)
    }

    @Test
    fun `matrix to without a permalink is just a website`() {
        assertThat(classify("https://matrix.to/")).isInstanceOf(ExternalWeb::class.java)
        assertThat(classify("https://matrix.to/#/")).isInstanceOf(ExternalWeb::class.java)
        assertThat(classify("https://matrix.to/about")).isInstanceOf(ExternalWeb::class.java)
    }

    // ---- indiafoss:// handoff cards ----

    @Test
    fun `chat and friend cards the intent resolver understands are handed to this app`() {
        val dm = "indiafoss://chat?dm=%40alice%3Amatrix.org"
        val join = "indiafoss://chat?join=%23indiafoss%3Amatrix.org"
        val friend = "indiafoss://friend?v=1&fn=Ada&matrix_id=%40ada%3Ax.org"
        assertThat(classify(dm)).isEqualTo(IndiafossHandoff(dm.toUri()))
        assertThat(classify(join)).isEqualTo(IndiafossHandoff(join.toUri()))
        assertThat(classify(friend)).isEqualTo(IndiafossHandoff(friend.toUri()))
    }

    @Test
    fun `chat and friend cards with an unusable payload are rejected instead of launching the messenger`() {
        assertThat(classify("indiafoss://chat")).isInstanceOf(Rejected::class.java)
        assertThat(classify("indiafoss://chat?dm=not-an-mxid")).isInstanceOf(Rejected::class.java)
        assertThat(classify("indiafoss://friend?v=2&matrix_id=%40ada%3Ax.org")).isInstanceOf(Rejected::class.java)
    }

    // ---- indiafoss:// companion routes: native companion first, PWA route as fallback ----

    @Test
    fun `routes the native companion handles carry the same deep link and the PWA route`() {
        assertThat(classify("indiafoss://activity/session-1")).isEqualTo(
            CompanionRoute("indiafoss://activity/session-1".toUri(), "$base/activity/session-1")
        )
        assertThat(classify("indiafoss://speaker/ada")).isEqualTo(
            CompanionRoute("indiafoss://speaker/ada".toUri(), "$base/speaker/ada")
        )
        assertThat(classify("indiafoss://location/hall-a")).isEqualTo(
            CompanionRoute("indiafoss://location/hall-a".toUri(), "$base/scan?payload=indiafoss%3A%2F%2Flocation%2Fhall-a")
        )
        assertThat(classify("indiafoss://event/")).isEqualTo(ConferenceLinks.HOME)
    }

    @Test
    fun `routes only the PWA knows have no companion deep link`() {
        assertThat(classify("indiafoss://booth/b1")).isEqualTo(CompanionRoute(null, "$base/booth/b1"))
        assertThat(classify("indiafoss://activity/")).isEqualTo(CompanionRoute(null, "$base/schedule"))
        assertThat(classify("indiafoss://booth")).isEqualTo(CompanionRoute(null, "$base/explore/booths"))
        assertThat(classify("indiafoss://location")).isEqualTo(CompanionRoute(null, "$base/map"))
    }

    @Test
    fun `route ids must match the companion alphabet and depth`() {
        assertThat(classify("indiafoss://activity/has%20space")).isInstanceOf(Rejected::class.java)
        assertThat(classify("indiafoss://activity/${"x".repeat(129)}")).isInstanceOf(Rejected::class.java)
        assertThat(classify("indiafoss://activity/a/b")).isInstanceOf(Rejected::class.java)
        assertThat(classify("indiafoss://activity/..")).isInstanceOf(Rejected::class.java)
        assertThat(classify("indiafoss://ticket/123")).isInstanceOf(Rejected::class.java)
        assertThat(classify("indiafoss:activity/session-1")).isInstanceOf(Rejected::class.java)
    }

    // ---- Communication and everything else ----

    @Test
    fun `mailto tel and sms go to the matching system app`() {
        assertThat(classify("mailto:hello@fossunited.org")).isEqualTo(Communication("mailto:hello@fossunited.org".toUri()))
        assertThat(classify("tel:+911234567890")).isInstanceOf(Communication::class.java)
        assertThat(classify("sms:+911234567890?body=hi")).isInstanceOf(Communication::class.java)
        assertThat(classify("MAILTO:hello@fossunited.org")).isInstanceOf(Communication::class.java)
    }

    @Test
    fun `unsupported and dangerous schemes are rejected`() {
        assertThat(classify("javascript:alert(1)")).isInstanceOf(Rejected::class.java)
        assertThat(classify("JavaScript:alert(1)")).isInstanceOf(Rejected::class.java)
        assertThat(classify("data:text/html,<script>alert(1)</script>")).isInstanceOf(Rejected::class.java)
        assertThat(classify("file:///etc/hosts")).isInstanceOf(Rejected::class.java)
        assertThat(classify("content://org.indiafoss.chat.fileprovider/x")).isInstanceOf(Rejected::class.java)
        assertThat(classify("intent://scan/#Intent;scheme=zxing;end")).isInstanceOf(Rejected::class.java)
        assertThat(classify("about:blank")).isInstanceOf(Rejected::class.java)
        assertThat(classify("ftp://example.org/file")).isInstanceOf(Rejected::class.java)
    }

    @Test
    fun `empty relative and oversized input is rejected without throwing`() {
        assertThat(ConferenceLinks.classify(null)).isInstanceOf(Rejected::class.java)
        assertThat(classify("")).isInstanceOf(Rejected::class.java)
        assertThat(classify("   ")).isInstanceOf(Rejected::class.java)
        assertThat(classify("/schedule")).isInstanceOf(Rejected::class.java)
        assertThat(classify("schedule")).isInstanceOf(Rejected::class.java)
        assertThat(classify("https://")).isInstanceOf(Rejected::class.java)
        assertThat(classify("https://hanthor.github.io/indiafoss-companion/" + "a".repeat(2100))).isInstanceOf(Rejected::class.java)
        assertThat(classify("matrix:r/" + "a".repeat(2100))).isInstanceOf(Rejected::class.java)
    }

    // ---- Entry intents: indiafoss://conference/<route> ----

    @Test
    fun `no data or a bare conference link opens the companion home`() {
        assertThat(ConferenceLinks.entry(null)).isEqualTo(ConferenceLinks.HOME)
        assertThat(ConferenceLinks.entry("indiafoss://conference".toUri())).isEqualTo(ConferenceLinks.HOME)
        assertThat(ConferenceLinks.entry("indiafoss://conference/".toUri())).isEqualTo(ConferenceLinks.HOME)
        assertThat(ConferenceLinks.HOME.pwaUrl).isEqualTo(ConferenceLinks.COMPANION_URL)
        assertThat(ConferenceLinks.HOME.companionUri).isEqualTo("indiafoss://event/".toUri())
    }

    @Test
    fun `a conference route keeps its path query and fragment on the PWA url`() {
        val route = ConferenceLinks.entry("indiafoss://conference/schedule?day=2#now".toUri())
        assertThat(route.pwaUrl).isEqualTo("$base/schedule?day=2#now")
        // The PWA-only route is not something the native companion can show.
        assertThat(route.companionUri).isNull()
        assertThat(ConferenceLinks.entry("indiafoss://conference/explore/booths".toUri()).pwaUrl).isEqualTo("$base/explore/booths")
    }

    @Test
    fun `a conference route the native companion handles is translated to its deep link with the event context`() {
        val route = ConferenceLinks.entry("indiafoss://conference/activity/session-1?at=hall-a".toUri())
        assertThat(route.companionUri).isEqualTo("indiafoss://activity/session-1?at=hall-a".toUri())
        assertThat(route.pwaUrl).isEqualTo("$base/activity/session-1?at=hall-a")
        assertThat(ConferenceLinks.entry("indiafoss://conference/location/hall-a".toUri()).companionUri)
            .isEqualTo("indiafoss://location/hall-a".toUri())
        assertThat(ConferenceLinks.entry("indiafoss://conference/event".toUri()).companionUri).isEqualTo(ConferenceLinks.HOME.companionUri)
        // Three segments, or an id outside the companion alphabet, stays on the PWA.
        assertThat(ConferenceLinks.entry("indiafoss://conference/activity/session-1/notes".toUri()).companionUri).isNull()
        assertThat(ConferenceLinks.entry("indiafoss://conference/activity/has%20space".toUri()).companionUri).isNull()
    }

    @Test
    fun `the path always joins the base url with exactly one slash`() {
        // The alleged missing-leading-slash concatenation: android.net.Uri never yields a path without one.
        assertThat("indiafoss://conference/schedule".toUri().path).isEqualTo("/schedule")
        assertThat(ConferenceLinks.entry("indiafoss://conference/schedule".toUri()).pwaUrl).isEqualTo("$base/schedule")
        // A malformed host such as "conferenceschedule" is not our route at all, so it cannot produce a joined url.
        assertThat(ConferenceLinks.entry("indiafoss://conferenceschedule".toUri())).isEqualTo(ConferenceLinks.HOME)
        // Double and trailing slashes collapse instead of reaching GitHub Pages as "//schedule".
        assertThat(ConferenceLinks.entry("indiafoss://conference//schedule".toUri()).pwaUrl).isEqualTo("$base/schedule")
        assertThat(ConferenceLinks.entry("indiafoss://conference/schedule//".toUri()).pwaUrl).isEqualTo("$base/schedule")
        assertThat(ConferenceLinks.entry("indiafoss://conference///".toUri())).isEqualTo(ConferenceLinks.HOME)
    }

    @Test
    fun `path segments are re-encoded and traversal or excess depth falls back to home`() {
        assertThat(ConferenceLinks.entry("indiafoss://conference/sch%20edule".toUri()).pwaUrl).isEqualTo("$base/sch%20edule")
        assertThat(ConferenceLinks.entry("indiafoss://conference/schédule".toUri()).pwaUrl).isEqualTo("$base/sch%C3%A9dule")
        assertThat(ConferenceLinks.entry("indiafoss://conference/../../evil".toUri())).isEqualTo(ConferenceLinks.HOME)
        assertThat(ConferenceLinks.entry("indiafoss://conference/%2e%2e/evil".toUri())).isEqualTo(ConferenceLinks.HOME)
        assertThat(ConferenceLinks.entry("indiafoss://conference/./schedule".toUri())).isEqualTo(ConferenceLinks.HOME)
        assertThat(ConferenceLinks.entry("indiafoss://conference/a/b/c/d/e/f/g/h/i".toUri())).isEqualTo(ConferenceLinks.HOME)
        assertThat(ConferenceLinks.entry("indiafoss://conference/${"a".repeat(2100)}".toUri())).isEqualTo(ConferenceLinks.HOME)
    }

    @Test
    fun `entry data that is not a conference route opens the home rather than being forwarded`() {
        assertThat(ConferenceLinks.entry("https://matrix.to/#/@alice:matrix.org".toUri())).isEqualTo(ConferenceLinks.HOME)
        assertThat(ConferenceLinks.entry("https://evil.example/".toUri())).isEqualTo(ConferenceLinks.HOME)
        assertThat(ConferenceLinks.entry("javascript:alert(1)".toUri())).isEqualTo(ConferenceLinks.HOME)
        assertThat(ConferenceLinks.entry("indiafoss://chat?dm=%40alice%3Amatrix.org".toUri())).isEqualTo(ConferenceLinks.HOME)
    }
}
