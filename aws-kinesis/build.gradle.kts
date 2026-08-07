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

plugins {
    alias(libs.plugins.amplify.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.amplify.publishing)
}

apply(from = rootProject.file("configuration/checkstyle.gradle"))

android {
    namespace = "com.amplifyframework.kinesis"
}

dependencies {
    api(project(":foundation"))
    api(project(":foundation-bridge"))

    implementation(libs.androidx.appcompat)
    api(platform(libs.aws.bom))
    api(libs.aws.kinesis)
    api(libs.aws.firehose)
    implementation(libs.aws.http)
    implementation(libs.kotlin.serializationJson)
    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.sqlite.bundled)
    implementation(libs.androidx.workmanager)

    testImplementation(libs.bundles.test.unit)
    testImplementation(libs.bundles.test.unit.android)
    testImplementation(libs.androidx.sqlite.bundled.jvm)
    testImplementation(project(":testutils"))

    androidTestImplementation(libs.bundles.test.android)
    androidTestImplementation(project(":testutils"))
    androidTestImplementation(project(":core"))
    androidTestImplementation(project(":aws-core"))
    androidTestImplementation(project(":aws-auth-cognito"))
}
