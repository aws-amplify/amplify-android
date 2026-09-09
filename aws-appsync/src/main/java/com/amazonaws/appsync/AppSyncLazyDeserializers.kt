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

import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelList
import com.amplifyframework.core.model.ModelPage
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

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
        message = "A related list was expected to be a JSON object, but was ${this::class.simpleName}."
    )

private const val ITEMS_KEY = "items"
private const val NEXT_TOKEN_KEY = "nextToken"
