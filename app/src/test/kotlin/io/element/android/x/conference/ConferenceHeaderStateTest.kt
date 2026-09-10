/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.conference

import com.google.common.truth.Truth.assertThat
import org.junit.Test

// The app module has no screenshot coverage, so the header state is exercised here for the Kover states rule.
class ConferenceHeaderStateTest {
    @Test
    fun `state carries whether the native companion is installed`() {
        val installed = ConferenceHeaderState(hasNativeCompanion = true)
        val (hasNativeCompanion) = installed
        assertThat(hasNativeCompanion).isTrue()
        assertThat(installed.copy(hasNativeCompanion = false).hasNativeCompanion).isFalse()
        assertThat(installed).isEqualTo(ConferenceHeaderState(hasNativeCompanion = true))
        assertThat(installed.hashCode()).isEqualTo(ConferenceHeaderState(hasNativeCompanion = true).hashCode())
        assertThat(installed.toString()).contains("hasNativeCompanion=true")
    }

    @Test
    fun `preview provider covers both header variants`() {
        assertThat(ConferenceHeaderStateProvider().values.map { it.hasNativeCompanion }.toList())
            .containsExactly(true, false)
            .inOrder()
    }
}
