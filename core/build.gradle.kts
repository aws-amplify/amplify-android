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
    id("com.android.library")
    id("kotlin-android")
}

apply(from = rootProject.file("configuration/checkstyle.gradle"))
apply(from = rootProject.file("configuration/publishing.gradle"))

group = properties["POM_GROUP"].toString()

android {
    kotlinOptions {
        moduleName = "com.amplifyframework.core"
    }
}

dependencies {
    implementation(dependency.androidx.v4support)
    implementation(dependency.androidx.annotation)
    implementation(dependency.androidx.nav.fragment)
    implementation(dependency.androidx.nav.ui)
    implementation(dependency.kotlin.serializationJson)

    implementation(dependency.aws.credentials)

    testImplementation(project(":aws-api-appsync"))
    // Used to reference Temporal types in tests.
    testImplementation(project(":testmodels"))
    testImplementation(project(":testutils")) {
        isTransitive = false
    }
    testImplementation(testDependency.junit)
    testImplementation(testDependency.mockito)
    testImplementation(testDependency.robolectric)
    testImplementation(dependency.rxjava)
    testImplementation(testDependency.androidx.test.core)
    testImplementation(testDependency.jsonassert)
    testImplementation(dependency.gson)

    androidTestImplementation(project(":testutils")) {
        isTransitive = false
    }
    androidTestImplementation(dependency.androidx.annotation)
    androidTestImplementation(testDependency.androidx.test.core)
    androidTestImplementation(testDependency.androidx.test.runner)
    androidTestImplementation(testDependency.androidx.test.junit)
    androidTestImplementation(testDependency.androidx.test.espresso)
    androidTestImplementation(testDependency.androidx.test.navigation)
    androidTestImplementation(testDependency.androidx.test.fragment)
}

afterEvaluate {
    // Disables this warning:
    // warning: listOf(classfile) MethodParameters attribute
    // introduced in version 52.0 class files is ignored in
    // version 51.0 class files
    // Root project has -Werror, so this warning
    // would fail the build, otherwise.
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-Xlint:-classfile")
    }
}
