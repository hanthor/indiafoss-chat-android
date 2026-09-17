/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.auth

import com.google.common.truth.Truth.assertThat
import io.element.android.features.enterprise.api.EnterpriseService
import io.element.android.features.enterprise.test.FakeEnterpriseService
import io.element.android.libraries.matrix.impl.ClientBuilderProvider
import io.element.android.libraries.matrix.impl.FakeClientBuilderProvider
import io.element.android.libraries.matrix.impl.createRustMatrixClientFactory
import io.element.android.libraries.matrix.impl.fixtures.fakes.FakeFfiClient
import io.element.android.libraries.matrix.impl.fixtures.fakes.FakeFfiClientBuilder
import io.element.android.libraries.matrix.impl.fixtures.fakes.FakeFfiHomeserverLoginDetails
import io.element.android.libraries.matrix.impl.paths.SessionPathsFactory
import io.element.android.libraries.matrix.test.auth.FakeOAuthRedirectUrlProvider
import io.element.android.libraries.matrix.test.core.aBuildMeta
import io.element.android.libraries.sessionstorage.api.SessionStore
import io.element.android.libraries.sessionstorage.test.InMemorySessionStore
import io.element.android.libraries.workmanager.test.FakeWorkManagerScheduler
import io.element.android.tests.testutils.testCoroutineDispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RustMatrixAuthenticationServiceTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `setHomeserver is successful`() = runTest {
        val sut = createRustMatrixAuthenticationService(
            clientBuilderProvider = FakeClientBuilderProvider(
                provideResult = {
                    FakeFfiClientBuilder(
                        buildResult = {
                            FakeFfiClient(
                                homeserverLoginDetailsResult = {
                                    FakeFfiHomeserverLoginDetails()
                                }
                            )
                        }
                    )
                }
            ),
        )
        assertThat(sut.setHomeserver("matrix.org").isSuccess).isTrue()
    }

    @Test
    fun `setHomeserver after a login keeps the stored session's directories`() = runTest {
        val sessionStore = InMemorySessionStore(updateUserProfileResult = { _, _, _ -> })
        val baseDirectory = temporaryFolder.newFolder("sessions")
        val cacheDirectory = temporaryFolder.newFolder("cache")
        val sut = createRustMatrixAuthenticationService(
            sessionStore = sessionStore,
            clientBuilderProvider = aLoginCapableClientBuilderProvider(),
            baseDirectory = baseDirectory,
            cacheDirectory = cacheDirectory,
        )
        assertThat(sut.setHomeserver("matrix.org").isSuccess).isTrue()
        val sessionId = sut.login("alice", "password").getOrThrow()
        val sessionData = sessionStore.getSession(sessionId.value)!!
        // The SDK would have created these; stand in for it so deletion is observable.
        val cryptoStore = File(sessionData.sessionPath, "matrix-sdk-crypto.sqlite3")
        cryptoStore.parentFile!!.mkdirs()
        cryptoStore.writeText("keys")
        val cache = File(sessionData.cachePath, "state.sqlite3")
        cache.parentFile!!.mkdirs()
        cache.writeText("cache")

        // A second attempt (adding an account, or the embedded-homeserver auto-login firing
        // again) must rotate onto a fresh directory, not over the live one.
        assertThat(sut.setHomeserver("matrix.org").isSuccess).isTrue()

        assertThat(cryptoStore.exists()).isTrue()
        assertThat(cache.exists()).isTrue()
    }

    private fun aLoginCapableClientBuilderProvider() = FakeClientBuilderProvider(
        provideResult = {
            FakeFfiClientBuilder(
                buildResult = {
                    FakeFfiClient(
                        homeserverLoginDetailsResult = { FakeFfiHomeserverLoginDetails() },
                        loginResult = { _, _ -> },
                        withUtdHook = {},
                    )
                }
            )
        }
    )

    private fun TestScope.createRustMatrixAuthenticationService(
        sessionStore: SessionStore = InMemorySessionStore(),
        clientBuilderProvider: ClientBuilderProvider = FakeClientBuilderProvider(),
        enterpriseService: EnterpriseService = FakeEnterpriseService(),
        baseDirectory: File = File("/base"),
        cacheDirectory: File = File("/cache"),
    ): RustMatrixAuthenticationService {
        val rustMatrixClientFactory = createRustMatrixClientFactory(
            cacheDirectory = cacheDirectory,
            sessionStore = sessionStore,
            clientBuilderProvider = clientBuilderProvider,
            workManagerScheduler = FakeWorkManagerScheduler(submitLambda = {}),
        )
        return RustMatrixAuthenticationService(
            sessionPathsFactory = SessionPathsFactory(baseDirectory, cacheDirectory),
            coroutineDispatchers = testCoroutineDispatchers(),
            sessionStore = sessionStore,
            rustMatrixClientFactory = rustMatrixClientFactory,
            passphraseGenerator = FakePassphraseGenerator(),
            oAuthConfigurationProvider = OAuthConfigurationProvider(
                buildMeta = aBuildMeta(),
                oAuthRedirectUrlProvider = FakeOAuthRedirectUrlProvider(),
            ),
            enterpriseService = enterpriseService,
        )
    }
}
