/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.startchat.impl.scanqr

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import io.element.android.features.invite.test.InMemoryKnownContactsStore
import io.element.android.features.invitepeople.test.FakeStartDMAction
import io.element.android.features.startchat.impl.FakeStartChatNavigator
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.permalink.PermalinkData
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.permalink.FakePermalinkParser
import io.element.android.libraries.permissions.api.aPermissionsState
import io.element.android.libraries.permissions.test.FakePermissionsPresenter
import io.element.android.libraries.permissions.test.FakePermissionsPresenterFactory
import io.element.android.tests.testutils.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ScanQrPresenterTest {
    private val node = "a".repeat(64)
    private val meshUser = UserId("@n:$node")

    @Test
    fun `present - a recognised code is previewed and nothing is contacted until confirmed`() = runTest {
        var executed = 0
        val startDMAction = FakeStartDMAction { _, _, state ->
            executed++
            state.value = AsyncAction.Success(A_ROOM_ID)
        }
        val knownContacts = InMemoryKnownContactsStore()
        val presenter = createPresenter(startDMAction = startDMAction, knownContactsStore = knownContacts)
        presenter.test {
            val initial = awaitItem()
            assertThat(initial.isScanning).isTrue()
            initial.eventSink(ScanQrEvents.QrCodeScanned("indiafoss://friend?v=1&neutrino_server_name=$node".toByteArray()))
            val previewed = awaitItem()
            assertThat(previewed.scanResult).isEqualTo(ScanResult.Recognized(meshUser))
            assertThat(previewed.isScanning).isFalse()
            assertThat(executed).isEqualTo(0)
            assertThat(knownContacts.knownUserIds().first()).isEmpty()

            previewed.eventSink(ScanQrEvents.StartChat)
            val started = awaitItem()
            assertThat(started.startDmAction).isEqualTo(AsyncAction.Success(A_ROOM_ID))
            assertThat(executed).isEqualTo(1)
            assertThat(knownContacts.knownUserIds().first()).containsExactly(meshUser)
        }
    }

    @Test
    fun `present - an unreadable code says so and scan again resumes`() = runTest {
        val presenter = createPresenter()
        presenter.test {
            val initial = awaitItem()
            initial.eventSink(ScanQrEvents.QrCodeScanned("hello".toByteArray()))
            val rejected = awaitItem()
            assertThat(rejected.scanResult).isEqualTo(ScanResult.NotRecognized)
            assertThat(rejected.isScanning).isFalse()
            rejected.eventSink(ScanQrEvents.TryAgain)
            val resumed = awaitItem()
            assertThat(resumed.scanResult).isEqualTo(ScanResult.Scanning)
            assertThat(resumed.isScanning).isTrue()
        }
    }

    @Test
    fun `present - a matrix permalink is read by the permalink parser first`() = runTest {
        val parser = FakePermalinkParser { PermalinkData.UserLink(UserId("@alice:example.org")) }
        val presenter = createPresenter(permalinkParser = parser)
        presenter.test {
            awaitItem().eventSink(ScanQrEvents.QrCodeScanned("https://matrix.to/#/@alice:example.org".toByteArray()))
            assertThat(awaitItem().scanResult).isEqualTo(ScanResult.Recognized(UserId("@alice:example.org")))
        }
    }

    private fun createPresenter(
        startDMAction: FakeStartDMAction = FakeStartDMAction(),
        knownContactsStore: InMemoryKnownContactsStore = InMemoryKnownContactsStore(),
        permalinkParser: FakePermalinkParser = FakePermalinkParser { PermalinkData.FallbackLink(Uri.EMPTY) },
    ) = ScanQrPresenter(
        navigator = FakeStartChatNavigator(),
        permalinkParser = permalinkParser,
        startDMAction = startDMAction,
        knownContactsStore = knownContactsStore,
        permissionsPresenterFactory = FakePermissionsPresenterFactory(
            FakePermissionsPresenter(aPermissionsState(showDialog = false, permissionGranted = true)),
        ),
    )
}
