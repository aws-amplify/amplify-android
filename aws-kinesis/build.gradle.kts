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
    implementation(project(":foundation"))
    implementation(project(":foundation-bridge"))

    implementation(libs.androidx.appcompat)
    implementation(libs.aws.kinesis)
    implementation(libs.kotlin.serializationJson)
    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.sqlite.bundled)
    implementation(libs.androidx.workmanager)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.mockito.core)
    testImplementation(libs.test.mockito.inline)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.test.androidx.junit)
    testImplementation(libs.test.androidx.core)
    testImplementation(libs.test.kotlin.coroutines)
    testImplementation(libs.test.kotest.assertions)
    testImplementation(libs.androidx.sqlite.bundled.jvm)
    testImplementation(project(":testutils"))
    testImplementation(project(":aws-kinesis"))

    androidTestImplementation(project(":testutils"))
    androidTestImplementation(libs.test.androidx.core)
    androidTestImplementation(project(":core"))
    androidTestImplementation(project(":aws-core"))
    androidTestImplementation(project(":aws-auth-cognito"))
    androidTestImplementation(libs.test.androidx.runner)
    androidTestImplementation(libs.test.kotlin.coroutines)
    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(libs.test.kotest.assertions)
    androidTestImplementation(project(":aws-kinesis"))
    androidTestImplementation(libs.androidx.sqlite)

    androidTestUtil(libs.test.androidx.orchestrator)
}
