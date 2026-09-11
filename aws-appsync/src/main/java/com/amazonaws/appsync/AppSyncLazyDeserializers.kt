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
import com.amplifyframework.core.model.ModelList
import com.amplifyframework.core.model.ModelPage
import com.amplifyframework.core.model.ModelReference
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Deserializes a related model, which may or may not have arrived with the response.
 *
 * A selection set that requested the related model's fields yields a
 * [com.amplifyframework.core.model.LoadedModelReference] holding it. One that requested only its
 * primary key yields a reference that fetches the model when it is first accessed.
 *
 * A relationship the parent does not have is never seen here: a JSON null becomes a null field before
 * a deserializer is consulted.
 */
internal class AppSyncModelReferenceDeserializer<M : Model>(
    private val loader: AppSyncModelLoader,
    private val schemaRegistry: AppSyncSchemaRegistry
) : JsonDeserializer<ModelReference<M>> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ModelReference<M> {
        val parameterized = typeOfT as? ParameterizedType
            ?: throw AppSyncDeserializationException(
                message = "A related model was requested as ${typeOfT.typeName}, which carries no model type.",
                recoverySuggestion = "Request the relationship as ModelReference<T> so the model type is known."
            )

        @Suppress("UNCHECKED_CAST")
        val modelClass = parameterized.actualTypeArguments.first() as Class<M>
        val jsonObject = json.asJsonObjectOrThrow()
        val keyFields = schemaRegistry.schemaFor(modelClass).primaryIndexFields

        // More fields than the key means the selection set asked for the model itself, so it is already
        // here and there is nothing to defer. A failure to read it is not fatal: the key is still
        // present, so the model can be fetched instead.
        if (jsonObject.size() > keyFields.size) {
            runCatching { context.deserialize<M>(json, modelClass) }
                .onSuccess { return LoadedModelReferenceImpl(it) }
        }

        return AppSyncLazyModelReference(modelClass, jsonObject.keyValues(keyFields), loader)
    }

    /**
     * Reads the primary key values, which are what identify the model in the follow-up query.
     *
     * A key that is incomplete identifies nothing, so it is reported as no key at all — the reference
     * then resolves to null rather than issuing a request that cannot succeed.
     */
    private fun JsonObject.keyValues(keyFields: List<String>): Map<String, Any> {
        val values = keyFields.mapNotNull { field ->
            (get(field) as? JsonPrimitive)?.let { field to it.keyValue() }
        }.toMap()
        return if (values.size == keyFields.size) values else emptyMap()
    }

    /**
     * Unwraps a key so it travels as the type the model declares. The value becomes a query variable,
     * and the JSON wrapper would not serialize as the scalar the variable's type requires.
     */
    private fun JsonPrimitive.keyValue(): Any = when {
        isBoolean -> asBoolean
        isNumber -> asNumber
        else -> asString
    }

    companion object {
        fun register(builder: GsonBuilder, loader: AppSyncModelLoader, schemaRegistry: AppSyncSchemaRegistry) {
            builder.registerTypeAdapter(
                ModelReference::class.java,
                AppSyncModelReferenceDeserializer<Model>(loader, schemaRegistry)
            )
        }
    }
}

/**
 * Deserializes a related list that arrived in full, as `{"items": [...]}`.
 *
 * Produces a [com.amplifyframework.core.model.LoadedModelList], never a lazy one: everything the
 * caller asked for is already in the payload, so there is nothing to defer.
 */
internal class AppSyncModelListDeserializer<M : Model> : JsonDeserializer<ModelList<M>> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext) =
        AppSyncLoadedModelList(deserializeItems<M>(json, typeOfT, context))

    companion object {
        fun register(builder: GsonBuilder) {
            builder.registerTypeAdapter(ModelList::class.java, AppSyncModelListDeserializer<Model>())
        }
    }
}

/**
 * Deserializes one page of a paginated list, as `{"items": [...], "nextToken": "..."}`.
 */
internal class AppSyncModelPageDeserializer<M : Model> : JsonDeserializer<ModelPage<M>> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext) = AppSyncModelPage(
        items = deserializeItems<M>(json, typeOfT, context),
        nextToken = deserializeNextToken(json)
    )

    companion object {
        fun register(builder: GsonBuilder) {
            // A hierarchy adapter, unlike the list above: a response type may be a subtype of ModelPage,
            // and an exact-type adapter would not be consulted for one.
            builder.registerTypeHierarchyAdapter(ModelPage::class.java, AppSyncModelPageDeserializer<Model>())
        }
    }
}

/**
 * Reads the `items` array, deserializing each entry as the list's element type.
 *
 * The element type comes from the requested type's parameter, so the caller's `ModelList<Todo>` is what
 * decides how each item is read.
 */
private fun <M : Model> deserializeItems(
    json: JsonElement,
    typeOfT: Type,
    context: JsonDeserializationContext
): List<M> {
    val parameterized = typeOfT as? ParameterizedType
        ?: throw AppSyncDeserializationException(
            message = "A related list was requested as ${typeOfT.typeName}, which carries no element type.",
            recoverySuggestion = "Request the list as ModelList<T> or ModelPage<T> so the item type is known."
        )
    val elementType = parameterized.actualTypeArguments.first()

    val items = json.asJsonObjectOrThrow().get(ITEMS_KEY)
        ?.takeIf { it.isJsonArray }
        ?: throw AppSyncDeserializationException(
            message = "A related list was expected to carry an \"$ITEMS_KEY\" array, but did not.",
            recoverySuggestion = "Verify the selection set requests the list's items."
        )

    return items.asJsonArray.map { context.deserialize(it.asJsonObject, elementType) }
}

/** Reads `nextToken`, absent when the page is the last one. */
private fun deserializeNextToken(json: JsonElement): AppSyncPaginationToken? =
    json.asJsonObjectOrThrow().get(NEXT_TOKEN_KEY)
        ?.takeIf { it.isJsonPrimitive }
        ?.let { AppSyncPaginationToken(it.asString) }

private fun JsonElement.asJsonObjectOrThrow(): JsonObject = this as? JsonObject
    ?: throw AppSyncDeserializationException(
        message = "A relationship was expected to be a JSON object, but was ${this::class.simpleName}."
    )

private const val ITEMS_KEY = "items"
private const val NEXT_TOKEN_KEY = "nextToken"
