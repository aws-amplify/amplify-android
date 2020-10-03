/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws;

import androidx.annotation.VisibleForTesting;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.util.GsonFactory;
import com.amplifyframework.util.TypeMaker;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts JSON strings into models of a given type, using Gson.
 */
final class GsonGraphQLResponseFactory implements GraphQLResponse.Factory {
    private final Gson gson;

    GsonGraphQLResponseFactory() {
        this(GsonFactory.instance());
    }

    @VisibleForTesting
    GsonGraphQLResponseFactory(Gson gson) {
        this.gson = gson;
    }

    @Override
    public <T> GraphQLResponse<T> buildResponse(GraphQLRequest<T> request, String responseJson, Type typeOfT)
            throws ApiException {
        Type responseType = TypeMaker.getParameterizedType(GraphQLResponse.class, typeOfT);
        try {
            Gson responseGson = gson.newBuilder()
                .registerTypeHierarchyAdapter(Iterable.class, new IterableDeserializer<>(request))
                .create();
            return responseGson.fromJson(responseJson, responseType);
        } catch (JsonSyntaxException jsonSyntaxException) {
            throw new ApiException(
                "Amplify encountered an error while deserializing an object.",
                jsonSyntaxException,
                AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
    }

    static final class IterableDeserializer<R> implements JsonDeserializer<Iterable<Object>> {
        private static final String ITEMS_KEY = "items";
        private static final String NEXT_TOKEN_KEY = "nextToken";

        private final GraphQLRequest<R> request;

        IterableDeserializer(GraphQLRequest<R> request) {
            this.request = request;
        }

        @Override
        public Iterable<Object> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            final Type templateClassType;
            if (typeOfT instanceof ParameterizedType) {
                templateClassType = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
            } else {
                throw new JsonParseException("Expected a parameterized type during list deserialization.");
            }

            // If the json we got is not really a list and the list has a generics type...
            if (json.isJsonObject()) {
                /*
                 * Parses JSON such as the following:
                 *   {
                 *      "items" : [
                 *          {
                 *              "description": null,
                 *              "id": "92863611-684a-424d-b3e5-94d42c4914c9",
                 *              "name": "some name"
                 *          }
                 *      ],
                 *      "nextToken" : "some_next_token"
                 *   }
                 */
                JsonObject jsonObject = json.getAsJsonObject();
                // ...and it is in the format we expect from AppSync
                // for a list of objects in a relationship
                if (jsonObject.has(ITEMS_KEY) && jsonObject.get(ITEMS_KEY).isJsonArray()) {
                    JsonArray itemsArray = jsonObject.get(ITEMS_KEY).getAsJsonArray();
                    Iterable<Object> items = toList(itemsArray, templateClassType, context);
                    if (PaginatedResult.class.equals(((ParameterizedType) typeOfT).getRawType())) {
                        // Results of a GraphQL query at the root level are parsed into a PaginatedResult.
                        // A PaginatedResult extends the Iterable class, augmenting it with knowledge
                        // of whether a next page exists, and how to request that next page
                        // (via the nextToken).
                        return buildPaginatedResult(items, jsonObject.get(NEXT_TOKEN_KEY));
                    } else {
                        // Results below than the root level are parsed as a List, because that
                        // is the type on the code generated model for a one to many relationship
                        // to a list of objects.  For this case, a nextToken may be present,
                        // but we currently ignore it.  In the future, we could update the
                        // generated model to use a PaginatedResult instead of List,
                        // which would expose these details for customers.
                        return items;
                    }
                } else {
                    throw new JsonParseException(
                        "Got JSON from an API call which was supposed to go with a List " +
                            "but is in the form of an object rather than an array. " +
                            "It also is not in the standard format of having an items " +
                            "property with the actual array of data so we do not know how " +
                            "to deserialize it."
                    );
                }
            } else if (json.isJsonArray()) {
                return toList(json.getAsJsonArray(), templateClassType, context);
            }
            throw new JsonParseException(
                    "Got a JSON value that was not an object or a list. " +
                            "Refusing to deserialize into a Java Iterable."
            );
        }

        private Iterable<Object> toList(JsonArray jsonArray, Type type, JsonDeserializationContext context) {
            final List<Object> items = new ArrayList<>();
            for (JsonElement item : jsonArray) {
                items.add(context.deserialize(item, type));
            }
            return items;
        }

        private PaginatedResult<Object> buildPaginatedResult(Iterable<Object> items, JsonElement nextTokenElement) {
            GraphQLRequest<PaginatedResult<Object>> requestForNextPage = null;
            if (nextTokenElement.isJsonPrimitive()) {
                String nextToken = nextTokenElement.getAsJsonPrimitive().getAsString();
                try {
                    if (request instanceof AppSyncGraphQLRequest) {
                        requestForNextPage = ((AppSyncGraphQLRequest<R>) request).newBuilder()
                                .variable(NEXT_TOKEN_KEY, "String", nextToken)
                                .build();
                    }
                } catch (AmplifyException exception) {
                    throw new JsonParseException(
                        "Failed to create requestForNextPage with nextToken variable",
                        exception
                    );
                }
            }
            return new PaginatedResult<>(items, requestForNextPage);
        }
    }
}
