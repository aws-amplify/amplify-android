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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Converts JSON strings into models of a given type, using Gson.
 */
final class GsonGraphQLResponseFactory implements GraphQLResponse.Factory {
    private final Gson gson;

    /**
     * Default constructor using default Gson object.
     */
    GsonGraphQLResponseFactory() {
        this(
                new GsonBuilder()
                .registerTypeAdapter(List.class, new GsonListDeserializer())
                .create()
        );
    }

    /**
     * Constructor using customized Gson object.
     * @param gson custom Gson object
     */
    GsonGraphQLResponseFactory(Gson gson) {
        this.gson = gson;
    }

    @Override
    public <T> GraphQLResponse<T> buildSingleItemResponse(
            String responseJson,
            Class<T> classToCast
    ) throws ApiException {
        JsonElement jsonData = null;
        JsonElement jsonErrors = null;

        try {
            final JsonObject toJson = JsonParser.parseString(responseJson).getAsJsonObject();
            if (toJson.has("data")) {
                jsonData = skipQueryLevel(toJson.get("data"));
            }
            if (toJson.has("errors")) {
                jsonErrors = toJson.get("errors");
            }
        } catch (JsonParseException jsonParseException) {
            throw new ApiException(
                "Amplify encountered an error while serializing/deserializing an object.",
                jsonParseException,
                AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }

        List<GraphQLResponse.Error> errors = parseErrors(jsonErrors);

        if (jsonData == null || jsonData.isJsonNull()) {
            return new GraphQLResponse<>(null, errors);
        } else if (jsonData.isJsonObject() || jsonData.isJsonPrimitive() || classToCast.equals(JsonElement.class)) {
            T data = parseData(jsonData, classToCast);
            return new GraphQLResponse<>(data, errors);
        } else {
            throw new ApiException(
                "Tried to build a single item GraphQL response object but the JSON data was in the wrong format",
                AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
    }

    public <T> GraphQLResponse<Iterable<T>> buildSingleArrayResponse(
            String responseJson,
            Class<T> classToCast
    ) throws ApiException {
        JsonElement jsonData = null;
        JsonElement jsonErrors = null;

        try {
            final JsonObject toJson = JsonParser.parseString(responseJson).getAsJsonObject();
            if (toJson.has("data")) {
                jsonData = skipQueryLevel(toJson.get("data"));
            }
            if (toJson.has("errors")) {
                jsonErrors = toJson.get("errors");
            }
        } catch (JsonParseException jsonParseException) {
            throw new ApiException(
                    "Amplify encountered an error while serializing/deserializing an object.",
                    jsonParseException,
                    AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }

        List<GraphQLResponse.Error> errors = parseErrors(jsonErrors);

        if (jsonData == null || jsonData.isJsonNull()) {
            return new GraphQLResponse<>(null, errors);
        } else if (
                jsonData.isJsonObject() &&
                jsonData.getAsJsonObject().has("items")
        ) {
            Iterable<T> data = parseDataAsList(jsonData.getAsJsonObject().get("items"), classToCast);
            return new GraphQLResponse<>(data, errors);
        } else if (jsonData.isJsonObject() || jsonData.isJsonPrimitive() || classToCast.equals(JsonElement.class)) {
            T data = parseData(jsonData, classToCast);
            return new GraphQLResponse<>(Collections.singletonList(data), errors);
        } else {
            throw new ApiException(
                    "Tried to build a multi item GraphQL response object but the JSON data was in the wrong format",
                    AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
    }

    // Skips a JSON level to get content of query, not query itself
    private JsonElement skipQueryLevel(JsonElement jsonData) throws ApiException {
        if (jsonData == null || jsonData.isJsonNull()) {
            return null;
        }

        JsonObject data = jsonData.getAsJsonObject();
        if (data.size() == 0) {
            throw new ApiException(
                    "Amplify encountered an error while serializing/deserializing an object.",
                    "Please add a single top level field in your query."
            );
        } else if (data.size() > 1) {
            throw new ApiException(
                    "Amplify encountered an error while serializing/deserializing an object.",
                    "Please reduce your query to a single top level field."
            );
        }

        return data.get(data.keySet().iterator().next());
    }

    @SuppressWarnings("unchecked") // (T) jsonData.toString() *is* checked via isAssignableFrom().
    private <T> T parseData(JsonElement jsonData, Class<T> classToCast) throws ApiException {
        if (jsonData == null || jsonData.isJsonNull()) {
            return null;
        } else if (String.class.isAssignableFrom(classToCast)) {
            return (T) jsonData.toString();
        }

        try {
            return gson.fromJson(jsonData, classToCast);
        } catch (ClassCastException classCastException) {
            throw new ApiException(
                "Failed to parse GraphQL data portion.",
                classCastException,
                AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
    }

    // Cannot use the same TypeToken trick used in parseErrors due to type erasure
    @SuppressWarnings("unchecked") // (T) current.toString() IS CHECKED by String.class.isAssignableFrom(...)
    private <T> Iterable<T> parseDataAsList(JsonElement jsonData, Class<T> classToCast) throws ApiException {
        try {
            ArrayList<T> dataAsList = new ArrayList<>();

            for (JsonElement current : jsonData.getAsJsonArray()) {
                if (String.class.isAssignableFrom(classToCast)) {
                    dataAsList.add((T) current.toString());
                } else {
                    dataAsList.add(gson.fromJson(current, classToCast));
                }
            }
            return dataAsList;
        } catch (ClassCastException classCastException) {
            throw new ApiException(
                    "Failed to parse GraphQL data portion.",
                    classCastException,
                    AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
    }

    @SuppressWarnings("checkstyle:WhitespaceAround")
    private List<GraphQLResponse.Error> parseErrors(JsonElement jsonErrors) throws ApiException {
        if (jsonErrors == null || jsonErrors.isJsonNull()) {
            return Collections.emptyList();
        }

        JsonArray errors = jsonErrors.getAsJsonArray();
        Type listType = new TypeToken<ArrayList<GraphQLResponse.Error>>() {}.getType();

        try {
            return gson.fromJson(errors, listType);
        } catch (ClassCastException classCastException) {
            throw new ApiException(
                    "Failed to parse GraphQL errors.",
                    classCastException,
                    AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
    }
}
