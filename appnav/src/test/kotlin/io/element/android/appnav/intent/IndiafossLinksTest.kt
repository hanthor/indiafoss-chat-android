/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appnav.intent

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IndiafossLinksTest {
    @Test
    fun `chat dm link becomes a user permalink`() {
        assertThat(IndiafossLinks.toMatrixTo("indiafoss://chat?dm=%40alice%3Amatrix.org"))
            .isEqualTo("https://matrix.to/#/%40alice%3Amatrix.org")
    }

    @Test
    fun `chat join link becomes a room permalink for aliases and ids`() {
        assertThat(IndiafossLinks.toMatrixTo("indiafoss://chat?join=%23indiafoss-2026-session-act-1%3Amatrix.org"))
            .isEqualTo("https://matrix.to/#/%23indiafoss-2026-session-act-1%3Amatrix.org")
        assertThat(IndiafossLinks.toMatrixTo("indiafoss://chat?join=%21abc%3Ahs"))
            .isEqualTo("https://matrix.to/#/%21abc%3Ahs")
    }

    @Test
    fun `friend card uses the matrix id, else the derived neutrino address`() {
        val server = "a".repeat(64)
        assertThat(IndiafossLinks.toMatrixTo("indiafoss://friend?v=1&fn=Ada&matrix_id=%40ada%3Ax.org&neutrino_server_name=$server"))
            .isEqualTo("https://matrix.to/#/%40ada%3Ax.org")
        assertThat(IndiafossLinks.toMatrixTo("indiafoss://friend?v=1&fn=Ada&neutrino_server_name=${server.uppercase()}"))
            .isEqualTo("https://matrix.to/#/%40n%3A$server")
    }

    @Test
    fun `unsupported or malformed payloads are ignored`() {
        assertThat(IndiafossLinks.toMatrixTo("indiafoss://friend?v=2&matrix_id=%40ada%3Ax.org")).isNull()
        assertThat(IndiafossLinks.toMatrixTo("indiafoss://friend?v=1&fn=Ada")).isNull()
        assertThat(IndiafossLinks.toMatrixTo("indiafoss://chat?dm=alice")).isNull()
        assertThat(IndiafossLinks.toMatrixTo("indiafoss://location/audi-1")).isNull()
        assertThat(IndiafossLinks.toMatrixTo("https://matrix.to/#/@a:b")).isNull()
        assertThat(IndiafossLinks.toMatrixTo("")).isNull()
    }
}
