/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import app.cash.licensee.LicenseeExtension
import com.android.build.gradle.LibraryExtension
import kotlinx.validation.ApiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.collections.forEach

buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath(kotlin("gradle-plugin", version = "2.2.0"))
        classpath("com.google.gms:google-services:4.3.15")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:12.2.0")
        classpath("app.cash.licensee:licensee-gradle-plugin:1.7.0")
    }
}

plugins {
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kover)
}

allprojects {
    gradle.projectsEvaluated {
        tasks.withType<JavaCompile>().configureEach {
            options.compilerArgs.apply {
                add("-Xlint:all")
                // add("-Werror")
            }
        }
        tasks.withType<Test>().configureEach {
            minHeapSize = "128m"
            maxHeapSize = "4g"
        }
    }
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.layout.buildDirectory)
}

val internalApiAnnotations = listOf(
    "com.amplifyframework.annotations.InternalApiWarning",
    "com.amplifyframework.annotations.InternalAmplifyApi",
    "com.amplifyframework.annotations.AmplifyFlutterApi"
)

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.5.0")
        android.set(true)
        filter {
            exclude("**/generated/**")
        }
    }

    apply(plugin = "app.cash.licensee")
    afterEvaluate {
        configure<LicenseeExtension> {
            allow("Apache-2.0")
            allow("MIT")
            allow("BSD-2-Clause")
            allow("CC0-1.0")
            allowUrl("https://developer.android.com/studio/terms.html")
            allowDependency("net.zetetic", "sqlcipher-android", "4.6.1") {
                because("BSD style License")
            }
            allowDependency("org.jetbrains", "annotations", "16.0.1") {
                because("Apache-2.0, but typo in license URL fixed in newer versions")
            }
            allowDependency("org.mockito", "mockito-core", "3.9.0") {
                because("MIT license")
            }
            allowDependency("junit", "junit", "4.13.2") {
                because("Test Dependency")
            }
            allowUrl("https://raw.githubusercontent.com/apollographql/apollo-kotlin/main/LICENSE") {
                because("MIT license")
            }
        }

        configureAndroid()

        if (!project.name.contains("test")) {
            apply(from = rootProject.file("kover.gradle"))
        }
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            internalApiAnnotations.forEach {
                freeCompilerArgs.add("-opt-in=$it")
                freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
            }
        }
    }

    pluginManager.withPlugin("kotlin-android") {
        configure<KotlinProjectExtension> {
            jvmToolchain(17)
        }
    }
}

@Suppress("ExpiredTargetSdkVersion")
fun Project.configureAndroid() {
    if (hasProperty("signingKeyId")) {
        println("Getting signing info from protected source.")
        extra["signing.keyId"] = findProperty("signingKeyId")
        extra["signing.password"] = findProperty("signingPassword")
        extra["signing.inMemoryKey"] = findProperty("signingInMemoryKey")
    }

    pluginManager.withPlugin("com.android.library") {
        val sdkVersionName = findProperty("VERSION_NAME") ?: rootProject.findProperty("VERSION_NAME")

        configure<LibraryExtension> {
            compileSdk = 34

            buildFeatures {
                // Allow specifying custom buildConfig fields
                buildConfig = true
            }

            defaultConfig {
                minSdk = 24
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                testInstrumentationRunnerArguments += "clearPackageData" to "true"
                consumerProguardFiles += rootProject.file("configuration/consumer-rules.pro")
                buildConfigField("String", "VERSION_NAME", "\"$sdkVersionName\"")
            }

            testOptions {
                animationsDisabled = true
                unitTests {
                    isIncludeAndroidResources = true
                }
                execution = "ANDROIDX_TEST_ORCHESTRATOR"
            }

            lint {
                warningsAsErrors = true
                abortOnError = true
                enable += listOf("UnusedResources")
                disable += listOf(
                    "GradleDependency",
                    "NewerVersionAvailable",
                    "AndroidGradlePluginVersion",
                    "CredentialDependency"
                )
            }

            compileOptions {
                isCoreLibraryDesugaringEnabled = true
            }

            // Needed when running integration tests. The oauth2 library uses relies on two
            // dependencies (Apache's httpcore and httpclient), both of which include
            // META-INF/DEPENDENCIES. Tried a couple other options to no avail.
            packaging {
                resources.excludes.addAll(
                    listOf(
                        "META-INF/DEPENDENCIES",
                        "META-INF/LICENSE.md",
                        "META-INF/LICENSE-notice.md"
                    )
                )
            }

            publishing {
                singleVariant("release") {
                    withSourcesJar()
                }
            }
        }

        dependencies {
            add("coreLibraryDesugaring", libs.android.desugartools)
            constraints {
                add("implementation", libs.androidx.annotation.experimental) {
                    because("Fixes a lint bug with RequiresOptIn")
                }
            }
        }
    }
}

apply(from = rootProject.file("configuration/instrumentation-tests.gradle"))

configure<ApiValidationExtension> {
    // Interfaces marked with an internal API annotation are not part of the public API
    nonPublicMarkers.addAll(internalApiAnnotations)
    nonPublicMarkers.add("androidx.annotation.VisibleForTesting")

    ignoredProjects.addAll(setOf("testutils", "testmodels", "annotations"))
}

dependencies {
    subprojects.forEach {
        if (!it.name.contains("test")) {
            kover(project(it.name))
        }
    }
}
