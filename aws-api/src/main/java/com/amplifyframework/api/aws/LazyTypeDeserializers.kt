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
package com.amplifyframework.api.aws

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
import com.google.gson.JsonParseException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

const val ITEMS_KEY = "items"
const val NEXT_TOKEN_KEY = "nextToken"

internal class ModelReferenceDeserializer<M : Model>(
    val apiName: String?,
    private val schemaRegistry: AWSApiSchemaRegistry
) :
    JsonDeserializer<ModelReference<M>> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ModelReference<M> {
        val pType = typeOfT as? ParameterizedType
            ?: throw JsonParseException("Expected a parameterized type during list deserialization.")
        val type = pType.actualTypeArguments[0] as Class<M>

        val jsonObject = getJsonObject(json)

        val predicateKeyMap = schemaRegistry
            .getModelSchemaForModelClass(type)
            .primaryIndexFields
            .associateWith { jsonObject[it] }

        if (jsonObject.size() > predicateKeyMap.size) {
            try {
                val preloadedValue = context.deserialize<M>(json, type)
                return LoadedModelReferenceImpl(preloadedValue)
            } catch (e: Exception) {
                // fallback to create lazy
            }
        }
        return ApiLazyModelReference(type, predicateKeyMap, apiName)
    }
}

internal class ModelListDeserializer<M : Model> : JsonDeserializer<ModelList<M>> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ModelList<M> {
        val items = deserializeItems<M>(json, typeOfT, context)
        return ApiLoadedModelList(items)
    }

    companion object {
        @JvmStatic
        fun register(builder: GsonBuilder) {
            builder.registerTypeAdapter(ModelList::class.java, ModelListDeserializer<Model>())
        }
    }
}

internal class ModelPageDeserializer<M : Model> : JsonDeserializer<ModelPage<M>> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ModelPage<M> {
        val items = deserializeItems<M>(json, typeOfT, context)
        val nextToken = deserializeNextToken(json)
        return ApiModelPage(items, nextToken)
    }

    companion object {
        @JvmStatic
        fun register(builder: GsonBuilder) {
            builder.registerTypeHierarchyAdapter(ModelPage::class.java, ModelPageDeserializer<Model>())
        }
    }
}

@Throws(JsonParseException::class)
private fun getJsonObject(json: JsonElement): JsonObject {
    return json as? JsonObject ?: throw JsonParseException(
        "Got a JSON value that was not an object " +
            "Unable to deserialize the response"
    )
}

@Throws(JsonParseException::class)
private fun <M : Model> deserializeItems(
    json: JsonElement,
    typeOfT: Type,
    context: JsonDeserializationContext
): List<M> {
    val pType = typeOfT as? ParameterizedType
        ?: throw JsonParseException("Expected a parameterized type during list deserialization.")
    val type = pType.actualTypeArguments[0]

    val jsonObject = getJsonObject(json)

    val itemsJsonArray = if (jsonObject.has(ITEMS_KEY) && jsonObject.get(ITEMS_KEY).isJsonArray) {
        jsonObject.getAsJsonArray(ITEMS_KEY)
    } else {
        throw JsonParseException(
            "Got JSON from an API call which was supposed to go with a List " +
                "but is in the form of an object rather than an array. " +
                "It also is not in the standard format of having an items " +
                "property with the actual array of data so we do not know how " +
                "to deserialize it."
        )
    }

    return itemsJsonArray.map {
        context.deserialize(it.asJsonObject, type)
    }
}
@Throws(JsonParseException::class)
private fun deserializeNextToken(json: JsonElement): ApiPaginationToken? {
    return getJsonObject(json).get(NEXT_TOKEN_KEY)
        ?.let { if (it.isJsonPrimitive) it.asString else null }
        ?.let { ApiPaginationToken(it) }
}
