/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

android {
    namespace = "com.amazonaws.appsync"
}

dependencies {
    api(project(":core"))
    api(libs.okhttp)
    api(project(":foundation"))

    api(project(":aws-api-appsync"))
    implementation(libs.gson)
    implementation(libs.kotlin.coroutines)
    implementation(project(":foundation-bridge"))
    implementation(platform(libs.aws.bom))
    implementation(libs.aws.signing)
    implementation(libs.aws.smithy.http)

    testImplementation(libs.bundles.test.unit)
    testImplementation(libs.bundles.test.unit.android)
    testImplementation(libs.test.kotlin.reflection)
    testImplementation(libs.test.mockwebserver)
    testImplementation(project(":testutils"))
}
