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

plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

ktlint {
    android.set(true)
}

dependencies {
    compileOnly(libs.plugin.android.gradle)
    compileOnly(libs.plugin.binary.compatibility)
    compileOnly(libs.plugin.kotlin.android)
    compileOnly(libs.plugin.kover)
    compileOnly(libs.plugin.ktlint)
    compileOnly(libs.plugin.licensee)
    implementation(kotlin("test"))
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = libs.plugins.amplify.android.library.get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("apiValidator") {
            id = libs.plugins.amplify.api.get().pluginId
            implementationClass = "ApiValidatorConventionPlugin"
        }
        register("kotlin") {
            id = libs.plugins.amplify.kotlin.get().pluginId
            implementationClass = "KotlinConventionPlugin"
        }
        register("kover") {
            id = libs.plugins.amplify.kover.get().pluginId
            implementationClass = "KoverConventionPlugin"
        }
        register("ktlint") {
            id = libs.plugins.amplify.ktlint.get().pluginId
            implementationClass = "KtLintConventionPlugin"
        }
        register("licenses") {
            id = libs.plugins.amplify.licenses.get().pluginId
            implementationClass = "LicensesConventionPlugin"
        }
    }
}
