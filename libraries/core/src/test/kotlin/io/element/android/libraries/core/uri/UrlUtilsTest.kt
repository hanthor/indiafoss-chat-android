/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2022-2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.core.uri

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun `isValidUrl returns true for valid URLs`() {
        assertTrue("https://element.io".isValidUrl())
        assertTrue("http://localhost:8080/path?query=1".isValidUrl())
    }

    @Test
    fun `isValidUrl returns false for invalid URLs`() {
        assertFalse("not a url".isValidUrl())
        assertFalse("".isValidUrl())
        assertFalse("://invalid".isValidUrl())
    }

    @Test
    fun `ensureProtocol prepends https when missing`() {
        assertEquals("https://element.io", "element.io".ensureProtocol())
        assertEquals("http://element.io", "http://element.io".ensureProtocol())
        assertEquals("https://element.io", "https://element.io".ensureProtocol())
        assertEquals("", "".ensureProtocol())
    }

    @Test
    fun `ensureTrailingSlash appends slash when missing`() {
        assertEquals("https://element.io/", "https://element.io".ensureTrailingSlash())
        assertEquals("https://element.io/", "https://element.io/".ensureTrailingSlash())
        assertEquals("", "".ensureTrailingSlash())
    }
}
