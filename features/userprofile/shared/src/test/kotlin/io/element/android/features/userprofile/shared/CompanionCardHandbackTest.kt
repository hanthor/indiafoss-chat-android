/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.userprofile.shared

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.UserId
import org.junit.Test

class CompanionCardHandbackTest {
    private val nodeId = "a".repeat(64)

    @Test
    fun `a mesh session hands over its node id as the mesh parameter`() {
        assertThat(CompanionCardHandback.uriFor(UserId("@n:$nodeId")))
            .isEqualTo("indiafoss://conference/connect?mesh=$nodeId")
    }

    @Test
    fun `an internet account hands over its Matrix id, encoded`() {
        assertThat(CompanionCardHandback.uriFor(UserId("@alice:matrix.org")))
            .isEqualTo("indiafoss://conference/connect?matrix=%40alice%3Amatrix.org")
    }
}
