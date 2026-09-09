/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.core.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class FilterUpToTest {
    @Test
    fun `filterUpTo returns up to count matching elements`() {
        val list = listOf(1, 2, 3, 4, 5, 6, 7, 8)
        val evens = list.filterUpTo(3) { it % 2 == 0 }
        assertThat(evens).containsExactly(2, 4, 6).inOrder()
    }

    @Test
    fun `filterUpTo returns all matching elements if fewer than count`() {
        val list = listOf(1, 2, 3, 4)
        val evens = list.filterUpTo(5) { it % 2 == 0 }
        assertThat(evens).containsExactly(2, 4).inOrder()
    }

    @Test
    fun `filterUpTo returns empty list when no matches found`() {
        val list = listOf(1, 3, 5)
        val evens = list.filterUpTo(3) { it % 2 == 0 }
        assertThat(evens).isEmpty()
    }
}

class TryTest {
    @Test
    fun `tryOrNull returns value on success`() {
        val result = tryOrNull { "success" }
        assertThat(result).isEqualTo("success")
    }

    @Test
    fun `tryOrNull returns null on exception and invokes callback`() {
        var caught: Exception? = null
        val result = tryOrNull(onException = { caught = it }) {
            throw IllegalStateException("something went wrong")
        }
        assertThat(result).isNull()
        assertThat(caught).isInstanceOf(IllegalStateException::class.java)
        assertThat(caught?.message).isEqualTo("something went wrong")
    }

    @Test(expected = CancellationException::class)
    fun `tryOrNull rethrows CancellationException`() {
        tryOrNull {
            throw CancellationException("cancelled")
        }
    }
}
