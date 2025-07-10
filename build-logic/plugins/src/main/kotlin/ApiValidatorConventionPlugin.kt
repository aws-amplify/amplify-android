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

import kotlinx.validation.ApiValidationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * This plugin configures the [Binary Compatibility Validator](https://github.com/Kotlin/binary-compatibility-validator)
 * to ensure that we don't change public API surface unintentionally.
 */
class ApiValidatorConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlinx.binary-compatibility-validator")

            extensions.configure<ApiValidationExtension> {
                // Ignore anything marked with an internal API marker
                nonPublicMarkers += amplifyInternalMarkers
                nonPublicMarkers += "androidx.annotation.VisibleForTesting"
            }
        }
    }
}
