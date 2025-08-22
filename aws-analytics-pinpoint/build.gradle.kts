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
    alias(libs.plugins.amplify.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.amplify.api)
}

apply(from = rootProject.file("configuration/checkstyle.gradle"))
apply(from = rootProject.file("configuration/publishing.gradle"))

group = properties["POM_GROUP"].toString()

android {
    namespace = "com.amplifyframework.analytics.pinpoint"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":aws-core"))
    implementation(project(":aws-pinpoint-core"))

    implementation(libs.androidx.appcompat)
    implementation(libs.aws.pinpoint)
    implementation(libs.kotlin.serializationJson)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.mockito.core)
    testImplementation(libs.test.mockito.inline)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.test.androidx.junit)
    testImplementation(libs.test.androidx.core)
    testImplementation(libs.test.kotlin.coroutines)
    testImplementation(libs.test.kotest.assertions)
    testImplementation(project(":testutils"))
    testImplementation(project(":aws-analytics-pinpoint"))

    androidTestImplementation(project(":testutils"))
    androidTestImplementation(libs.test.androidx.core)
    androidTestImplementation(project(":aws-auth-cognito"))
    androidTestImplementation(libs.test.androidx.runner)
    androidTestImplementation(libs.test.kotlin.coroutines)
    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(project(":aws-analytics-pinpoint"))
}
