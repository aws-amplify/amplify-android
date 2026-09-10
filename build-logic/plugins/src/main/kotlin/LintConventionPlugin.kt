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

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
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
            // AGP nests the Lint DSL in the `android` extension, whatever the Android module type.
            // Everything else needs the standalone plugin, which registers `lint` at the top level.
            val android = extensions.findByName("android")
            if (android is CommonExtension<*, *, *, *, *, *>) {
                configureLint(android.lint)
            } else {
                pluginManager.apply("com.android.lint")
                extensions.configure<Lint> { configureLint(this) }
            }

            // Android modules get `lintChecks` eagerly from AGP, but the standalone plugin only
            // creates it once a JVM plugin is present — so react to that rather than assume an
            // apply order. The checks module is skipped because it cannot depend on itself.
            if (path != LINT_RULES_PATH) {
                plugins.withType<JavaBasePlugin> {
                    dependencies {
                        add("lintChecks", project(LINT_RULES_PATH))
                    }
                }
            }
        }
    }

    // The Lint settings shared by every module. Both the Android plugins and the standalone
    // `com.android.lint` plugin expose this same Lint interface, so one helper serves all of them.
    private fun Project.configureLint(lint: Lint) {
        lint.lintConfig = rootProject.file("lint.xml")
        lint.warningsAsErrors = true
        lint.abortOnError = true
        // UnusedResources is a no-op in modules without resources, but keeping one shared list is
        // the point of this plugin.
        lint.enable += listOf("UnusedResources")
        lint.disable += listOf(
            "GradleDependency",
            "NewerVersionAvailable",
            "AndroidGradlePluginVersion",
            "CredentialDependency"
        )
    }

    private companion object {
        const val LINT_RULES_PATH = ":lint-rules"
    }
}
