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
    alias(libs.plugins.amplify.api)
}

apply(from = rootProject.file("configuration/checkstyle.gradle"))
apply(from = rootProject.file("configuration/publishing.gradle"))

group = properties["POM_GROUP"].toString()

android {
    namespace = "com.amplifyframework.storage.s3"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":aws-core"))

    implementation(libs.androidx.appcompat)
    implementation(libs.aws.s3)
    implementation(libs.androidx.workmanager)
    implementation(libs.kotlin.futures)
    implementation(libs.gson)

    testImplementation(project(":testutils"))
    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockito.core)
    testImplementation(libs.test.mockito.inline)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.test.androidx.core)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.androidx.workmanager)
    testImplementation(libs.test.kotlin.coroutines)
    testImplementation(libs.test.kotest.assertions)
    testImplementation(project(":aws-storage-s3"))

    androidTestImplementation(project(":testutils"))
    androidTestImplementation(project(":aws-auth-cognito"))
    androidTestImplementation(libs.androidx.annotation)
    androidTestImplementation(libs.test.androidx.runner)
    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(libs.test.androidx.workmanager)
    androidTestImplementation(libs.test.kotest.assertions)
    androidTestImplementation(project(":aws-storage-s3"))

    androidTestUtil(libs.test.androidx.orchestrator)
}
