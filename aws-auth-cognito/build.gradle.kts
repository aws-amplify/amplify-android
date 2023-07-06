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
    implementation(dependency.kotlin.coroutines)
    implementation(dependency.kotlin.serializationJson)
    implementation(dependency.androidx.appcompat)
    implementation(dependency.androidx.security)
    implementation(dependency.androidx.browser)

    implementation(dependency.aws.http)
    implementation(dependency.aws.cognitoidentity)
    implementation(dependency.aws.cognitoidentityprovider)

    testImplementation(project(":testutils"))
    //noinspection GradleDependency
    testImplementation(testDependency.json)

    testImplementation(testDependency.kotlin.test.junit)
    testImplementation(testDependency.kotlin.test.kotlinTest)
    testImplementation(testDependency.kotlin.test.coroutines)

    testImplementation(dependency.gson)
    testImplementation(testDependency.junit)
    testImplementation(testDependency.mockito)
    testImplementation(testDependency.mockk)
    testImplementation(testDependency.robolectric)
    testImplementation(testDependency.androidx.test.core)
    testImplementation(testDependency.kotlin.reflection)

    androidTestImplementation(dependency.gson)
    //noinspection GradleDependency
    androidTestImplementation(testDependency.aws.sdk.core)
    androidTestImplementation(testDependency.androidx.test.runner)
    androidTestImplementation(testDependency.androidx.test.junit)
    androidTestImplementation(testDependency.kotlin.test.coroutines)
    androidTestImplementation(project(":aws-api"))
    androidTestImplementation(project(":testutils"))
}
