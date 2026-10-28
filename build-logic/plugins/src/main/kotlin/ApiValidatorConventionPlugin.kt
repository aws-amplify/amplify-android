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

import kotlinx.validation.ApiValidationExtension
import kotlinx.validation.KotlinApiBuildTask
import kotlinx.validation.KotlinApiCompareTask
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * This plugin configures the [Binary Compatibility Validator](https://github.com/Kotlin/binary-compatibility-validator)
 * to ensure that we don't change public API surface unintentionally.
 */
class ApiValidatorConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlinx.binary-compatibility-validator")

            extensions.configure<ApiValidationExtension> {
                // Ignore anything marked with an internal API marker
                nonPublicMarkers += amplifyInternalMarkers
                nonPublicMarkers += "androidx.annotation.VisibleForTesting"
            }

            // The validator registers its tasks only for projects that apply a Kotlin plugin of
            // their own. Android modules get Kotlin from AGP instead, so without this they get no
            // apiCheck or apiDump and the dumps in their api directory go unvalidated. Drive the
            // validator's own task types off the release variant's compiled classes instead.
            pluginManager.withPlugin("com.android.library") { registerAndroidApiTasks() }
        }
    }

    private fun Project.registerAndroidApiTasks() {
        // The validator adds ASM to the classpath its worker runs on, but adds the metadata reader
        // that determines Kotlin visibility only for projects applying a Kotlin plugin.
        dependencies.add(WORKER_CLASSPATH, libs.findLibrary("kotlin-metadata-jvm").get())
        // Captured here because inside the registration blocks the receiver is the task, whose own
        // name would shadow the project's
        val moduleName = name
        val dumpFile = layout.projectDirectory.file("api/$moduleName.api")

        val apiBuild = tasks.register<KotlinApiBuildTask>("apiBuild") {
            // 'group' is deliberately unset so this stays hidden from ./gradlew tasks
            description = "Builds the public API of $moduleName. Complementary task, don't call it directly"
            inputClassesDirs.from(tasks.named("compileReleaseKotlin").map { it.outputs.files })
            inputClassesDirs.from(tasks.named("compileReleaseJavaWithJavac").map { it.outputs.files })
            outputApiFile.set(layout.buildDirectory.file("api/$moduleName.api"))
            runtimeClasspath.from(configurations.named(WORKER_CLASSPATH_RESOLVER))
        }

        val apiCheck = tasks.register<KotlinApiCompareTask>("apiCheck") {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Checks the public API of $moduleName against the dump in its api directory"
            projectApiFile.set(dumpFile)
            generatedApiFile.set(apiBuild.flatMap { it.outputApiFile })
        }

        tasks.register<ApiDumpTask>("apiDump") {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Updates the dump in the api directory with the public API of $moduleName"
            generatedApiFile.set(apiBuild.flatMap { it.outputApiFile })
            committedApiFile.set(dumpFile)
        }

        tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { dependsOn(apiCheck) }
    }

    private companion object {
        // Configurations the validator creates to run its worker with
        const val WORKER_CLASSPATH = "bcv-rt-jvm-cp"
        const val WORKER_CLASSPATH_RESOLVER = "bcv-rt-jvm-cp-resolver"
    }
}

/**
 * Copies a freshly built API dump over the one in the api directory. Gradle's Copy and Sync tasks
 * take a destination directory rather than a single file, which would make the whole api directory
 * an output of this task and clash with apiCheck reading its dump from there.
 */
internal abstract class ApiDumpTask : DefaultTask() {
    @get:InputFile
    abstract val generatedApiFile: RegularFileProperty

    @get:OutputFile
    abstract val committedApiFile: RegularFileProperty

    @TaskAction
    fun dump() {
        generatedApiFile.get().asFile.copyTo(committedApiFile.get().asFile, overwrite = true)
    }
}
