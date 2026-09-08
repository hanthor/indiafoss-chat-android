/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.androidutils.diff

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DiffCacheTest {
    @Test
    fun `MutableListDiffCache operations perform correctly`() {
        val cache = MutableListDiffCache<String>()
        assertThat(cache.isEmpty()).isTrue()
        assertThat(cache.indices).isEmpty()

        cache.add(0, "a")
        cache.add(1, "b")
        assertThat(cache.isEmpty()).isFalse()
        assertThat(cache.get(0)).isEqualTo("a")
        assertThat(cache.get(1)).isEqualTo("b")
        assertThat(cache.get(2)).isNull()
        assertThat(cache.indices).isEqualTo(0..1)

        cache[0] = "c"
        assertThat(cache.get(0)).isEqualTo("c")

        val removed = cache.removeAt(0)
        assertThat(removed).isEqualTo("c")
        assertThat(cache.get(0)).isEqualTo("b")
    }

    @Test
    fun `DefaultDiffCacheInvalidator updates cache correctly`() {
        val invalidator = DefaultDiffCacheInvalidator<String>()
        val cache = MutableListDiffCache<String>(mutableListOf("a", "b", "c"))

        invalidator.onChanged(1, 1, cache)
        assertThat(cache.get(0)).isEqualTo("a")
        assertThat(cache.get(1)).isNull()
        assertThat(cache.get(2)).isEqualTo("c")

        invalidator.onMoved(0, 2, cache)
        assertThat(cache.get(0)).isNull()
        assertThat(cache.get(1)).isEqualTo("c")
        assertThat(cache.get(2)).isEqualTo("a")

        invalidator.onInserted(1, 2, cache)
        assertThat(cache.get(0)).isNull()
        assertThat(cache.get(1)).isNull()
        assertThat(cache.get(2)).isNull()
        assertThat(cache.get(3)).isEqualTo("c")
        assertThat(cache.get(4)).isEqualTo("a")

        invalidator.onRemoved(0, 2, cache)
        assertThat(cache.get(0)).isNull()
        assertThat(cache.get(1)).isEqualTo("c")
        assertThat(cache.get(2)).isEqualTo("a")
    }

    @Test
    fun `DiffCacheUpdater applies diffs correctly`() {
        val cache = MutableListDiffCache<String>()
        val updater = DiffCacheUpdater<String, String>(
            diffCache = cache,
            detectMoves = true,
            areItemsTheSame = { oldItem, newItem -> oldItem == newItem },
        )

        val list1 = listOf("item1", "item2")
        updater.updateWith(list1)
        assertThat(cache.get(0)).isNull()
        assertThat(cache.get(1)).isNull()

        cache[0] = "cached1"
        cache[1] = "cached2"

        val list2 = listOf("item1", "item3", "item2")
        updater.updateWith(list2)
        assertThat(cache.get(0)).isEqualTo("cached1")
        assertThat(cache.get(1)).isNull()
        assertThat(cache.get(2)).isEqualTo("cached2")
    }
}
