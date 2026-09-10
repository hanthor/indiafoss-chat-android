/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

plugins {
    id("io.element.android-library")
}

android {
    namespace = "io.element.android.libraries.outbox.test"
}

dependencies {
    api(projects.libraries.outbox.api)
    implementation(projects.libraries.matrix.api)
    implementation(projects.tests.testutils)
    implementation(libs.coroutines.core)
}
