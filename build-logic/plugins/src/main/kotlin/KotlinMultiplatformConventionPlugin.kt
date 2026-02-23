/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.android.build.api.dsl.androidLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * This convention plugin configures a Kotlin Multiplatform module with Android support
 */
@Suppress("UnstableApiUsage")
class KotlinMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target.pluginManager) {
            apply("org.jetbrains.kotlin.multiplatform")
            apply("com.android.kotlin.multiplatform.library")
            apply("com.android.lint")
            apply("amplify.ktlint")
        }

        with(target) {
            extensions.configure<KotlinMultiplatformExtension> {
                configureKotlinMultiplatform(this)
            }
        }
    }

    private fun Project.configureKotlinMultiplatform(extension: KotlinMultiplatformExtension) {
        extension.apply {
            androidLibrary {
                namespace = "com.amplifyframework.${project.name.replace("-", ".")}"
                compileSdk = 36
                minSdk = 24

                withHostTestBuilder {
                }

                withDeviceTestBuilder {
                    sourceSetTreeName = "test"
                }.configure {
                    instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
            }

            sourceSets.apply {
                commonMain {
                    dependencies {
                        implementation(libs.findLibrary("kotlin-stdlib").get())
                    }
                }

                commonTest {
                    dependencies {
                        implementation(libs.findLibrary("test-kotlin-kotlinTest").get())
                    }
                }

                getByName("androidDeviceTest") {
                    dependencies {
                        implementation(libs.findLibrary("test-androidx-runner").get())
                        implementation(libs.findLibrary("test-androidx-core").get())
                        implementation(libs.findLibrary("test-androidx-junit").get())
                    }
                }
            }

            compilerOptions {
                freeCompilerArgs.addAll(amplifyInternalMarkers.map { "-opt-in=$it" })
                freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
            }
        }
    }
}
