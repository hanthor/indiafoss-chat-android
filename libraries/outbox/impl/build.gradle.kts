import extension.setupDependencyInjection
import extension.testCommonDependencies

/*
 * Copyright 2026 IndiaFOSS Companion contributors
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
plugins {
    id("io.element.android-library")
    alias(libs.plugins.sqldelight)
}

android {
    namespace = "io.element.android.libraries.outbox.impl"
}

setupDependencyInjection()

dependencies {
    api(projects.libraries.outbox.api)
    implementation(projects.libraries.core)
    implementation(projects.libraries.di)
    implementation(projects.libraries.encryptedDb)
    implementation(projects.libraries.matrix.api)
    implementation(projects.services.toolbox.api)
    implementation(libs.coroutines.core)
    implementation(libs.sqldelight.driver.android)
    implementation(libs.sqldelight.coroutines)
    implementation(libs.sqlcipher)
    implementation(libs.sqlite)

    testCommonDependencies(libs)
    testImplementation(projects.libraries.matrix.test)
    testImplementation(projects.libraries.outbox.test)
    testImplementation(projects.services.toolbox.test)
    testImplementation(libs.sqldelight.driver.jvm)
}

sqldelight {
    databases {
        create("OutboxDatabase") {
            packageName.set("io.element.android.libraries.outbox.impl")
        }
    }
}
