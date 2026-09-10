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

// This module holds the project's custom Android Lint checks. It is intentionally not published,
// and so does not apply amplify.publishing, amplify.api, or amplify.kover.
plugins {
    alias(libs.plugins.amplify.kotlin)
}

dependencies {
    // Lint provides this API to checks at runtime. Bundling a second copy breaks check loading.
    compileOnly(libs.lint.api)

    testImplementation(libs.lint.tests)
    testImplementation(libs.test.junit)
}

tasks.jar {
    manifest {
        // How Lint locates the checks in this jar.
        attributes("Lint-Registry-v2" to "com.amplifyframework.lint.AmplifyIssueRegistry")
    }
}
