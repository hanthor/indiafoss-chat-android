/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.androidutils.file

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileUtilsTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `safeDelete removes existing file`() {
        val file = temporaryFolder.newFile("test_delete.txt")
        assertThat(file.exists()).isTrue()

        file.safeDelete()

        assertThat(file.exists()).isFalse()
    }

    @Test
    fun `safeDelete on non-existent file handles gracefully`() {
        val file = File(temporaryFolder.root, "non_existent.txt")
        assertThat(file.exists()).isFalse()

        file.safeDelete()

        assertThat(file.exists()).isFalse()
    }

    @Test
    fun `safeRenameTo renames existing file`() {
        val file = temporaryFolder.newFile("src.txt")
        file.writeText("hello world")
        val dest = File(temporaryFolder.root, "dest.txt")

        file.safeRenameTo(dest)

        assertThat(file.exists()).isFalse()
        assertThat(dest.exists()).isTrue()
        assertThat(dest.readText()).isEqualTo("hello world")
    }

    @Test
    fun `getSizeOfFiles calculates total size recursively`() {
        val dir = temporaryFolder.newFolder("subfolder")
        val file1 = File(dir, "f1.txt").apply { writeText("12345") } // 5 bytes
        val file2 = File(dir, "f2.txt").apply { writeText("123") }   // 3 bytes

        val totalSize = dir.getSizeOfFiles()

        assertThat(totalSize).isEqualTo(dir.length() + file1.length() + file2.length())
    }

    @Test
    fun `compressFile compresses given file with gzip`() {
        val file = temporaryFolder.newFile("sample.txt")
        file.writeText("Repeating text pattern ".repeat(50))

        val compressedFile = compressFile(file)

        assertThat(compressedFile).isNotNull()
        assertThat(compressedFile!!.exists()).isTrue()
        assertThat(compressedFile.name).isEqualTo("sample.txt.gz")
        assertThat(compressedFile.length()).isLessThan(file.length())

        // Cleanup
        compressedFile.safeDelete()
    }
}
