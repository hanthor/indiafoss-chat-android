import extension.setupDependencyInjection
import extension.testCommonDependencies
import java.net.URI
import java.security.MessageDigest

/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

plugins {
    id("io.element.android-library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.element.android.services.neutrino.impl"
}

setupDependencyInjection()

// The Neutrino bindings used to come from io.element.neutrino:bindings on GitHub
// Packages, which returns 401 for anonymous requests even though the source is
// public — every contributor and CI run needed a personal access token with
// read:packages for element-hq (issue #3). We consume the same .aar the
// IndiaFOSS Companion project already builds from source and publishes
// anonymously for the identical reason (see its neutrino-bindings.yml): a
// local file dependency needs no repository credentials at all. This build is
// also compiled against hanthor/neutrino, our fork with the E2EE and CORS
// patches, rather than plain upstream — see version.json in that repo for the
// exact pinned rev.
val neutrinoVersion = libs.versions.neutrino.get()
val neutrinoSha256 = "e58da42ff8c46153796e9fca44c518ca58d92263289405cf3c1673cc45cb313a"
val neutrinoAarName = "neutrino-bindings-$neutrinoVersion.aar"
val neutrinoLibsDir = layout.projectDirectory.dir("libs")
val neutrinoAar = neutrinoLibsDir.file(neutrinoAarName)

val fetchNeutrinoBindings by tasks.registering {
    // Config-cache-friendly: doLast below runs at execution time and its
    // closure must not capture the build script itself — every value it
    // needs (including these) is copied into locals here, at configuration
    // time, rather than read from the script's top-level vals directly.
    val downloadUrl = "https://github.com/hanthor/indiafoss-companion/releases/download/" +
        "neutrino-bindings-$neutrinoVersion/$neutrinoAarName"
    val expectedSha256 = neutrinoSha256
    val outputFile = neutrinoAar.asFile
    outputs.file(outputFile)
    doLast {
        fun sha256(file: java.io.File): String =
            MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") {
                "%02x".format(it)
            }
        if (outputFile.exists() && sha256(outputFile) == expectedSha256) return@doLast
        outputFile.parentFile.mkdirs()
        logger.lifecycle("Fetching Neutrino bindings from $downloadUrl")
        URI(downloadUrl).toURL().openStream().use { input ->
            outputFile.outputStream().use { output -> input.copyTo(output) }
        }
        val actual = sha256(outputFile)
        check(actual == expectedSha256) {
            "Neutrino bindings checksum mismatch: expected $expectedSha256, got $actual"
        }
    }
}

dependencies {
    implementation(projects.libraries.androidutils)
    implementation(projects.libraries.architecture)
    implementation(projects.libraries.core)
    implementation(projects.libraries.di)
    implementation(libs.androidx.corektx)
    implementation(libs.coroutines.core)

    api(projects.services.neutrino.api)
    // builtBy (not just preBuild.dependsOn) so every consumer of this file
    // collection — including cross-project tasks like :app's dependency
    // report, which read it directly without going through this module's own
    // task graph — picks up the task dependency Gradle needs to order the
    // download correctly. Without it, Gradle's task validation rejects the
    // build outright ("uses this output... without declaring a dependency").
    implementation(files(neutrinoAar).builtBy(fetchNeutrinoBindings))

    testCommonDependencies(libs)
}
