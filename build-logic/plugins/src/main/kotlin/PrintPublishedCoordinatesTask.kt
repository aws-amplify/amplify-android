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

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Prints the Maven coordinates (group:artifact:version) this project publishes, one per line.
 *
 * The coordinates are an input property rather than something the action reads off the publishing
 * extension: Gradle resolves input properties while writing the configuration cache entry, so the
 * action never holds a reference to the publication model, which cannot be serialized.
 */
abstract class PrintPublishedCoordinatesTask : DefaultTask() {
    @get:Input
    abstract val coordinates: ListProperty<String>

    @TaskAction
    fun printCoordinates() = coordinates.get().forEach { logger.quiet(it) }
}
