import extension.testCommonDependencies

/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
plugins {
    id("io.element.android-compose-library")
}

android {
    namespace = "io.element.android.features.conference.impl"
}

dependencies {
    implementation(projects.appnav)
    implementation(projects.features.conference.api)
    implementation(projects.libraries.androidutils)
    implementation(projects.libraries.designsystem)
    implementation(libs.androidx.corektx)

    testCommonDependencies(libs)
}
