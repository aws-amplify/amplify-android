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

    /**
     * Returns the full path of the property, e.g. `"post.author.name"`.
     *
     * @param includesRoot whether it should include the root name or not. It's `false` by default.
     * @return path as a string
     */
    fun getKeyPath(includesRoot: Boolean = false): String {
        var metadata = getMetadata()
        val path = mutableListOf<String>()
        while (metadata.parent != null) {
            path.add(index = 0, element = metadata.name)
            metadata = metadata.parent!!.getMetadata()
        }
        if (includesRoot) {
            path.add(index = 0, metadata.name)
        }
        return path.joinToString(separator = ".")
    }
}

/**
 * Runtime information about a property. Its `name` and `parent` property reference,
 * as well as whether the property represents a collection of the type or not.
 */
data class PropertyPathMetadata(
    val name: String,
    val isCollection: Boolean = false,
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
     *
     */
    fun getModelType(): Class<Model>

}

/**
 * Represents the `Model` structure itself, a container of property references.
 *
 * ```kotlin
 * class Path(name: String = "root", isCollection: Boolean = false, parent: PropertyPath? = null)
 *    : ModelPath<Post>(name, isCollection, modelType = Post::class.java, parent = parent) {
 *
 *    val id = string("id")
 *    val title = string("title")
 *    val blog by lazy { Blog.Path("blog", parent = this) }
 *    val comments by lazy { Comment.Path("comments", isCollection = true, parent = this) }
 *  }
 *
 *  companion object {
 *    val rootPath = Path()
 *  }
 * ```
 */
open class ModelPath<ModelType : Model>(
    private val name: String,
    private val isCollection: Boolean = false,
    private val parent: PropertyPath? = null,
    private val modelType: Class<ModelType>
) : PropertyContainerPath {

    override fun getMetadata() = PropertyPathMetadata(
        name = name,
        isCollection = isCollection,
        parent = parent
    )

    override fun getModelType(): Class<Model> = modelType as Class<Model>

    protected fun <T : Any> field(name: String, type: Class<T>) = FieldPath(
        name = name,
        parent = this,
        propertyType = type
    )

    protected inline fun <reified T : Any> field(name: String) = FieldPath(
        name = name,
        parent = this,
        propertyType = T::class.java
    )

    protected fun string(name: String) = field<String>(name)

    protected fun integer(name: String) = field<Int>(name)

    protected fun double(name: String) = field<Double>(name)

    protected fun boolean(name: String) = field<Boolean>(name)

    protected inline fun <reified E : Enum<*>> enumeration(name: String) = FieldPath(
        name = name,
        parent = this,
        propertyType = E::class.java
    )

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
        @JvmStatic
        fun <M : Model, P : ModelPath<M>>getRootPath(clazz: Class<M>): P {
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
 * Represents a scalar (i.e. data type) of a model property.
 */
class FieldPath<Type : Any>(
    private val name: String,
    private val parent: PropertyPath? = null,
    val propertyType: Class<Type>
) : PropertyPath {

    override fun getMetadata() = PropertyPathMetadata(
        name = name,
        parent = parent
    )

}

/**
 * Function used to define which associations are included in the selection set
 * in an idiomatic manner. It's a simple delegation to `arrayOf` with the main
 * goal of improved code readability.
 *
 * Example:
 *
 * ```kotlin
 * getById<Post>("id") { includes(it.comments) }
 * ```
 *
 * @param associations the associations that should be included
 * @return the passed associations as an array
 */
fun includes(vararg associations: PropertyContainerPath) = listOf(*associations)
