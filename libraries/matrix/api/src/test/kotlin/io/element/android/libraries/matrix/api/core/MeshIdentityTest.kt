/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MeshIdentityTest {
    private val meshNode = "845aa456078572639c1543694de69e0a03fb883bd9c1dab1a2f6df811b75897e"

    @Test
    fun `a 64-hex server-name is a mesh identity`() {
        val userId = UserId("@n:$meshNode")
        assertThat(userId.isMeshUser).isTrue()
        assertThat(userId.meshNodeId).isEqualTo(meshNode)
    }

    @Test
    fun `the localpart is ignored when detecting a mesh identity`() {
        // The server-name shape decides, not the localpart.
        assertThat(UserId("@alice:$meshNode").isMeshUser).isTrue()
    }

    @Test
    fun `a normal homeserver id is not a mesh identity`() {
        val userId = UserId("@alice:example.org")
        assertThat(userId.isMeshUser).isFalse()
        assertThat(userId.meshNodeId).isNull()
        assertThat(userId.meshShortCode).isNull()
    }

    @Test
    fun `a domain that merely looks hex-ish but is the wrong length is not a mesh identity`() {
        assertThat(UserId("@n:deadbeef").isMeshUser).isFalse()
        // 63 chars.
        assertThat(UserId("@n:${meshNode.dropLast(1)}").isMeshUser).isFalse()
        // Uppercase hex is not how node ids are encoded.
        assertThat(UserId("@n:${meshNode.uppercase()}").isMeshUser).isFalse()
    }

    @Test
    fun `the short code is the last 8 hex, upper-cased and grouped`() {
        // …811b75897e -> last 8 = 1b75897e -> 1B75·897E
        assertThat(UserId("@n:$meshNode").meshShortCode).isEqualTo("1B75·897E")
    }
}
