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
    alias(libs.plugins.amplify.publishing)
}
apply(from = rootProject.file("configuration/checkstyle.gradle"))

android {
    namespace = "com.amplifyframework.geo.maplibre"
    lint {
        disable += "GradleDependency"
    }
}

dependencies {
    implementation(project(":aws-auth-cognito"))
    api(project(":aws-geo-location"))
    api(project(":core"))
    implementation(platform(libs.aws.bom))
    implementation(libs.aws.signing)
    api(libs.maplibre.sdk)
    api(libs.gson) // forces maplibre to pull at least the same gson version as other amplify libs
    api(libs.maplibre.annotations)
    implementation(libs.okhttp)
    implementation(libs.kotlin.coroutines)

    api(libs.androidx.lifecycle.runtime)
    // CoordinatorLayout is a supertype of the public AmplifyMapView; declare directly as api.
    api(libs.androidx.coordinatorlayout)
    implementation(libs.google.material)

    compileOnly(libs.aws.location)

    androidTestImplementation(libs.bundles.test.android)
    androidTestImplementation(project(":testutils"))
    androidTestImplementation(libs.androidx.appcompat)
    androidTestImplementation(libs.test.androidx.core.ktx)
}
