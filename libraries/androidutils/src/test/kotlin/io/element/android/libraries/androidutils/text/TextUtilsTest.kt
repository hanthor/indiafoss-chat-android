/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.androidutils.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TextUtilsTest {
    @Test
    fun `test urlEncoded and urlDecoded roundtrip`() {
        val original = "hello world & special characters = + ?"
        val encoded = original.urlEncoded()
        assertThat(encoded).isNotEqualTo(original)
        val decoded = encoded.urlDecoded()
        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `test urlEncoded encoding format`() {
        val input = "foo bar"
        assertThat(input.urlEncoded()).isEqualTo("foo+bar")
    }

    @Test
    fun `test urlDecoded decoding format`() {
        val input = "foo%20bar"
        assertThat(input.urlDecoded()).isEqualTo("foo bar")
    }
}
