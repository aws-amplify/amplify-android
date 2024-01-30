/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
    id("com.android.library")
    id("kotlin-android")
}

apply(from = rootProject.file("configuration/checkstyle.gradle"))
apply(from = rootProject.file("configuration/publishing.gradle"))

group = properties["POM_GROUP"].toString()

dependencies {
    implementation(project(":core"))
    implementation(project(":aws-core"))

    implementation(libs.androidx.security)
    implementation(libs.aws.signing)
    implementation(libs.okhttp)
    implementation(libs.aws.cloudwatchlogs)
    implementation(libs.sqlcipher)
    implementation(libs.androidx.sqlite)
    implementation(libs.kotlin.serializationJson)
    implementation(libs.androidx.workmanager)
    implementation(libs.kotlin.futures)

    testImplementation(project(":testutils"))
    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.test.androidx.core)
    testImplementation(libs.test.kotlin.coroutines)
    testImplementation(libs.test.androidx.workmanager)
    testImplementation(project(":aws-logging-cloudwatch"))
}

android.kotlinOptions {
    jvmTarget = "11"
}
