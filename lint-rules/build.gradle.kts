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

// Custom Android Lint checks. Not published — Lint loads this jar from the build, so it never
// reaches consumers.
plugins {
    alias(libs.plugins.amplify.kotlin)
    // Kover is normally applied by amplify.publishing, which this module deliberately does not use.
    // Applied directly so these tests run in the koverXmlReport gate alongside every other module's.
    alias(libs.plugins.amplify.kover)
}

// The runtime copy of the Lint API comes from AGP, not from this module's classpath, so a version
// that has drifted from AGP compiles cleanly here and then loads against a different runtime —
// which Lint reports as "usually fine" rather than as an error. Fail configuration instead.
val agpVersion = libs.versions.agp.get()
val expectedLintVersion = "${agpVersion.substringBefore('.').toInt() + 23}.${agpVersion.substringAfter('.')}"
check(libs.versions.lint.get() == expectedLintVersion) {
    "lint ${libs.versions.lint.get()} does not match agp $agpVersion (expected $expectedLintVersion)"
}

dependencies {
    // Lint provides this API to checks at runtime. Bundling a second copy breaks check loading.
    compileOnly(libs.lint.api)

    // lint-tests declares its own dependencies at runtime scope only, so the API has to be
    // requested again to compile tests against it.
    testImplementation(libs.lint.api)
    testImplementation(libs.lint.tests)
    testImplementation(libs.test.junit)
}

tasks.jar {
    manifest {
        // How Lint locates the checks in this jar.
        attributes("Lint-Registry-v2" to "com.amplifyframework.lint.AmplifyIssueRegistry")
    }
}
