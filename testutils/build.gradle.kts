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
    alias(libs.plugins.kotlin.parcelize)
}

apply(from = rootProject.file("configuration/checkstyle.gradle"))

android {
    namespace = "com.amplifyframework.testutils"
}

dependencies {
    implementation(project(":core"))
    implementation(libs.test.junit)
    implementation(libs.test.mockito.core)
    implementation(libs.test.androidx.core)
    implementation(libs.rxjava)

    implementation(libs.kotlin.serializationJson)
    implementation(libs.aws.cognitoidentity)
    implementation(libs.aws.cognitoidentityprovider)

    // dependency on Model/GraphQL integration classes
    // remove when modules are re-organized to provide better isolation
    compileOnly(project(":aws-api"))
}
