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
    alias(libs.plugins.amplify.publishing)
}

apply(from = rootProject.file("configuration/checkstyle.gradle"))

android {
    namespace = "com.amplifyframework.datastore"
}

dependencies {
    api(project(":core"))
    implementation(project(":aws-core"))
    api(project(":aws-api-appsync"))

    implementation(libs.androidx.appcompat)
    // androidx.core's Supplier leaks into the public API (Orchestrator constructor); declare
    // androidx.core directly as api rather than relying on transitive resolution via appcompat.
    api(libs.androidx.core)
    api(libs.gson)
    implementation(libs.kotlin.coroutines.rx3)
    api(libs.rxjava)
    implementation(libs.uuidgen)

    testImplementation(libs.bundles.test.unit)
    testImplementation(libs.bundles.test.unit.android)
    testImplementation(libs.bundles.test.mockito)
    testImplementation(libs.test.jsonassert)
    testImplementation(project(":testmodels"))
    testImplementation(project(":testutils"))

    androidTestImplementation(libs.bundles.test.android)
    androidTestImplementation(project(":testmodels"))
    androidTestImplementation(project(":testutils"))
    androidTestImplementation(project(":aws-api"))
    androidTestImplementation(libs.androidx.annotation)
    androidTestImplementation(project(":aws-auth-cognito"))
    androidTestImplementation(libs.okhttp)
    androidTestImplementation(libs.oauth2)
}
