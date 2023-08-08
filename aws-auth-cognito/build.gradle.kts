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
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.10"
    id("com.android.library")
    id("kotlin-android")
}

apply(from = rootProject.file("configuration/publishing.gradle"))
apply(from = rootProject.file("configuration/checkstyle.gradle"))

group = properties["POM_GROUP"].toString()

dependencies {
    implementation(project(":core"))
    implementation(project(":aws-core"))
    implementation(project(":aws-auth-plugins-core"))
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.serializationJson)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.security)
    implementation(libs.androidx.browser)

    implementation(libs.aws.http)
    implementation(libs.aws.cognitoidentity)
    implementation(libs.aws.cognitoidentityprovider)

    testImplementation(project(":testutils"))
    //noinspection GradleDependency
    testImplementation(libs.test.json)

    testImplementation(libs.test.kotlin.junit)
    testImplementation(libs.test.kotlin.kotlinTest)
    testImplementation(libs.test.kotlin.coroutines)

    testImplementation(libs.gson)
    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockito.core)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.test.androidx.core)
    testImplementation(libs.test.kotlin.reflection)

    androidTestImplementation(libs.gson)
    //noinspection GradleDependency
    androidTestImplementation(libs.test.aws.sdk.core)
    androidTestImplementation(libs.test.androidx.runner)
    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(libs.test.kotlin.coroutines)
    androidTestImplementation(project(":aws-api"))
    androidTestImplementation(project(":testutils"))
}
