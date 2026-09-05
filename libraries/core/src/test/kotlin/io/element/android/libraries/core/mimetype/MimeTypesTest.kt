/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2023-2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.core.mimetype

import io.element.android.libraries.core.mimetype.MimeTypes.isMimeTypeAny
import io.element.android.libraries.core.mimetype.MimeTypes.isMimeTypeAnimatedImage
import io.element.android.libraries.core.mimetype.MimeTypes.isMimeTypeApplication
import io.element.android.libraries.core.mimetype.MimeTypes.isMimeTypeAudio
import io.element.android.libraries.core.mimetype.MimeTypes.isMimeTypeFile
import io.element.android.libraries.core.mimetype.MimeTypes.isMimeTypeImage
import io.element.android.libraries.core.mimetype.MimeTypes.isMimeTypeText
import io.element.android.libraries.core.mimetype.MimeTypes.isMimeTypeVideo
import io.element.android.libraries.core.mimetype.MimeTypes.normalizeMimeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MimeTypesTest {

    @Test
    fun `normalizeMimeType converts bad jpg to jpeg`() {
        assertEquals(MimeTypes.Jpeg, MimeTypes.BadJpg.normalizeMimeType())
        assertEquals(MimeTypes.Png, MimeTypes.Png.normalizeMimeType())
        assertNull(null.normalizeMimeType())
    }

    @Test
    fun `isMimeTypeImage returns true for image types`() {
        assertTrue(MimeTypes.Png.isMimeTypeImage())
        assertTrue(MimeTypes.Jpeg.isMimeTypeImage())
        assertTrue(MimeTypes.Images.isMimeTypeImage())
        assertFalse(MimeTypes.Mp4.isMimeTypeImage())
        assertFalse(null.isMimeTypeImage())
    }

    @Test
    fun `isMimeTypeAnimatedImage returns true only for gif and webp`() {
        assertTrue(MimeTypes.Gif.isMimeTypeAnimatedImage())
        assertTrue(MimeTypes.WebP.isMimeTypeAnimatedImage())
        assertFalse(MimeTypes.Png.isMimeTypeAnimatedImage())
        assertFalse(MimeTypes.Jpeg.isMimeTypeAnimatedImage())
        assertFalse(null.isMimeTypeAnimatedImage())
    }

    @Test
    fun `isMimeTypeVideo returns true for video types`() {
        assertTrue(MimeTypes.Mp4.isMimeTypeVideo())
        assertTrue(MimeTypes.Videos.isMimeTypeVideo())
        assertFalse(MimeTypes.Png.isMimeTypeVideo())
        assertFalse(null.isMimeTypeVideo())
    }

    @Test
    fun `isMimeTypeAudio returns true for audio types`() {
        assertTrue(MimeTypes.Mp3.isMimeTypeAudio())
        assertTrue(MimeTypes.Ogg.isMimeTypeAudio())
        assertTrue(MimeTypes.Audio.isMimeTypeAudio())
        assertFalse(MimeTypes.Mp4.isMimeTypeAudio())
        assertFalse(null.isMimeTypeAudio())
    }

    @Test
    fun `isMimeTypeApplication returns true for application types`() {
        assertTrue(MimeTypes.Apk.isMimeTypeApplication())
        assertTrue(MimeTypes.Pdf.isMimeTypeApplication())
        assertTrue(MimeTypes.Json.isMimeTypeApplication())
        assertFalse(MimeTypes.Png.isMimeTypeApplication())
        assertFalse(null.isMimeTypeApplication())
    }

    @Test
    fun `isMimeTypeFile returns true for file prefix`() {
        assertTrue("file/something".isMimeTypeFile())
        assertFalse(MimeTypes.Png.isMimeTypeFile())
        assertFalse(null.isMimeTypeFile())
    }

    @Test
    fun `isMimeTypeText returns true for text types`() {
        assertTrue(MimeTypes.PlainText.isMimeTypeText())
        assertTrue("text/html".isMimeTypeText())
        assertFalse(MimeTypes.Png.isMimeTypeText())
        assertFalse(null.isMimeTypeText())
    }

    @Test
    fun `isMimeTypeAny returns true for wildcards`() {
        assertTrue(MimeTypes.Any.isMimeTypeAny())
        assertFalse(MimeTypes.Png.isMimeTypeAny())
        assertFalse(null.isMimeTypeAny())
    }

    @Test
    fun `fromFileExtension returns correct mime type`() {
        assertEquals(MimeTypes.Apk, MimeTypes.fromFileExtension("apk"))
        assertEquals(MimeTypes.Apk, MimeTypes.fromFileExtension("APK"))
        assertEquals(MimeTypes.Pdf, MimeTypes.fromFileExtension("pdf"))
        assertEquals(MimeTypes.OctetStream, MimeTypes.fromFileExtension("txt"))
        assertEquals(MimeTypes.OctetStream, MimeTypes.fromFileExtension(""))
    }

    @Test
    fun `hasSubtype checks valid mime type format`() {
        assertTrue(MimeTypes.hasSubtype("image/png"))
        assertTrue(MimeTypes.hasSubtype("application/json"))
        assertFalse(MimeTypes.hasSubtype("image/*"))
        assertFalse(MimeTypes.hasSubtype("invalid"))
        assertFalse(MimeTypes.hasSubtype("image/"))
        assertFalse(MimeTypes.hasSubtype("image/   "))
        assertFalse(MimeTypes.hasSubtype("a/b/c"))
    }
}
