/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

// Standalone synthetic consumer used by scripts/verify_api_scopes.py to resolve the compile
// classpath a real downstream project would assemble from a PUBLISHED Amplify artifact. It is
// intentionally NOT part of the main build (not in the root settings.gradle.kts) so that it
// resolves artifacts from a repository (mavenLocal) rather than via in-repo project substitution
// — the whole point is to exercise the published POM/Gradle-metadata scopes.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // Plugin versions are injected via -Pagp/-Pkgp by scripts/verify_api_scopes.sh, sourced from
    // the root gradle/libs.versions.toml so they never drift from the versions the libraries build
    // with. settings.gradle.kts (unlike a build script's plugins{} block) can resolve providers.
    val agp = providers.gradleProperty("agp").get()
    val kgp = providers.gradleProperty("kgp").get()
    plugins {
        id("com.android.library") version agp
        id("org.jetbrains.kotlin.android") version kgp
    }
}
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
    }
}
rootProject.name = "scope-check-consumer"
