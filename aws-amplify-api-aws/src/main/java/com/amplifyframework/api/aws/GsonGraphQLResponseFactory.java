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

import android.util.Log;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLResponse;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
        this(new Gson());
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
            throw new ApiException.ObjectSerializationException(jsonParseException);
        }

        List<GraphQLResponse.Error> errors = parseErrors(jsonErrors);

        if (jsonData == null || jsonData.isJsonNull()) {
            return new GraphQLResponse<>(null, errors);
        } else if (jsonData.isJsonObject() || jsonData.isJsonPrimitive() || classToCast.equals(JsonElement.class)) {
            T data = parseData(jsonData, classToCast);
            return new GraphQLResponse<>(data, errors);
        } else {
            throw new ApiException("Tried to build a single item GraphQL response object but " +
                    "the JSON data was in the wrong format");
        }
    }

    public <T> GraphQLResponse<List<T>> buildSingleArrayResponse(
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
            throw new ApiException.ObjectSerializationException(jsonParseException);
        }

        List<GraphQLResponse.Error> errors = parseErrors(jsonErrors);

        Log.i("TESTAPP", jsonData.toString());

        if (jsonData == null || jsonData.isJsonNull()) {
            return new GraphQLResponse<>(null, errors);
        } else if (
                jsonData.isJsonObject() &&
                jsonData.getAsJsonObject().has("items")
        ) {
            List<T> data = parseDataAsList(jsonData.getAsJsonObject().get("items"), classToCast);
            return new GraphQLResponse<>(data, errors);
        } else if (jsonData.isJsonObject() || jsonData.isJsonPrimitive() || classToCast.equals(JsonElement.class)) {
            T data = parseData(jsonData, classToCast);
            return new GraphQLResponse<>(Collections.singletonList(data), errors);
        } else {
            throw new ApiException("Tried to build a multi item GraphQL response object but " +
                    "the JSON data was in the wrong format");
        }
    }

    // Skips a JSON level to get content of query, not query itself
    private JsonElement skipQueryLevel(JsonElement jsonData) throws ApiException {
        if (jsonData == null || jsonData.isJsonNull()) {
            return null;
        }

        JsonObject data = jsonData.getAsJsonObject();
        if (data.size() == 0) {
            throw new ApiException("Please add a single top level field in your query.");
        } else if (data.size() > 1) {
            throw new ApiException("Please reduce your query to a single top level field.");
        }

        return data.get(data.keySet().iterator().next());
    }

    private <T> T parseData(JsonElement jsonData, Class<T> classToCast) throws ApiException {
        try {
            return gson.fromJson(jsonData, classToCast);
        } catch (ClassCastException classCastException) {
            throw new ApiException.ObjectSerializationException(
                    "Failed to parse GraphQL data portion.", classCastException);
        }
    }

    // Cannot use the same TypeToken trick used in parseErrors due to type erasure
    private <T> List<T> parseDataAsList(JsonElement jsonData, Class<T> classToCast) throws ApiException {
        try {
            ArrayList<T> dataAsList = new ArrayList<>();
            Iterator<JsonElement> iterator = jsonData.getAsJsonArray().iterator();

            while (iterator.hasNext()) {
                T data = gson.fromJson(iterator.next(), classToCast);
                dataAsList.add(data);
            }
            return dataAsList;
        } catch (ClassCastException classCastException) {
            throw new ApiException.ObjectSerializationException(
                    "Failed to parse GraphQL data portion.", classCastException);
        }
    }

    private List<GraphQLResponse.Error> parseErrors(JsonElement jsonErrors) throws ApiException {
        if (jsonErrors == null || jsonErrors.isJsonNull()) {
            return Collections.emptyList();
        }

        JsonArray errors = jsonErrors.getAsJsonArray();
        @SuppressWarnings("WhitespaceAround")
        final Type listType = new TypeToken<ArrayList<GraphQLResponse.Error>>() {}.getType();

        try {
            return gson.fromJson(errors, listType);
        } catch (ClassCastException classCastException) {
            throw new ApiException.ObjectSerializationException(
                "Failed to parse GraphQL errors.", classCastException);
        }
    }
}

