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
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.10"
    id("com.android.library")
    id("kotlin-android")
}

apply(from = rootProject.file("configuration/checkstyle.gradle"))

dependencies {
    implementation(project(":core"))
    implementation(testDependency.junit)
    implementation(testDependency.mockito)
    implementation(testDependency.androidx.test.core)
    implementation(dependency.rxjava)

    implementation(dependency.kotlin.serializationJson)
    implementation(dependency.aws.cognitoidentity)
    implementation(dependency.aws.cognitoidentityprovider)

    // dependency on Model/GraphQL integration classes
    // remove when modules are re-organized to provide better isolation
    compileOnly(project(":aws-api"))
}
