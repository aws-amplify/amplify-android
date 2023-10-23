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

import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.3.1")
        classpath(kotlin("gradle-plugin", version = "1.7.10"))
        classpath("com.google.gms:google-services:4.3.15")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:11.0.0")
        classpath("org.gradle:test-retry-gradle-plugin:1.4.1")
        classpath("org.jetbrains.kotlinx:kover:0.6.1")
        classpath("app.cash.licensee:licensee-gradle-plugin:1.7.0")
    }
}

allprojects {
    repositories {
        maven(url = "https://aws.oss.sonatype.org/content/repositories/snapshots/")
        google()
        mavenCentral()
    }

    gradle.projectsEvaluated {
        tasks.withType<JavaCompile>().configureEach {
            options.compilerArgs.apply {
                add("-Xlint:all")
                add("-Werror")
            }
        }
        tasks.withType<Test>().configureEach {
            minHeapSize = "128m"
            maxHeapSize = "4g"
        }
    }
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}

val optInAnnotations = listOf(
    "com.amplifyframework.annotations.InternalApiWarning",
    "com.amplifyframework.annotations.InternalAmplifyApi"
)

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
    }

    apply(plugin = "app.cash.licensee")
    configure<app.cash.licensee.LicenseeExtension> {
        allow("Apache-2.0")
        allow("MIT")
        allow("BSD-2-Clause")
        allow("CC0-1.0")
        allowUrl("https://www.zetetic.net/sqlcipher/license/")
        allowUrl("https://developer.android.com/studio/terms.html")
        allowDependency("org.jetbrains", "annotations", "16.0.1") {
            "Apache-2.0, but typo in license URL fixed in newer versions"
        }
        allowDependency("org.mockito", "mockito-core", "3.9.0") {
            "MIT license"
        }
        allowDependency("junit", "junit", "4.13.2") {
            "Test Dependency"
        }
    }

    afterEvaluate {
        configureAndroid()
        apply(from = "../kover.gradle")
    }

    apply(plugin = "org.gradle.test-retry")

    tasks.withType<Test>().configureEach {
        retry {
            maxRetries.set(1)
            maxFailures.set(100)
            failOnPassedAfterRetry.set(true)
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            optInAnnotations.forEach {
                freeCompilerArgs += "-opt-in=$it"
            }
        }
    }
}

@Suppress("ExpiredTargetSdkVersion")
fun Project.configureAndroid() {
    val sdkVersionName = findProperty("VERSION_NAME") ?: rootProject.findProperty("VERSION_NAME")

    if (hasProperty("signingKeyId")) {
        println("Getting signing info from protected source.")
        extra["signing.keyId"] = findProperty("signingKeyId")
        extra["signing.password"] = findProperty("signingPassword")
        extra["signing.inMemoryKey"] = findProperty("signingInMemoryKey")
    }

    configure<LibraryExtension> {
        buildToolsVersion = "30.0.3"
        compileSdk = 32

        defaultConfig {
            minSdk = 24
            targetSdk = 30
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            testInstrumentationRunnerArguments += "clearPackageData" to "true"
            consumerProguardFiles += rootProject.file("configuration/consumer-rules.pro")

            testOptions {
                animationsDisabled = true
                unitTests {
                    isIncludeAndroidResources = true
                }
            }

            buildConfigField("String", "VERSION_NAME", "\"$sdkVersionName\"")
        }

        lint {
            warningsAsErrors = true
            abortOnError = true
            enable += listOf("UnusedResources", "NewerVersionAvailable")
        }

        compileOptions {
            isCoreLibraryDesugaringEnabled = true
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        // Needed when running integration tests. The oauth2 library uses relies on two
        // dependencies (Apache's httpcore and httpclient), both of which include
        // META-INF/DEPENDENCIES. Tried a couple other options to no avail.
        packagingOptions {
            resources.excludes.add("META-INF/DEPENDENCIES")
        }
    }

    dependencies {
        add("coreLibraryDesugaring", libs.android.desugartools)
    }
}

apply(from = rootProject.file("configuration/instrumentation-tests.gradle"))
