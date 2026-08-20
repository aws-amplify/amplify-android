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
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.amplify.publishing)
}

apply(from = rootProject.file("configuration/checkstyle.gradle"))

android {
    namespace = "com.amplifyframework.core"
}

dependencies {
    api(project(":annotations"))
    implementation(libs.androidx.v4support)
    api(libs.androidx.annotation)
    implementation(libs.androidx.nav.fragment)
    implementation(libs.androidx.nav.ui)
    // Fragment / FragmentActivity appear in this module's public API; declare directly as api
    // so consumers can compile against those signatures.
    api(libs.androidx.fragment)
    implementation(libs.androidx.security)
    api(libs.kotlin.serializationJson)

    api(project(":common-core"))

    testImplementation(project(":aws-api-appsync"))
    // Used to reference Temporal types in tests.
    testImplementation(project(":testmodels"))
    testImplementation(project(":testutils"))
    testImplementation(libs.bundles.test.unit)
    testImplementation(libs.bundles.test.unit.android)
    testImplementation(libs.bundles.test.mockito)
    testImplementation(libs.rxjava)
    testImplementation(libs.test.jsonassert)
    testImplementation(libs.gson)

    androidTestImplementation(libs.bundles.test.android)
    androidTestImplementation(project(":testutils"))
    androidTestImplementation(libs.androidx.annotation)
}
