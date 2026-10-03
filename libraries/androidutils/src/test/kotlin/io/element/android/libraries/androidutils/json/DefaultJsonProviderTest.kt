/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.androidutils.json

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class DefaultJsonProviderTest {
    @Serializable
    data class TestData(val name: String, val age: Int)

    @Serializable
    data class TestDataWithOptional(val name: String, val age: Int, val email: String? = null)

    @Test
    fun `test json provider returns configured instance`() {
        val provider = DefaultJsonProvider()
        val json = provider()
        assertThat(json).isNotNull()
    }

    @Test
    fun `test json provider returns same instance on multiple calls`() {
        val provider = DefaultJsonProvider()
        val json1 = provider()
        val json2 = provider()
        assertThat(json1).isSameInstanceAs(json2)
    }

    @Test
    fun `test json configuration ignores unknown keys`() {
        val provider = DefaultJsonProvider()
        val json = provider()
        
        val jsonString = """{"name":"John","age":30,"unknownField":"value"}"""
        val decoded = json.decodeFromString<TestDataWithOptional>(jsonString)
        
        assertThat(decoded.name).isEqualTo("John")
        assertThat(decoded.age).isEqualTo(30)
        assertThat(decoded.email).isNull()
    }

    @Test
    fun `test json configuration allows comments`() {
        val provider = DefaultJsonProvider()
        val json = provider()
        
        val jsonWithComments = """
            {
                // This is a comment
                "name": "John",
                /* Block comment */ 
                "age": 30
            }
        """.trimIndent()
        
        val decoded = json.decodeFromString<TestDataWithOptional>(jsonWithComments)
        assertThat(decoded.name).isEqualTo("John")
        assertThat(decoded.age).isEqualTo(30)
    }

    @Test
    fun `test json configuration allows trailing commas`() {
        val provider = DefaultJsonProvider()
        val json = provider()
        
        val jsonWithTrailingComma = """
            {
                "name": "John",
                "age": 30,
            }
        """.trimIndent()
        
        val decoded = json.decodeFromString<TestDataWithOptional>(jsonWithTrailingComma)
        assertThat(decoded.name).isEqualTo("John")
        assertThat(decoded.age).isEqualTo(30)
    }

    @Test
    fun `test json provider handles valid compact json`() {
        val provider = DefaultJsonProvider()
        val json = provider()
        
        val jsonString = """{"name":"Alice","age":25}"""
        val decoded = json.decodeFromString<TestDataWithOptional>(jsonString)
        
        assertThat(decoded.name).isEqualTo("Alice")
        assertThat(decoded.age).isEqualTo(25)
    }

    @Test
    fun `test json provider encodes to valid json`() {
        val provider = DefaultJsonProvider()
        val json = provider()
        
        val data = TestData("Bob", 35)
        val encoded = json.encodeToString(data)
        
        val element = json.parseToJsonElement(encoded)
        val obj = element.jsonObject
        assertThat(obj["name"]?.jsonPrimitive?.content).isEqualTo("Bob")
        assertThat(obj["age"]?.jsonPrimitive?.content).isEqualTo("35")
    }

    @Test
    fun `test json provider with missing optional fields`() {
        val provider = DefaultJsonProvider()
        val json = provider()
        
        val jsonString = """{"name":"Charlie","age":40}"""
        val decoded = json.decodeFromString<TestDataWithOptional>(jsonString)
        
        assertThat(decoded.name).isEqualTo("Charlie")
        assertThat(decoded.age).isEqualTo(40)
        assertThat(decoded.email).isNull()
    }

    @Test
    fun `test json provider with explicit null optional field`() {
        val provider = DefaultJsonProvider()
        val json = provider()
        
        val jsonString = """{"name":"David","age":45,"email":null}"""
        val decoded = json.decodeFromString<TestDataWithOptional>(jsonString)
        
        assertThat(decoded.name).isEqualTo("David")
        assertThat(decoded.age).isEqualTo(45)
        assertThat(decoded.email).isNull()
    }
}
