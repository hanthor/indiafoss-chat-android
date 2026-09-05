/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.core.coroutine

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class CoroutineHelpersTest {

    @Test
    fun testParallelMap() = runTest {
        val input = listOf(1, 2, 3, 4, 5)
        val result = input.parallelMap { it * 2 }
        assertThat(result).isEqualTo(listOf(2, 4, 6, 8, 10))
    }

    @Test
    fun testParallelMapEmpty() = runTest {
        val input = emptyList<Int>()
        val result = input.parallelMap { it * 2 }
        assertThat(result).isEmpty()
    }

    @Test
    fun testParallelMapPreservesOrder() = runTest {
        val input = (1..10).toList()
        val result = input.parallelMap { it.toString() }
        assertThat(result).isEqualTo(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"))
    }

    @Test
    fun testSuspendLazy() = runTest {
        var counter = 0
        val lazyValue = suspendLazy {
            counter++
            "result_$counter"
        }

        assertThat(counter).isEqualTo(0)
        val deferred = lazyValue.value
        val result = deferred.await()
        assertThat(result).isEqualTo("result_1")
        assertThat(counter).isEqualTo(1)

        val secondCallResult = lazyValue.value.await()
        assertThat(secondCallResult).isEqualTo("result_1")
        assertThat(counter).isEqualTo(1)
    }

    @Test
    fun testDerivedStateFlowAndMapState() = runTest {
        val sourceState = MutableStateFlow(10)
        val mappedState = sourceState.mapState { "value: $it" }

        assertThat(mappedState.value).isEqualTo("value: 10")
        assertThat(mappedState.replayCache).containsExactly("value: 10")

        sourceState.value = 20
        assertThat(mappedState.value).isEqualTo("value: 20")
    }

    @Test
    fun testErrorFlow() = runTest {
        val testException = IllegalStateException("test error")
        val flow = errorFlow<Int>(testException)

        val exception = assertThrows(IllegalStateException::class.java) {
            runTest {
                flow.toList()
            }
        }
        assertThat(exception.message).isEqualTo("test error")
    }
}
