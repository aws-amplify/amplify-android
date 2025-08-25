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
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.nav.fragment)
    implementation(libs.androidx.nav.ui)
    implementation(libs.androidx.security)
    implementation(libs.kotlin.serializationJson)

    api(project(":common-core"))

    testImplementation(project(":aws-api-appsync"))
    // Used to reference Temporal types in tests.
    testImplementation(project(":testmodels"))
    testImplementation(project(":testutils"))
    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockito.core)
    testImplementation(libs.test.mockito.inline)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.rxjava)
    testImplementation(libs.test.androidx.core)
    testImplementation(libs.test.jsonassert)
    testImplementation(libs.gson)
    testImplementation(libs.test.kotest.assertions)

    androidTestImplementation(project(":testutils"))
    androidTestImplementation(libs.androidx.annotation)
    androidTestImplementation(libs.test.androidx.core)
    androidTestImplementation(libs.test.androidx.runner)
    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(libs.test.androidx.espresso)
    androidTestImplementation(libs.test.androidx.navigation)
    androidTestImplementation(libs.test.androidx.fragment)
}
