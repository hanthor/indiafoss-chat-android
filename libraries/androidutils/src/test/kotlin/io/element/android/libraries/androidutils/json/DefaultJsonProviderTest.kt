/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.androidutils.json

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class DefaultJsonProviderTest {
    @Test
    fun ignoreUnknownKeys_unknownFieldsIgnored() {
        val provider = DefaultJsonProvider()
        val json = provider()
        
        // This JSON has an unknown field "extraField" that the data class doesn't have
        val jsonStr = """{"name":"test","extraField":"should be ignored"}"""
        
        // Should not throw with ignoreUnknownKeys enabled
        val result = json.decodeFromString(SimpleData.serializer(), jsonStr)
        assertEquals("test", result.name)
    }

    @Test
    fun ignoreUnknownKeys_verifyDefaultBehavior() {
        // Verify that the default Json (without ignoreUnknownKeys) would throw
        val defaultJson = Json
        val jsonStr = """{"name":"test","extraField":"should fail"}"""
        
        // This should throw because the default Json doesn't ignore unknown keys
        assertFailsWith<SerializationException> {
            defaultJson.decodeFromString(SimpleData.serializer(), jsonStr)
        }
    }

    @Test
    fun allowComments_lineCommentsAccepted() {
        val provider = DefaultJsonProvider()
        val json = provider()
        
        val jsonWithComments = """{
            // This is a line comment
            "name": "test"
        }"""
        
        // Should not throw with allowComments enabled
        val result = json.decodeFromString(SimpleData.serializer(), jsonWithComments)
        assertEquals("test", result.name)
    }

    @Test
    fun allowComments_blockCommentsAccepted() {
        val provider = DefaultJsonProvider()
        val json = provider()
        
        val jsonWithBlockComments = """{
            /* This is a block comment */
            "name": "test"
            /* Another block comment */
        }"""
        
        // Should not throw with allowComments enabled
        val result = json.decodeFromString(SimpleData.serializer(), jsonWithBlockComments)
        assertEquals("test", result.name)
    }

    @Test
    fun allowTrailingComma_trailingCommaInObject() {
        val provider = DefaultJsonProvider()
        val json = provider()
        
        val jsonWithTrailingComma = """{"name": "test",}"""
        
        // Should not throw with allowTrailingComma enabled
        val result = json.decodeFromString(SimpleData.serializer(), jsonWithTrailingComma)
        assertEquals("test", result.name)
    }

    @Test
    fun allowTrailingComma_trailingCommaInArray() {
        val provider = DefaultJsonProvider()
        val json = provider()
        
        val jsonWithTrailingCommaArray = """["a","b","c",]"""
        
        // Should not throw with allowTrailingComma enabled
        val result = json.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.builtins.serializer<String>()),
            jsonWithTrailingCommaArray
        )
        assertEquals(listOf("a", "b", "c"), result)
    }

    @Test
    fun lazy_sameInstanceReturnedOnRepeatedCalls() {
        val provider = DefaultJsonProvider()
        
        val json1 = provider()
        val json2 = provider()
        
        // Should return the same instance (by lazy caching)
        assertSame(json1, json2)
    }

    @Test
    fun multipleProviders_doNotShareState() {
        val provider1 = DefaultJsonProvider()
        val provider2 = DefaultJsonProvider()
        
        val json1 = provider1()
        val json2 = provider2()
        
        // Two separate provider instances should not share the same Json instance
        assertNotSame(json1, json2)
    }

    @Test
    fun configurationCombined_allFlagsWork() {
        val provider = DefaultJsonProvider()
        val json = provider()
        
        // Complex JSON that exercises all three flags at once
        val complexJson = """{
            // Comment at start
            "name": "test",  // inline comment
            "ignored_field": "should be ignored",
            /* block comment */ "valid": true,
            "final_field": "value",
        }"""
        
        val result = json.decodeFromString(SimpleData.serializer(), complexJson)
        assertEquals("test", result.name)
    }

    @Test
    fun functionInterface_invokeWorks() {
        val provider: JsonProvider = DefaultJsonProvider()
        
        val json = provider()
        val result = json.decodeFromString(SimpleData.serializer(), """{"name":"test"}""")
        
        assertEquals("test", result.name)
    }
}

@kotlinx.serialization.Serializable
data class SimpleData(val name: String)
