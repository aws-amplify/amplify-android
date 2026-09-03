/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate

/**
 * This convention plugin configures an Android library module
 */
@Suppress("LocalVariableName")
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target.pluginManager) {
            apply("com.android.library")

            // Apply other convention plugins
            apply("amplify.kotlin")
        }

        @Suppress("ktlint:standard:property-naming")
        val POM_GROUP: String by target

        with(target) {
            group = POM_GROUP
            extensions.configure<LibraryExtension> {
                configureAndroid(this)
                afterEvaluate {
                    val sdkVersionName = findProperty("VERSION_NAME") ?: rootProject.findProperty("VERSION_NAME")
                    this@configure.defaultConfig.buildConfigField("String", "VERSION_NAME", "\"$sdkVersionName\"")
                }
            }
        }
    }

    private fun Project.configureAndroid(extension: LibraryExtension) {
        extension.apply {
            compileSdk = 36

            buildFeatures {
                buildConfig = true
            }

            defaultConfig {
                minSdk = 24
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                testInstrumentationRunnerArguments += "clearPackageData" to "true"
                consumerProguardFiles += rootProject.file("configuration/consumer-rules.pro")
            }

            testOptions {
                animationsDisabled = true
                unitTests {
                    isIncludeAndroidResources = true
                }
                execution = "ANDROIDX_TEST_ORCHESTRATOR"
            }

            lint {
                lintConfig = rootProject.file("lint.xml")
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

            // These metadata files are shipped by more than one dependency, and merging them into a
            // single APK or AAR fails unless they are dropped. AGP already drops the most common
            // ones (META-INF/LICENSE, META-INF/NOTICE) by default, so only the variants it does not
            // know about are listed here.
            packaging {
                resources.excludes.addAll(
                    listOf(
                        "META-INF/COPYRIGHT",
                        "META-INF/DEPENDENCIES",
                        "META-INF/LICENSE.md",
                        "META-INF/LICENSE-notice.md",
                        "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
                    )
                )
            }
        }

        dependencies {
            "coreLibraryDesugaring"(libs.findLibrary("android-desugartools").get())
            constraints {
                add("implementation", libs.findLibrary("androidx-annotation-experimental").get()) {
                    because("Fixes a lint bug with RequiresOptIn")
                }
            }
            "androidTestUtil"(libs.findLibrary("test-androidx-orchestrator").get())
        }
    }
}
