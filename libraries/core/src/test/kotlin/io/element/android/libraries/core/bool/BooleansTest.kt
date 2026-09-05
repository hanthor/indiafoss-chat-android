/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2022-2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.core.bool

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BooleansTest {

    @Test
    fun `orTrue returns boolean value or true if null`() {
        val trueBool: Boolean? = true
        val falseBool: Boolean? = false
        val nullBool: Boolean? = null

        assertTrue(trueBool.orTrue())
        assertFalse(falseBool.orTrue())
        assertTrue(nullBool.orTrue())
    }

    @Test
    fun `orFalse returns boolean value or false if null`() {
        val trueBool: Boolean? = true
        val falseBool: Boolean? = false
        val nullBool: Boolean? = null

        assertTrue(trueBool.orFalse())
        assertFalse(falseBool.orFalse())
        assertFalse(nullBool.orFalse())
    }
}
