/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Properties

plugins {
    alias(libs.plugins.amplify.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.amplify.publishing)
}

fun readVersion() = Properties().run {
    file("../version.properties").inputStream().use { load(it) }
    get("VERSION_NAME").toString()
}

project.setProperty("VERSION_NAME", readVersion())

android {
    namespace = "com.amazonaws.sdk.appsync.events"
}

dependencies {
    api(project(":aws-sdk-appsync-core"))

    // These types appear in this module's public API (OkHttpClient, kotlinx.serialization
    // serializers, Flow), so declare them as api rather than relying on transitive resolution.
    api(libs.okhttp)
    api(libs.kotlin.serializationJson)
    api(libs.kotlin.coroutines)

    testImplementation(libs.bundles.test.unit)
    testImplementation(libs.test.kotest.assertions.json)
    testImplementation(libs.test.mockwebserver)

    androidTestApi(project(":aws-sdk-appsync-amplify"))
    androidTestImplementation(project(":aws-auth-cognito"))
    androidTestImplementation(project(":core-kotlin"))
    androidTestImplementation(project(":testutils"))
    androidTestImplementation(libs.bundles.test.android)
}
