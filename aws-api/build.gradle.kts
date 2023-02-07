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
    api(project(":core"))
    implementation(project(":aws-api-appsync"))

    implementation(dependency.androidx.appcompat)
    implementation(dependency.aws.signing)
    implementation(dependency.gson)
    implementation(dependency.okhttp)

    testImplementation(project(":testutils"))
    testImplementation(project(":testmodels"))
    testImplementation(testDependency.androidx.test.core)
    testImplementation(testDependency.jsonassert)
    testImplementation(testDependency.junit)
    testImplementation(testDependency.mockito)
    testImplementation(testDependency.mockwebserver)
    testImplementation(dependency.rxjava)
    testImplementation(testDependency.robolectric)

    androidTestImplementation(project(":testutils"))
    androidTestImplementation(project(":testmodels"))
    androidTestImplementation(testDependency.androidx.test.core)
    androidTestImplementation(project(":aws-auth-cognito"))
    androidTestImplementation(testDependency.androidx.test.runner)
    androidTestImplementation(testDependency.androidx.test.junit)
    androidTestImplementation(dependency.rxjava)
}
