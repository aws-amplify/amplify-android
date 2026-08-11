/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

// A minimal Android library that depends on ONE published Amplify artifact (passed via
// -PmoduleCoords=group:artifact:version) and dumps its resolved release compile classpath.
// Android variant attributes are required to resolve aar artifacts and to apply api/implementation
// scope filtering the way a real consumer does — which is why this must be a Gradle/AGP build and
// cannot be replicated by reading POMs alone.
// Plugin versions are resolved in settings.gradle.kts from -Pagp/-Pkgp (injected by the CI
// driver, sourced from the root gradle/libs.versions.toml so they never drift).
plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.amplifyframework.scopecheck"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
}

val moduleCoords: String = providers.gradleProperty("moduleCoords").get()

dependencies {
    "implementation"(moduleCoords)
}

// Emit the resolved RELEASE compile classpath (aar/jar files), one absolute path per line.
tasks.register("dumpCompileClasspath") {
    doLast {
        val cfg = configurations.getByName("releaseCompileClasspath")
        val out = layout.buildDirectory.file("compile-classpath.txt").get().asFile
        out.parentFile.mkdirs()
        out.writeText(
            cfg.incoming.artifactView { isLenient = true }
                .artifacts.artifactFiles.files.joinToString("\n") { it.absolutePath }
        )
        println("WROTE ${out.absolutePath}")
    }
}
