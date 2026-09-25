/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.androidutils.json

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.core.extensions.runCatchingExceptions
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Test

class DefaultJsonProviderTest {
    @Serializable
    data class Sample(val name: String, val value: Int)

    // Decodes explicitly via Sample.serializer(), matching the pattern used
    // elsewhere in this codebase (e.g. WidgetMessageSerializer, MessageParser)
    // instead of the reified decodeFromString<Sample>() extension.
    private fun Json.decodeSample(input: String): Sample = decodeFromString(Sample.serializer(), input)

    @Test
    fun `ignores unknown keys instead of throwing`() {
        val json = DefaultJsonProvider().invoke()
        val decoded = json.decodeSample(
            """{"name":"a","value":1,"extra":"should be ignored"}"""
        )
        assertThat(decoded).isEqualTo(Sample("a", 1))
    }

    @Test
    fun `rejects unknown keys with a plain kotlinx-serialization default Json`() {
        // Control case: proves the previous test exercises DefaultJsonProvider's
        // configuration, not kotlinx.serialization's own default behavior.
        val plainJson = Json
        val result = plainJson.runCatchingExceptions {
            decodeSample("""{"name":"a","value":1,"extra":"x"}""")
        }
        assertThat(result.exceptionOrNull()).isInstanceOf(SerializationException::class.java)
    }

    @Test
    fun `allows line and block comments`() {
        val json = DefaultJsonProvider().invoke()
        val decoded = json.decodeSample(
            """
            {
                // a line comment
                "name": "a",
                /* a block comment */
                "value": 1
            }
            """.trimIndent()
        )
        assertThat(decoded).isEqualTo(Sample("a", 1))
    }

    @Test
    fun `allows a trailing comma in objects`() {
        val json = DefaultJsonProvider().invoke()
        val decoded = json.decodeSample(
            """{"name":"a","value":1,}"""
        )
        assertThat(decoded).isEqualTo(Sample("a", 1))
    }

    @Test
    fun `invoke returns the same lazily-created instance on repeated calls`() {
        val provider = DefaultJsonProvider()
        assertThat(provider.invoke()).isSameInstanceAs(provider.invoke())
    }

    @Test
    fun `separate provider instances do not share their Json instance`() {
        val first = DefaultJsonProvider().invoke()
        val second = DefaultJsonProvider().invoke()
        assertThat(first).isNotSameInstanceAs(second)
    }
}
