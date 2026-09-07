/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MeshContactTest {
    private val node = "845aa456078572639c1543694de69e0a03fb883bd9c1dab1a2f6df811b75897e"

    @Test
    fun `a raw mesh mxid parses to itself`() {
        assertThat(parseMeshContactUserId("@n:$node")).isEqualTo(UserId("@n:$node"))
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        assertThat(parseMeshContactUserId("  @n:$node\n")).isEqualTo(UserId("@n:$node"))
    }

    @Test
    fun `a non-mesh mxid is not a mesh contact`() {
        assertThat(parseMeshContactUserId("@alice:example.org")).isNull()
    }

    @Test
    fun `the companion vCard mesh line parses to a mesh mxid`() {
        val vcard = buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            appendLine("FN:rueh")
            appendLine("X-INDIAFOSS-MESH:$node")
            appendLine("X-MATRIX-ID:@alice:matrix.org")
            appendLine("END:VCARD")
        }
        // The mesh line wins; the localpart is always "n".
        assertThat(parseMeshContactUserId(vcard)).isEqualTo(UserId("@n:$node"))
    }

    @Test
    fun `the property name is case-insensitive and tolerates vCard params`() {
        assertThat(parseMeshContactUserId("x-indiafoss-mesh;TYPE=work:$node"))
            .isEqualTo(UserId("@n:$node"))
    }

    @Test
    fun `the older X-NEUTRINO-SERVER-NAME spelling is still accepted`() {
        assertThat(parseMeshContactUserId("X-NEUTRINO-SERVER-NAME:$node"))
            .isEqualTo(UserId("@n:$node"))
    }

    @Test
    fun `an uppercase node id is normalised to lowercase`() {
        assertThat(parseMeshContactUserId("X-INDIAFOSS-MESH:${node.uppercase()}"))
            .isEqualTo(UserId("@n:$node"))
    }

    @Test
    fun `a mesh line whose value is not a 64-hex node id is rejected`() {
        assertThat(parseMeshContactUserId("X-INDIAFOSS-MESH:not-a-node-id")).isNull()
        assertThat(parseMeshContactUserId("X-INDIAFOSS-MESH:${node.dropLast(1)}")).isNull()
    }

    @Test
    fun `plain text with no mesh identity is null`() {
        assertThat(parseMeshContactUserId("hello there")).isNull()
        assertThat(parseMeshContactUserId("")).isNull()
        assertThat(parseMeshContactUserId("https://example.org/@n:$node")).isNull()
    }
}
