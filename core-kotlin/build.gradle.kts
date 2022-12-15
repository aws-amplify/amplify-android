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
apply(from = rootProject.file("configuration/publishing.gradle"))

group = properties["POM_GROUP"].toString()

dependencies {
    implementation(dependency.kotlin.stdlib)
    implementation(dependency.kotlin.coroutines)
    implementation(dependency.androidx.core.ktx)
    implementation(project(":core"))

    testImplementation(testDependency.androidx.test.core)
    testImplementation(testDependency.junit)
    testImplementation(testDependency.mockk)
    testImplementation(testDependency.kotlin.test.coroutines)
    testImplementation(project(":testmodels"))
}

android.kotlinOptions {
    jvmTarget = "1.8"
    freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
}
