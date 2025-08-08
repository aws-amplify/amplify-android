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
    alias(libs.plugins.amplify.kotlin)
    id("maven-publish")
}

java {
    withSourcesJar()
    withJavadocJar()
}

apply(from = rootProject.file("configuration/publishing.gradle"))

group = properties["POM_GROUP"].toString()

dependencies {
    implementation(libs.kotlin.stdlib)
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
