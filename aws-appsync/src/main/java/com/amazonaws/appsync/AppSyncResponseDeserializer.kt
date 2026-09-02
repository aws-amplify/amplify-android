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

import com.amplifyframework.api.aws.AppSyncGraphQLRequest
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.util.TypeMaker
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Turns an AppSync JSON response body into a typed [GraphQLResponse].
 *
 * A private reimplementation of the plugin's `GsonGraphQLResponseFactory`, minus the lazy-model
 * deserializers. Unlike the plugin's version this reports failure through
 * [AppSyncDeserializationException] rather than `ApiException`.
 */
internal object AppSyncResponseDeserializer {

    private const val ITEMS_KEY = "items"
    private const val NEXT_TOKEN_KEY = "nextToken"

    /**
     * Deserializes [responseJson] into a [GraphQLResponse] of the request's response type.
     *
     * @throws AppSyncDeserializationException if the body is empty or cannot be parsed.
     */
    fun <T> deserialize(request: GraphQLRequest<T>, responseJson: String?): GraphQLResponse<T> {
        // Gson returns null rather than throwing for an empty string, so this is checked up front.
        // See https://github.com/google/gson/issues/457
        if (responseJson.isNullOrEmpty()) {
            throw AppSyncDeserializationException(
                message = "The response body was empty, so it could not be deserialized.",
                cause = JsonParseException("Empty response.")
            )
        }

        val responseType = TypeMaker.getParameterizedType(GraphQLResponse::class.java, request.responseType)

        return try {
            AppSyncGson.instance.newBuilder()
                .registerTypeHierarchyAdapter(Iterable::class.java, IterableDeserializer(request))
                .create()
                .fromJson(responseJson, responseType)
        } catch (error: JsonParseException) {
            throw AppSyncDeserializationException(
                message = "The response could not be deserialized into ${request.responseType}.",
                cause = error
            )
        }
    }

    /**
     * Deserializes AppSync's list shape. At the root of a query the result becomes a
     * [PaginatedResult] carrying the request for the next page; below the root it is a plain list,
     * because that is what codegen emits for a one-to-many relationship.
     */
    private class IterableDeserializer<R>(
        private val request: GraphQLRequest<R>
    ) : JsonDeserializer<Iterable<Any>> {

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Iterable<Any> {
            val parameterized = typeOfT as? ParameterizedType
                ?: throw JsonParseException("Expected a parameterized type during list deserialization.")
            val templateType = parameterized.actualTypeArguments[0]

            when {
                json.isJsonObject -> {
                    val jsonObject = json.asJsonObject
                    if (!jsonObject.has(ITEMS_KEY) || !jsonObject.get(ITEMS_KEY).isJsonArray) {
                        throw JsonParseException(
                            "Expected a JSON array, or an object with an '$ITEMS_KEY' array, for a list " +
                                "response. Got neither, so it cannot be deserialized."
                        )
                    }
                    val items = toList(jsonObject.get(ITEMS_KEY).asJsonArray, templateType, context)
                    return if (PaginatedResult::class.java == parameterized.rawType) {
                        buildPaginatedResult(items, jsonObject.get(NEXT_TOKEN_KEY))
                    } else {
                        // A nextToken may be present below the root, but codegen models the field as
                        // a plain List, so there is nowhere to surface it.
                        items
                    }
                }
                json.isJsonArray -> return toList(json.asJsonArray, templateType, context)
                else -> throw JsonParseException(
                    "Expected a JSON object or array to deserialize into an Iterable."
                )
            }
        }

        private fun toList(jsonArray: JsonArray, type: Type, context: JsonDeserializationContext): List<Any> =
            jsonArray.map { context.deserialize(it, type) }

        private fun buildPaginatedResult(items: Iterable<Any>, nextTokenElement: JsonElement?): PaginatedResult<Any> {
            val nextToken = nextTokenElement?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.asString
            val appSyncRequest = request as? AppSyncGraphQLRequest<R>

            val requestForNextPage = if (nextToken != null && appSyncRequest != null) {
                try {
                    appSyncRequest.newBuilder()
                        .variable(NEXT_TOKEN_KEY, "String", nextToken)
                        .build<PaginatedResult<Any>>()
                } catch (error: Exception) {
                    throw JsonParseException("Could not build the request for the next page.", error)
                }
            } else {
                null
            }

            return PaginatedResult<Any>(items, requestForNextPage)
        }
    }
}
