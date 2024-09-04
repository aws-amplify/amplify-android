import java.util.Properties

plugins {
    id("java-library")
    id("maven-publish")
    alias(libs.plugins.kotlin.jvm)
}

java {
    withSourcesJar()
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
