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
    id("com.android.library")
    id("kotlin-android")
}
apply(from = rootProject.file("configuration/checkstyle.gradle"))
apply(from = rootProject.file("configuration/publishing.gradle"))

group = properties["POM_GROUP"].toString()

dependencies {
    implementation(project(":core"))
    implementation(project(":aws-core"))
    implementation(libs.aws.location)

    testImplementation(project(":testutils"))
    testImplementation(libs.test.junit)
    testImplementation(libs.test.robolectric)

    androidTestImplementation(project(":testutils"))
    androidTestImplementation(project(":aws-auth-cognito"))
    androidTestImplementation(libs.androidx.annotation)
    androidTestImplementation(libs.test.androidx.core)
    androidTestImplementation(libs.test.androidx.runner)
    androidTestImplementation(libs.test.androidx.junit)
}

afterEvaluate {
    android.kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}
