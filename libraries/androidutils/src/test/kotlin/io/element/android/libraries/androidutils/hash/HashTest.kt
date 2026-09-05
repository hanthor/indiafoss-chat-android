/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.androidutils.hash

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HashTest {
    @Test
    fun `test string hashing produces sha512 output`() {
        val input = "hello world"
        val hash = input.hash()
        assertThat(hash).isNotNull()
        assertThat(hash.length).isEqualTo(128)
        assertThat(hash).isEqualTo("b10a8db164e0754105b7a99be72e3fe5daf3300c1b23747e2ee47365d6cebe092424e97686345b2f326396a580c4c32f4c8d85f6623976370e443988cb7e42d7")
    }

    @Test
    fun `test same input produces identical hash`() {
        val input = "test-string-123"
        assertThat(input.hash()).isEqualTo(input.hash())
    }

    @Test
    fun `test empty string hash`() {
        val hash = "".hash()
        assertThat(hash).isNotNull()
        assertThat(hash.length).isEqualTo(128)
    }
}
