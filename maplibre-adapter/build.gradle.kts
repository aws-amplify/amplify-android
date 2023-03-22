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
    id("kotlin-parcelize")
}
apply(from = rootProject.file("configuration/checkstyle.gradle"))
apply(from = rootProject.file("configuration/publishing.gradle"))

group = properties["POM_GROUP"].toString()

android {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    lint {
        disable += "GradleDependency"
    }
}

dependencies {
    implementation(project(":aws-auth-cognito"))
    implementation(project(":aws-geo-location"))
    implementation(project(":core"))
    implementation(dependency.aws.signing)
    implementation(dependency.maplibre.sdk)
    implementation(dependency.maplibre.annotations)
    implementation(dependency.okhttp)
    implementation(dependency.kotlin.coroutines)

    implementation(dependency.androidx.lifecycle.runtime)
    implementation(dependency.google.material)

    compileOnly(dependency.aws.location)

    androidTestImplementation(project(":testutils"))
    androidTestImplementation(dependency.androidx.appcompat)
    androidTestImplementation(testDependency.androidx.test.junit)
    androidTestImplementation(testDependency.androidx.test.core)
    androidTestImplementation(testDependency.androidx.test.core.ktx)
    androidTestImplementation(testDependency.androidx.test.runner)
    androidTestImplementation(dependency.kotlin.coroutines.android)
}
