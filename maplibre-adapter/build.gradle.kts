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

plugins {
    alias(libs.plugins.amplify.android.library)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.amplify.api)
}
apply(from = rootProject.file("configuration/checkstyle.gradle"))
apply(from = rootProject.file("configuration/publishing.gradle"))

group = properties["POM_GROUP"].toString()

android {
    namespace = "com.amplifyframework.geo.maplibre"
    lint {
        disable += "GradleDependency"
    }
}

dependencies {
    implementation(project(":aws-auth-cognito"))
    implementation(project(":aws-geo-location"))
    implementation(project(":core"))
    implementation(libs.aws.signing)
    implementation(libs.maplibre.sdk)
    implementation(libs.gson) // forces maplibre to pull at least the same gson version as other amplify libs
    implementation(libs.maplibre.annotations)
    implementation(libs.okhttp)
    implementation(libs.kotlin.coroutines)

    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.google.material)

    compileOnly(libs.aws.location)

    androidTestImplementation(project(":testutils"))
    androidTestImplementation(libs.androidx.appcompat)
    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(libs.test.androidx.core)
    androidTestImplementation(libs.test.androidx.core.ktx)
    androidTestImplementation(libs.test.androidx.runner)
    androidTestImplementation(libs.kotlin.coroutines.android)
}
