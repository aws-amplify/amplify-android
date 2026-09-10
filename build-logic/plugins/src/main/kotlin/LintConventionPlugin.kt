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

import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * Runs Android Lint, with the project's custom checks, against a module.
 *
 * Android library modules get Lint from the Android plugin, which nests its configuration under the
 * `android` extension. Every other module type needs the standalone `com.android.lint` plugin,
 * which registers `lint` as a top-level extension instead.
 */
class LintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            if (pluginManager.hasPlugin("com.android.base")) {
                extensions.configure<LibraryExtension> { configureLint(lint) }
            } else {
                pluginManager.apply("com.android.lint")
                extensions.configure<Lint> { configureLint(this) }
            }

            // The standalone plugin creates `lintChecks` only once a JVM plugin is present, so react
            // to that rather than depending on an apply order. The checks module is skipped because
            // it cannot depend on itself.
            if (path != CHECKS_PROJECT) {
                plugins.withType<JavaBasePlugin> {
                    dependencies {
                        add("lintChecks", project(CHECKS_PROJECT))
                    }
                }
            }
        }
    }

    private companion object {
        const val CHECKS_PROJECT = ":lint-rules"
    }
}
