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
    alias(libs.plugins.amplify.android.library)
    alias(libs.plugins.amplify.publishing)
}

apply(from = rootProject.file("configuration/checkstyle.gradle"))

android {
    namespace = "com.amplifyframework.aws.core"
}

dependencies {
    api(project(":core"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.coroutines)

    api(platform(libs.aws.bom))
    implementation(libs.aws.smithy.http)
    compileOnly(libs.aws.smithy.okhttp4)

    api(libs.aws.credentials)
    // Smithy runtime-core types (Instant, Attributes) appear in this module's public API
    // (e.g. AWSTemporaryCredentials, CognitoCredentialsProvider), so declare it directly as api
    // rather than relying on transitive resolution.
    api(libs.aws.smithy.runtime.core)
    // slf4j dependency is added to fix https://github.com/awslabs/aws-sdk-kotlin/issues/993#issuecomment-1678885524
    implementation(libs.slf4j)

    testImplementation(libs.bundles.test.unit)
    testImplementation(libs.bundles.test.unit.android)
}
