/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.preferences.api.store

import kotlinx.coroutines.flow.Flow

interface SessionPreferencesStore {
    suspend fun setSharePresence(enabled: Boolean)
    fun isSharePresenceEnabled(): Flow<Boolean>

    suspend fun setSendPublicReadReceipts(enabled: Boolean)
    fun isSendPublicReadReceiptsEnabled(): Flow<Boolean>

    suspend fun setRenderReadReceipts(enabled: Boolean)
    fun isRenderReadReceiptsEnabled(): Flow<Boolean>

    suspend fun setSendTypingNotifications(enabled: Boolean)
    fun isSendTypingNotificationsEnabled(): Flow<Boolean>

    suspend fun setRenderTypingNotifications(enabled: Boolean)
    fun isRenderTypingNotificationsEnabled(): Flow<Boolean>

    suspend fun setSkipSessionVerification(skip: Boolean)
    fun isSessionVerificationSkipped(): Flow<Boolean>

    suspend fun setOptimizeImages(compress: Boolean)
    fun doesOptimizeImages(): Flow<Boolean>

    suspend fun setVideoCompressionPreset(preset: VideoCompressionPreset)
    fun getVideoCompressionPreset(): Flow<VideoCompressionPreset>

    /**
     * Whether the one-time "set your display name" prompt shown at first launch
     * has been completed, so it isn't shown again. The name itself lives on the
     * (embedded) homeserver via the profile API; this is only the "seen it" flag.
     */
    suspend fun setDisplayNamePromptCompleted(completed: Boolean)
    fun isDisplayNamePromptCompleted(): Flow<Boolean>

    /**
     * Whether the local user is discoverable by nearby peers, i.e. whether the
     * embedded node advertises this user over the BLE mesh. Defaults to `true`
     * (discoverable); when `false` the node should stop advertising.
     */
    suspend fun setDiscoverable(discoverable: Boolean)
    fun isDiscoverable(): Flow<Boolean>

    /**
     * Whether the one-time "let people near you find you?" prompt shown at first
     * launch has been answered, so it isn't shown again.
     */
    suspend fun setDiscoveryPromptCompleted(completed: Boolean)
    fun isDiscoveryPromptCompleted(): Flow<Boolean>

    suspend fun clear()
}
