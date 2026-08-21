/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
    namespace = "com.amplifyframework.cloudwatch"

    // Persist app data across connected test methods so the client's integration tests keep a stable device
    // id and write to a single CloudWatch stream (one per test otherwise). Overrides the shared convention's
    // clearPackageData=true; the module's own DB instrumentation test self-cleans in @After.
    defaultConfig {
        testInstrumentationRunnerArguments["clearPackageData"] = "false"
    }
}

dependencies {
    api(project(":foundation"))
    api(project(":foundation-bridge"))

    api(platform(libs.aws.bom))
    api(libs.aws.cloudwatchlogs)
    implementation(libs.aws.http)
    implementation(libs.androidx.workmanager)
    implementation(libs.kotlin.futures)
    implementation(libs.kotlin.coroutines)
    implementation(libs.sqlcipher)
    implementation(libs.androidx.sqlite)

    testImplementation(libs.bundles.test.unit)
    testImplementation(libs.bundles.test.unit.android)
    testImplementation(libs.test.androidx.workmanager)
    testImplementation(project(":testutils"))

    androidTestImplementation(libs.bundles.test.android)
    androidTestImplementation(project(":testutils"))
    // Integration tests obtain guest AWS credentials via Cognito (identity pool) and Amplify.configure.
    androidTestImplementation(project(":core"))
    androidTestImplementation(project(":aws-core"))
    androidTestImplementation(project(":aws-auth-cognito"))
}
