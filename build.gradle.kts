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
import org.jetbrains.dokka.gradle.DokkaTask

buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.3.1")
        classpath(kotlin("gradle-plugin", version = "1.7.10"))
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.7.10")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:11.0.0")
        classpath("org.gradle:test-retry-gradle-plugin:1.4.1")
        classpath("org.jetbrains.kotlinx:kover:0.6.1")
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

apply(plugin = "org.jetbrains.dokka")
tasks.withType<DokkaTask>().configureEach {
    outputDirectory.set(rootProject.buildDir)
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
    }

    afterEvaluate {
        configureAndroid()
        apply(from = "../kover.gradle")
    }

    if (!name.contains("test")) {
        apply(plugin = "org.jetbrains.dokka")
        tasks.withType<DokkaTask>().configureEach {
            dokkaSourceSets {
                configureEach {
                    includeNonPublic.set(false)
                    skipEmptyPackages.set(true)
                    skipDeprecated.set(true)
                    reportUndocumented.set(true)
                    jdkVersion.set(8)
                }
            }
        }
    }

    apply(plugin = "org.gradle.test-retry")

    tasks.withType<Test>().configureEach {
        retry {
            maxRetries.set(9)
            maxFailures.set(100)
            failOnPassedAfterRetry.set(true)
        }
    }
}

@Suppress("ExpiredTargetSdkVersion")
fun Project.configureAndroid() {
    val sdkVersionName = findProperty("VERSION_NAME") ?: rootProject.findProperty("VERSION_NAME")

    if (hasProperty("signingKeyId")) {
        println("Getting signing info from protected source.")
        project.setProperty("signing.keyId", findProperty("signingKeyId"))
        project.setProperty("signing.password", findProperty("signingPassword"))
        project.setProperty("signing.inMemoryKey", findProperty("signingInMemoryKey"))
    }

    configure<LibraryExtension> {
        buildToolsVersion = "30.0.2"
        compileSdk = 31

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
        add("coreLibraryDesugaring", dependency.android.desugartools)
    }
}

apply(from = rootProject.file("configuration/instrumentation-tests.gradle"))
