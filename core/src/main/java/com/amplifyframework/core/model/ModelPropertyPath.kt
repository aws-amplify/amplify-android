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

package com.amplifyframework.core.model

import com.amplifyframework.annotations.InternalAmplifyApi

/**
 * Represents a property of a `Model`. PropertyPath is a way of representing the
 * structure of a model with static typing, so developers can reference model
 * properties in queries and other functionality that require them.
 */
interface PropertyPath {

    /**
     * Access the property metadata.
     *
     * **Implementation note:** this function is in place over an implicit accessor over
     * a property named `metadata` in order to avoid name conflict with the actual property
     * names that will get generate from the `Model`.
     *
     * @return the property metadata, that contains the name and a reference to its parent.
     */
    fun getMetadata(): PropertyPathMetadata
}

/**
 * Runtime information about a property. Its `name` and `parent` property reference,
 * as well as whether the property represents a collection of the type or not.
 */
data class PropertyPathMetadata internal constructor(
    /**
     * Name of node path
     */
    val name: String,

    /**
     * Whether or not the path is a collection
     */
    val isCollection: Boolean = false,

    /**
     * Parent node path, if any
     */
    val parent: PropertyPath? = null
)

/**
 * This interface is used to mark a property path as being a container
 * for other properties.
 *
 * @see ModelPath for a more concrete representation of a property container
 */
interface PropertyContainerPath : PropertyPath {

    /**
     * Returns the model type of the property container.
     */
    fun getModelType(): Class<Model>
}

/**
 * Represents the `Model` structure itself, a container of property references.
 */
open class ModelPath<ModelType : Model> protected constructor(
    private val name: String,
    private val isCollection: Boolean = false,
    private val parent: PropertyPath? = null,
    private val modelType: Class<ModelType>
) : PropertyContainerPath {

    @InternalAmplifyApi
    override fun getMetadata() = PropertyPathMetadata(
        name = name,
        isCollection = isCollection,
        parent = parent
    )

    @InternalAmplifyApi
    override fun getModelType(): Class<Model> = modelType as Class<Model>

    companion object {

        /**
         * Attempts to get a reference to the root property path of a given model
         * of type `M`. This uses reflection to allow models created before the
         * `PropertyPath` type was added to continue working without disruption
         * of the development workflow.
         *
         * @return the `P : ModelPath<M>`
         * @throws ModelException.PropertyPathNotFound in case the path could not be read or found.
         */
        @Throws(ModelException.PropertyPathNotFound::class)
        @InternalAmplifyApi
        fun <M : Model, P : ModelPath<M>> getRootPath(clazz: Class<M>): P {
            val field = try {
                clazz.getDeclaredField("rootPath")
            } catch (e: NoSuchFieldException) {
                throw ModelException.PropertyPathNotFound(clazz.simpleName)
            }
            field.isAccessible = true
            val path = field.get(null) as? P
            return path ?: throw ModelException.PropertyPathNotFound(clazz.simpleName)
        }
    }
}

/**
 * Function used to define which relationships are included in the selection set
 * in an idiomatic manner. It's a simple delegation to `listOf` with the main
 * goal of improved code readability.
 *
 * Example:
 *
 * ```kotlin
 * ModelQuery.get<Post, PostPath>(Post::class.java, "id") { postPath ->
 *   includes(postPath.comments)
 * }
 * ```
 *
 * @param relationships the relationships that should be included
 * @return the passed associations as an array
 */
fun includes(vararg relationships: PropertyContainerPath) = listOf(*relationships)
