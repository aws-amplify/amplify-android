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
    implementation(project(":core-kotlin"))
    implementation(project(":aws-core"))
    implementation(project(":aws-api-appsync"))

    implementation(dependency.androidx.appcompat)
    implementation(dependency.gson)
    implementation(dependency.kotlin.stdlib)
    implementation(dependency.kotlin.coroutines)
    implementation(dependency.androidx.core.ktx)
    implementation(dependency.rxjava)
    implementation(dependency.rxandroid)
    implementation(dependency.uuidgen)

    testImplementation(project(":testmodels"))
    testImplementation(project(":testutils"))
    testImplementation(testDependency.jsonassert)
    testImplementation(testDependency.junit)
    testImplementation(testDependency.mockito)
    testImplementation(testDependency.robolectric)
    testImplementation(testDependency.androidx.test.core)
    testImplementation(testDependency.mockk)

    androidTestImplementation(testDependency.mockito)
    androidTestImplementation(project(":testmodels"))
    androidTestImplementation(project(":testutils"))
    androidTestImplementation(project(":aws-api"))
    androidTestImplementation(project(":aws-datastore"))
    androidTestImplementation(dependency.androidx.annotation)
    androidTestImplementation(testDependency.androidx.test.core)
    androidTestImplementation(testDependency.androidx.test.runner)
    androidTestImplementation(testDependency.androidx.test.junit)
    androidTestImplementation(project(":aws-auth-cognito"))
    androidTestImplementation(dependency.rxjava)
    androidTestImplementation(dependency.okhttp)
    androidTestImplementation(dependency.oauth2)
}

afterEvaluate {
    android.kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}
