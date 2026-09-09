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
        assertThat(hash).isEqualTo(
            "309ecc489c12d6eb4cc40f50c902f2b4d0ed77ee511a7c7a9bcd3ca86d4cd86f" +
                "989dd35bc5ff499670da34255b45b0cfd830e81f605dcf7dc5542e93ae9cd76f"
        )
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
        assertThat(hash).isEqualTo(
            "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce" +
                "47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e"
        )
    }
}
