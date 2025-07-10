import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

/**
 * Applies and configures the [Licensee Plugin](https://github.com/cashapp/licensee) to ensure all our dependencies
 * (and transitive dependencies) are using allowed licenses.
 */
class LicensesConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("app.cash.licensee")

            extensions.configure<app.cash.licensee.LicenseeExtension> {
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
        }
    }
}
