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
    alias(libs.plugins.amplify.api)
}

apply(from = rootProject.file("configuration/publishing.gradle"))

fun readVersion() = Properties().run {
    file("../version.properties").inputStream().use { load(it) }
    get("VERSION_NAME").toString()
}

project.setProperty("VERSION_NAME", readVersion())
group = properties["POM_GROUP"].toString()

android {
    namespace = "com.amazonaws.sdk.appsync.events"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
}

dependencies {
    api(project(":aws-sdk-appsync-core"))

    implementation(libs.okhttp)
    implementation(libs.kotlin.serializationJson)
    implementation(libs.kotlin.coroutines)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.kotlin.coroutines)
    testImplementation(libs.test.kotest.assertions)
    testImplementation(libs.test.kotest.assertions.json)
    testImplementation(libs.test.mockwebserver)
    testImplementation(libs.test.turbine)

    androidTestApi(project(":aws-sdk-appsync-amplify"))
    androidTestImplementation(project(":aws-auth-cognito"))
    androidTestImplementation(project(":core-kotlin"))
    androidTestImplementation(project(":testutils"))
    androidTestImplementation(libs.test.androidx.runner)
    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(libs.test.kotlin.coroutines)
    androidTestImplementation(libs.test.kotest.assertions)
    androidTestImplementation(libs.test.turbine)

    androidTestUtil(libs.test.androidx.orchestrator)
}
