import com.android.build.api.dsl.Lint
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

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

internal val amplifyInternalMarkers = listOf(
    "com.amplifyframework.annotations.InternalApiWarning",
    "com.amplifyframework.annotations.InternalAmplifyApi",
    "com.amplifyframework.annotations.AmplifyFlutterApi"
)

internal val optInAnnotations = amplifyInternalMarkers + listOf(
    "com.amplifyframework.annotations.ExperimentalAmplifyApi"
)

internal val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * The Lint settings shared by every module. Both the Android plugins and the standalone
 * `com.android.lint` plugin expose this same [Lint] interface, so one helper serves all of them.
 */
internal fun Project.configureLint(lint: Lint) = lint.apply {
    lintConfig = rootProject.file("lint.xml")
    warningsAsErrors = true
    abortOnError = true
    enable += listOf("UnusedResources")
    disable += listOf(
        "GradleDependency",
        "NewerVersionAvailable",
        "AndroidGradlePluginVersion",
        "CredentialDependency"
    )
}
