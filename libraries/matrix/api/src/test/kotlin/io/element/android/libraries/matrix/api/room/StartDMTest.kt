/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.room

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.UserId
import org.junit.Test

class StartDMTest {
    private val meshUser = UserId("@n:" + "a".repeat(64))
    private val otherMeshUser = UserId("@n:" + "b".repeat(64))
    private val internetUser = UserId("@alice:conf.example")

    @Test
    fun `mesh to mesh can be keyed, so it is not blocked`() {
        assertThat(dmWouldBeKeyDead(meshUser, otherMeshUser)).isFalse()
    }

    @Test
    fun `mesh to internet cannot be keyed and is blocked`() {
        // The room event would cross via a gateway's copy; the Megolm key share
        // is a to-device EDU with no relay, so the invitee would be stuck on
        // "waiting for the key" forever. Companion issue #176.
        assertThat(dmWouldBeKeyDead(meshUser, internetUser)).isTrue()
    }

    @Test
    fun `internet sessions are never blocked, in either direction`() {
        // Only the mesh side lacks routes; an internet session reaching a mesh
        // user's key endpoint may still work through whatever brought them a
        // card, and blocking it here would be guessing.
        assertThat(dmWouldBeKeyDead(internetUser, meshUser)).isFalse()
        assertThat(dmWouldBeKeyDead(internetUser, UserId("@bob:matrix.org"))).isFalse()
    }

    @Test
    fun `an uppercase or short hex name is not a mesh name`() {
        assertThat(dmWouldBeKeyDead(UserId("@n:" + "A".repeat(64)), internetUser)).isFalse()
        assertThat(dmWouldBeKeyDead(UserId("@n:" + "a".repeat(63)), internetUser)).isFalse()
    }
}
