/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.invite.impl

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import io.element.android.features.invite.api.KnownContactsStore
import io.element.android.libraries.androidutils.file.safeDelete
import io.element.android.libraries.androidutils.hash.hash
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.sessionstorage.api.observer.SessionListener
import io.element.android.libraries.sessionstorage.api.observer.SessionObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val knownContactsKey = stringSetPreferencesKey("knownContacts")

class DefaultKnownContactsStore(
    context: Context,
    sessionId: SessionId,
    sessionCoroutineScope: CoroutineScope,
    sessionObserver: SessionObserver,
) : KnownContactsStore {
    init {
        sessionObserver.addListener(object : SessionListener {
            override suspend fun onSessionDeleted(userId: String, wasLastSession: Boolean) {
                if (sessionId.value == userId) {
                    clear()
                }
            }
        })
    }

    private val dataStoreFile = sessionId.value.hash().take(16).let { hashedUserId ->
        context.preferencesDataStoreFile("session_${hashedUserId}_known-contacts")
    }

    private val store = PreferenceDataStoreFactory.create(
        scope = sessionCoroutineScope,
        migrations = emptyList(),
    ) {
        dataStoreFile
    }

    override fun knownUserIds(): Flow<Set<UserId>> =
        store.data.map { prefs ->
            prefs[knownContactsKey]
                .orEmpty()
                .map { UserId(it) }
                .toSet()
        }

    override suspend fun markKnown(userId: UserId) {
        store.edit { prefs ->
            prefs[knownContactsKey] = prefs[knownContactsKey].orEmpty() + userId.value
        }
    }

    override suspend fun clear() {
        dataStoreFile.safeDelete()
    }
}
