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
    alias(libs.plugins.amplify.publishing)
}

apply(from = rootProject.file("configuration/checkstyle.gradle"))

android {
    namespace = "com.amplifyframework.api.aws"
}

dependencies {
    api(project(":core"))
    implementation(project(":aws-core"))
    api(project(":aws-api-appsync"))

    implementation(libs.androidx.appcompat)
    api(platform(libs.aws.bom))
    api(libs.aws.signing)
    // Smithy types leak into this module's public API: CredentialsProvider (ApiAuthProviders,
    // IamRequestDecorator) and HttpRequest (AWS4Signer.sign).
    api(libs.aws.credentials)
    api(libs.aws.smithy.http)
    implementation(libs.gson)
    api(libs.okhttp)

    testImplementation(libs.bundles.test.unit)
    testImplementation(libs.bundles.test.unit.android)
    testImplementation(project(":testutils"))
    testImplementation(project(":testmodels"))
    testImplementation(libs.test.jsonassert)
    testImplementation(libs.test.mockito.core)
    testImplementation(libs.test.mockwebserver)

    androidTestImplementation(libs.bundles.test.android)
    androidTestImplementation(project(":testutils"))
    androidTestImplementation(project(":testmodels"))
    androidTestImplementation(project(":aws-auth-cognito"))
    androidTestImplementation(project(":core-kotlin"))
    androidTestImplementation(libs.rxjava)
}
