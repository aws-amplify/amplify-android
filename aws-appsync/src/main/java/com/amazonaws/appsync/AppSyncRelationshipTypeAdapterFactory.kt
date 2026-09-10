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
package com.amazonaws.appsync

import com.amplifyframework.core.model.LoadedModelReferenceImpl
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelField
import com.amplifyframework.core.model.ModelIdentifier
import com.amplifyframework.core.model.ModelSchema
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

/**
 * Fills in the relationship fields a response did not carry.
 *
 * A relationship the selection set did not ask for is absent from the payload, so the model arrives with
 * that field null. What the absence means depends on the relationship:
 *
 *  - a related model came with no key to fetch it by, so there is nothing to defer: the field becomes a
 *    reference that is already loaded, holding null.
 *  - a related list is found by the parent's own key, which the response does carry, so the field becomes
 *    a list that fetches its pages when the caller asks for one.
 *
 * A field the response did populate holds what the caller asked for, and is left alone.
 *
 * The adapter that would otherwise have read the type is wrapped rather than replaced: the delegate does
 * all of the reading, and only the fields it left null are filled in afterwards.
 */
internal class AppSyncRelationshipTypeAdapterFactory(
    private val loader: AppSyncModelLoader,
    private val schemaRegistry: AppSyncSchemaRegistry
) : TypeAdapterFactory {

    // Derived once per class rather than once per instance: a response carrying a list of models would
    // otherwise reflect over the same class for every item in it.
    private val schemas = ConcurrentHashMap<Class<out Model>, ModelSchema>()

    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T> {
        val delegate = gson.getDelegateAdapter(this, type)

        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) = delegate.write(out, value)

            override fun read(reader: JsonReader): T {
                val value = delegate.read(reader)
                // This is consulted for every type the client reads, so anything that is not a model is
                // handed straight back without a schema being looked at at all.
                (value as? Model)?.let { fillAbsentRelationships(it) }
                return value
            }
        }
    }

    private fun fillAbsentRelationships(parent: Model) {
        val schema = schemaOrNull(parent.javaClass) ?: return

        schema.fields.values.forEach { field ->
            when {
                field.isModelReference -> parent.fillIfAbsent(field.name) { LoadedModelReferenceImpl<Model>() }
                field.isModelList -> parent.fillIfAbsent(field.name) { lazyListFor(parent, field) }
            }
        }
    }

    /**
     * Builds the list that fetches the related models on demand.
     *
     * The query is filtered by the foreign key fields on the child that point back at the parent, which
     * the child's association to the parent names. Those names line up positionally with the parent's
     * identifying values: one for a single primary key, and the partition key followed by the sort keys
     * for a composite one.
     *
     * Null when the list cannot be identified, which leaves the field null.
     */
    private fun lazyListFor(parent: Model, field: ModelField): AppSyncLazyModelList<Model>? {
        val childSchema = schemaRegistry.schemaFor(field.targetType)
        val targetNames = childSchema.associations.values
            // TODO: this picks arbitrarily when a child declares two associations back to the same parent
            //  type, so one of the two lists would be keyed wrongly. The parent field's own association
            //  names the child field via associatedName, which resolves it exactly; needs a fixture with
            //  two such associations to test.
            .firstOrNull { it.associatedType == parent.modelName }
            ?.targetNames
            ?.toList()
            .orEmpty()

        if (targetNames.isEmpty()) {
            throw AppSyncInvalidConfigException(
                message = "${field.targetType} declares no relationship back to ${parent.modelName}, so the " +
                    "related ${field.name} cannot be identified.",
                recoverySuggestion = "Regenerate the models so that ${field.targetType} and " +
                    "${parent.modelName} describe the same relationship."
            )
        }

        // Nothing to filter the child query by, and an unfiltered query would answer with every child
        // rather than this parent's. The field is left null instead, so the absence stays visible.
        val identifiers = parent.identifyingValuesOrNull() ?: return null

        if (targetNames.size > identifiers.size) {
            throw AppSyncInvalidConfigException(
                message = "${field.targetType} points back at ${parent.modelName} through ${targetNames.size} " +
                    "fields, but ${parent.modelName} is identified by ${identifiers.size}.",
                recoverySuggestion = "Regenerate the models so that ${field.targetType} and " +
                    "${parent.modelName} describe the same primary key."
            )
        }

        return AppSyncLazyModelList(childSchema.modelClass, targetNames.zip(identifiers).toMap(), loader)
    }

    /**
     * A type whose schema cannot be derived carries no relationship metadata to act on, so it is handed
     * back as it was read rather than failing a response that is otherwise complete.
     */
    private fun schemaOrNull(modelClass: Class<out Model>): ModelSchema? = schemas[modelClass]
        ?: runCatching { ModelSchema.fromModelClass(modelClass) }
            .getOrNull()
            ?.also { schemas[modelClass] = it }

    companion object {
        fun register(builder: GsonBuilder, loader: AppSyncModelLoader, schemaRegistry: AppSyncSchemaRegistry) {
            builder.registerTypeAdapterFactory(AppSyncRelationshipTypeAdapterFactory(loader, schemaRegistry))
        }
    }
}

/** Writes [newValue] to the named field when it is null, and leaves it alone when it is not. */
private fun Model.fillIfAbsent(name: String, newValue: () -> Any?) {
    val field = javaClass.relationshipField(name) ?: return
    if (field.get(this) != null) return
    newValue()?.let { field.set(this, it) }
}

/**
 * Finds the field a schema entry names, searching up the hierarchy the schema itself was derived from. A
 * name with no field behind it is skipped, since there is nothing to write to.
 */
private fun Class<*>.relationshipField(name: String): Field? = generateSequence(this) { it.superclass }
    .flatMap { it.declaredFields.asSequence() }
    .firstOrNull { it.name == name }
    ?.apply { isAccessible = true }

/**
 * The values that identify this model, in the order a composite key declares them: the partition key
 * first, then the sort keys.
 *
 * Null when a value is missing, which is what a response that did not select the primary key leaves
 * behind. Reading the key is guarded because a model missing it reports that by failing rather than by
 * answering with null.
 */
private fun Model.identifyingValuesOrNull(): List<Any>? {
    val identifier = runCatching { resolveIdentifier() }.getOrNull() ?: return null
    val values = when (identifier) {
        is ModelIdentifier<*> -> listOf(identifier.key()) + identifier.sortedKeys()
        else -> listOf(identifier.toString())
    }
    return if (values.any { it == null }) null else values.filterNotNull()
}
