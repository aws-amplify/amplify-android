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
    id("org.jetbrains.kotlin.android")
}

apply(from = rootProject.file("configuration/checkstyle.gradle"))
apply(from = rootProject.file("configuration/publishing.gradle"))

group = properties["POM_GROUP"].toString()

dependencies {
    implementation(project(":core"))
    implementation(project(":aws-core"))
    implementation(project(":common-core"))
    implementation(project(":aws-pinpoint-core"))

    implementation(project(":aws-push-notifications-pinpoint-common"))
    api(dependency.firebasemessaging)

    implementation(dependency.aws.http)
    implementation(dependency.aws.pinpoint)

    implementation(dependency.androidx.core.ktx)
    implementation(dependency.androidx.appcompat)
    implementation(dependency.kotlin.serializationJson)
    implementation("androidx.core:core-ktx:+")

    testImplementation(testDependency.junit)

    androidTestImplementation(project(":aws-auth-cognito"))
    androidTestImplementation(testDependency.androidx.test.runner)
    androidTestImplementation(testDependency.androidx.test.junit)
}
