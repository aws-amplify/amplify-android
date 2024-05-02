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

                allowUrl("http://aws.amazon.com/apache2.0")
                allowUrl("https://developer.android.com/studio/terms.html")

                ignoreDependencies("javax.annotation", "javax.annotation-api") {
                    because("Transitive dependency for androidx.test.espresso:espresso-core")
                }
                ignoreDependencies("org.junit", "junit-bom") {
                    because("Unit Testing Dependency")
                }
                ignoreDependencies("org.junit", "jupiter") {
                    because("Unit Testing Dependency")
                }
                ignoreDependencies("org.junit.jupiter", "junit-jupiter") {
                    because("Unit Testing Dependency")
                }
                ignoreDependencies("org.junit.jupiter", "junit-jupiter-params") {
                    because("Unit Testing Dependency")
                }
                ignoreDependencies("org.junit", "junit-jupiter-params") {
                    because("Unit Testing Dependency")
                }
                ignoreDependencies("org.junit.platform", "junit-platform-commons") {
                    because("Unit Testing Dependency")
                }
                ignoreDependencies("org.junit.platform", "junit-platform-engine") {
                    because("Unit Testing Dependency")
                }
                ignoreDependencies("junit", "junit") {
                    because("Unit Testing Dependency")
                }
            }
        }
    }
}
