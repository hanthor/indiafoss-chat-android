/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.outbox.impl.di

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.outbox.impl.OutboxDatabase
import io.element.encrypteddb.SqlCipherDriverFactory
import io.element.encrypteddb.passphrase.RandomSecretPassphraseProvider

@BindingContainer
@ContributesTo(AppScope::class)
object OutboxModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideOutboxDatabase(
        @ApplicationContext context: Context,
    ): OutboxDatabase {
        val name = "outbox_database"
        val secretFile = context.getDatabasePath("$name.key")
        val parentDir = secretFile.parentFile
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs()
        }
        val passphraseProvider = RandomSecretPassphraseProvider(context, secretFile)
        val driver = SqlCipherDriverFactory(passphraseProvider)
            .create(OutboxDatabase.Schema, "$name.db", context)
        return OutboxDatabase(driver)
    }
}
