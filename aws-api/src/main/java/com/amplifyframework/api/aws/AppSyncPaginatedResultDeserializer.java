/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.core.model.Model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Takes an input such as the following and deserializes to an AppSyncPage.
 * {
 *   "items": [
 *     {
 *       "description": null,
 *       "id": "92863611-684a-424d-b3e5-94d42c4914c9",
 *       "name": "some task"
 *     }
 *   ],
 *   "nextToken": "asdf"
 * }
 */
final class AppSyncPaginatedResultDeserializer implements JsonDeserializer<PaginatedResult<Model>> {
    private static final String ITEMS_KEY = "items";
    private static final String NEXT_TOKEN_KEY = "nextToken";

    private final GraphQLRequest<PaginatedResult<Model>> request;

    AppSyncPaginatedResultDeserializer(GraphQLRequest<PaginatedResult<Model>> request) {
        this.request = request;
    }

    @Override
    @SuppressWarnings("unchecked") // Cast Type to Class<Model>
    public PaginatedResult<Model> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context
    ) throws JsonParseException {
        final Class<Model> templateClassType;
        if (typeOfT instanceof ParameterizedType) {
            // Because typeOfT is ParameterizedType we can be sure this is a safe cast.
            templateClassType = (Class<Model>) ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
        } else {
            throw new JsonParseException("Expected a parameterized type during AppSyncPage deserialization.");
        }
        if (!json.isJsonObject()) {
            throw new JsonParseException("Expected JsonObject while deserializing AppSyncPage but found " + json);
        }
        JsonObject jsonObject = json.getAsJsonObject();

        Type dataType = TypeMaker.getParameterizedType(Iterable.class, templateClassType);
        Iterable<Model> items = context.deserialize(jsonObject.get(ITEMS_KEY), dataType);

        JsonElement nextTokenElement = jsonObject.get(NEXT_TOKEN_KEY);
        GraphQLRequest<PaginatedResult<Model>> requestForNextPage = null;
        if (nextTokenElement.isJsonPrimitive()) {
            String nextToken = nextTokenElement.getAsJsonPrimitive().getAsString();
            requestForNextPage = request.copy();
            requestForNextPage.putVariable(NEXT_TOKEN_KEY, nextToken);
        }

        return new AppSyncPaginatedResult<>(items, requestForNextPage);
    }
}
