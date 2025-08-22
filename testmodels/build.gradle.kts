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
}

android {
    namespace = "com.amplifyframework.testmodels"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":aws-api-appsync"))
    implementation(libs.androidx.core)

    testImplementation(project(":testutils"))
    testImplementation(libs.test.junit)
}
