import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

class KotlinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target.pluginManager) {
            // Apply the proper kotlin plugin
            if (hasPlugin("com.android.base")) {
                apply("org.jetbrains.kotlin.android")
            } else {
                apply("java-library")
                apply("org.jetbrains.kotlin.jvm")
            }

            // Apply other convention plugins
            apply("amplify.ktlint")

            // Note: these only need to be applied in a future publishing convention plugin
            withPlugin("maven-publish") {
                apply("amplify.kover")
                apply("amplify.licenses")
            }
        }

        with(target) {
            // Set up signing properties for publishing
            if (hasProperty("signingKeyId")) {
                println("Getting signing info from protected source.")
                extra["signing.keyId"] = findProperty("signingKeyId")
                extra["signing.password"] = findProperty("signingPassword")
                extra["signing.inMemoryKey"] = findProperty("signingInMemoryKey")
            }

            configure<KotlinProjectExtension> {
                jvmToolchain(17)
            }

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

            tasks.withType<KotlinCompile>().configureEach {
                compilerOptions {
                    freeCompilerArgs.addAll(amplifyInternalMarkers.map { "-opt-in=$it" })
                    freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
                }
            }
        }
    }
}
