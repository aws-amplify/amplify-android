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
    namespace = "com.amplifyframework.api.aws"
}

dependencies {
    api(project(":core"))
    api(project(":aws-core"))
    implementation(project(":aws-api-appsync"))

    implementation(libs.androidx.appcompat)
    implementation(libs.aws.signing)
    implementation(libs.gson)
    implementation(libs.okhttp)

    testImplementation(project(":testutils"))
    testImplementation(project(":testmodels"))
    testImplementation(libs.test.androidx.core)
    testImplementation(libs.test.jsonassert)
    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockito.core)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.kotest.assertions)
    testImplementation(libs.test.mockwebserver)
    testImplementation(libs.rxjava)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.test.kotlin.coroutines)

    androidTestImplementation(project(":testutils"))
    androidTestImplementation(project(":testmodels"))
    androidTestImplementation(libs.test.androidx.core)
    androidTestImplementation(project(":aws-auth-cognito"))
    androidTestImplementation(project(":core-kotlin"))
    androidTestImplementation(libs.test.androidx.runner)
    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(libs.rxjava)
    androidTestImplementation(libs.test.kotlin.coroutines)

    androidTestUtil(libs.test.androidx.orchestrator)
}
