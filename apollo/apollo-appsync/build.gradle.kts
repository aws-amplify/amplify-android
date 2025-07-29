/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
    alias(libs.plugins.amplify.kotlin)
    id("maven-publish")
}

java {
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(17)
}

fun readVersion() = Properties().run {
    file("../version.properties").inputStream().use { load(it) }
    get("VERSION_NAME").toString()
}

project.setProperty("VERSION_NAME", readVersion())

apply(from = rootProject.file("configuration/publishing.gradle"))

val packageInfoGenerator by tasks.registering {
    val constantsDir = project.layout.buildDirectory.dir("generated/sources/constants/java")
    val outputFile = constantsDir.get().file("com/amplifyframework/apollo/appsync/util/PackageInfo.kt").asFile
    inputs.property("version", version)
    outputs.dir(constantsDir)
    doLast {
        outputFile.parentFile.mkdirs()
        val properties = inputs.properties
        val version by properties
        outputFile.writeText(
            """package com.amplifyframework.apollo.appsync.util
                |
                |internal object PackageInfo {
                |    const val version = "$version"
                |}
                |
            """.trimMargin()
        )
    }
}

sourceSets.main {
    java.srcDir(packageInfoGenerator)
}

dependencies {
    api(libs.apollo.runtime)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.kotlin.coroutines)
    testImplementation(libs.test.kotest.assertions)
}
