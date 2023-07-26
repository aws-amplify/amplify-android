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
    id("com.android.library")
    id("kotlin-android")
}

apply(from = rootProject.file("configuration/checkstyle.gradle"))
apply(from = rootProject.file("configuration/publishing.gradle"))

group = properties["POM_GROUP"].toString()

dependencies {
    val lifecycleVersion = "2.4.1"

    implementation(project(":annotations"))
    api(project(":common-core"))

    implementation(dependency.androidx.core.ktx)
    implementation(dependency.androidx.activity)
    implementation(dependency.androidx.appcompat)
    implementation(dependency.androidx.annotation)
    implementation(dependency.androidx.core)
    implementation(dependency.firebasemessaging)

    //noinspection GradleDependency
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    //noinspection GradleDependency
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    //noinspection GradleDependency
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    //noinspection GradleDependency
    implementation(dependency.google.material)

    testImplementation(testDependency.junit)
    testImplementation(testDependency.mockk)

    androidTestImplementation(testDependency.androidx.test.runner)
    androidTestImplementation(testDependency.androidx.test.junit)
}
