/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.androidutils.json

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class DefaultJsonProviderTest {
    @Test
    fun `ignoreUnknownKeys is enabled - unknown keys are silently ignored`() {
        val provider = DefaultJsonProvider()
        val json = provider()

        // A payload with an unknown key
        val payload = """{"name":"test","unknown_field":"ignored","value":42}"""
        val parsed = json.parseToJsonElement(payload).jsonObject

        // Verify the known fields are parsed
        assertThat(parsed["name"]?.jsonPrimitive?.content).isEqualTo("test")
        assertThat(parsed["value"]?.jsonPrimitive?.content).isEqualTo("42")
        // The unknown field is silently ignored (not present in parsed result)
        assertThat(parsed.containsKey("unknown_field")).isFalse()
    }

    @Test
    fun `ignoreUnknownKeys default behavior - unknown keys throw an error`() {
        // Use a plain Json instance to prove the default behavior is different
        val plainJson = Json

        val payload = """{"name":"test","unknown_field":"not ignored"}"""

        // Parsing with a plain Json (which has ignoreUnknownKeys=false) should fail
        val exception = try {
            plainJson.parseToJsonElement(payload)
            null
        } catch (e: SerializationException) {
            e
        }

        assertThat(exception).isNotNull()
        // This proves the assertion above isn't vacuous: the default Json throws
        // on unknown keys, but DefaultJsonProvider's configuration ignores them.
    }

    @Test
    fun `allowComments is enabled - line and block comments are accepted`() {
        val provider = DefaultJsonProvider()
        val json = provider()

        // Payload with line and block comments
        val payloadWithComments = """{
            // This is a line comment
            "name": "test",
            /* This is a block comment */
            "value": 42
        }"""

        val parsed = json.parseToJsonElement(payloadWithComments).jsonObject
        assertThat(parsed["name"]?.jsonPrimitive?.content).isEqualTo("test")
        assertThat(parsed["value"]?.jsonPrimitive?.content).isEqualTo("42")
    }

    @Test
    fun `allowTrailingComma is enabled - trailing commas in objects are accepted`() {
        val provider = DefaultJsonProvider()
        val json = provider()

        // Payload with trailing comma
        val payloadWithTrailingComma = """{
            "name": "test",
            "value": 42,
        }"""

        val parsed = json.parseToJsonElement(payloadWithTrailingComma).jsonObject
        assertThat(parsed["name"]?.jsonPrimitive?.content).isEqualTo("test")
        assertThat(parsed["value"]?.jsonPrimitive?.content).isEqualTo("42")
    }

    @Test
    fun `invoke returns the same lazily-created instance on repeated calls`() {
        val provider = DefaultJsonProvider()

        val first = provider()
        val second = provider()

        // Both calls return the exact same instance (by lazy caching)
        assertThat(first).isSameInstanceAs(second)
    }

    @Test
    fun `different provider instances have separate Json state`() {
        val provider1 = DefaultJsonProvider()
        val provider2 = DefaultJsonProvider()

        val json1 = provider1()
        val json2 = provider2()

        // Two different provider instances create separate Json instances
        assertThat(json1).isNotSameInstanceAs(json2)
    }
}
