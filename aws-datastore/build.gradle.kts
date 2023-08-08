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
    implementation(project(":aws-api-appsync"))

    implementation(libs.androidx.appcompat)
    implementation(libs.gson)
    implementation(libs.rxjava)
    implementation(libs.uuidgen)

    testImplementation(project(":testmodels"))
    testImplementation(project(":testutils"))
    testImplementation(libs.test.jsonassert)
    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockito.core)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.test.androidx.core)
    testImplementation(libs.test.mockk)

    androidTestImplementation(libs.test.mockito.core)
    androidTestImplementation(project(":testmodels"))
    androidTestImplementation(project(":testutils"))
    androidTestImplementation(project(":aws-api"))
    androidTestImplementation(project(":aws-datastore"))
    androidTestImplementation(libs.androidx.annotation)
    androidTestImplementation(libs.test.androidx.core)
    androidTestImplementation(libs.test.androidx.runner)
    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(project(":aws-auth-cognito"))
    androidTestImplementation(libs.rxjava)
    androidTestImplementation(libs.okhttp)
    androidTestImplementation(libs.oauth2)
}

afterEvaluate {
    android.kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}
