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
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.amplify.publishing)
}

apply(from = rootProject.file("configuration/checkstyle.gradle"))

android {
    namespace = "com.amplifyframework.predictions.aws"
}

dependencies {
    api(project(":core"))
    api(project(":aws-core"))
    implementation(libs.androidx.appcompat)
    api(platform(libs.aws.bom))
    api(libs.aws.comprehend)
    api(libs.aws.polly)
    api(libs.aws.rekognition)
    api(libs.aws.textract)
    api(libs.aws.translate)
    // Smithy types leak into this module's public API: CredentialsProvider (AWSPredictionsService,
    // PresignedSynthesizeSpeechUrlOptions) and SdkClientConfig (AmazonPollyPresigningClient).
    api(libs.aws.credentials)
    api(libs.aws.smithy.client)
    implementation(libs.kotlin.serializationJson)
    implementation(libs.okhttp)

    testImplementation(libs.bundles.test.unit)
    testImplementation(libs.bundles.test.unit.android)
    testImplementation(project(":testutils"))
    testImplementation(libs.test.mockwebserver)

    androidTestImplementation(project(":testutils"))
    androidTestImplementation(project(":aws-auth-cognito"))
    androidTestImplementation(libs.bundles.test.android)
    androidTestImplementation(libs.test.mockk.android)
}
