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

package com.amplifyframework.api.okhttp;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.Response;
import com.amplifyframework.api.graphql.ResponseFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts JSON strings into models of a given type, using GSON.
 */
final class GsonResponseFactory implements ResponseFactory {
    private final Gson gson;

    /**
     * Default constructor using default Gson object.
     */
    GsonResponseFactory() {
        this(new Gson());
    }

    /**
     * Constructor using customized Gson object.
     * @param gson custom Gson object
     */
    GsonResponseFactory(Gson gson) {
        this.gson = gson;
    }

    @Override
    public <T> Response<T> buildResponse(String responseJson, Class<T> classToCast) throws ApiException {
        JsonObject jsonData = null;
        JsonArray jsonErrors = null;

        T data;
        List<Response.Error> errors;

        try {
            JsonParser parser = new JsonParser();

            final JsonObject toJson = parser.parse(responseJson).getAsJsonObject();
            if (toJson.has("data")) {
                jsonData = toJson.get("data").getAsJsonObject();
            }
            if (toJson.has("errors")) {
                jsonErrors = toJson.get("errors").getAsJsonArray();
            }

            data = parseData(jsonData, classToCast);
            errors = parseErrors(jsonErrors);
        } catch (JsonParseException jsonParseException) {
            throw new ApiException.ObjectSerializationException(jsonParseException);
        }

        return new Response<>(data, errors);
    }

    private <T> T parseData(JsonObject data, Class<T> classToCast) throws ApiException {
        if (data == null) {
            return null;
        } else if (data.size() == 0) {
            throw new ApiException("Please add a single top level field in your query.");
        } else if (data.size() > 1) {
            throw new ApiException("Please reduce your query to a single top level field.");
        }

        final JsonObject objectValueOfFirstKey =
            data.getAsJsonObject(data.keySet().iterator().next());

        try {
            return gson.fromJson(objectValueOfFirstKey, classToCast);
        } catch (ClassCastException classCastException) {
            throw new ApiException.ObjectSerializationException(
                "Failed to parse GraphQL data portion.", classCastException);
        }
    }

    private List<Response.Error> parseErrors(JsonArray errors) throws ApiException {
        @SuppressWarnings("WhitespaceAround") // {} looks better on same line, here.
        final Type listType = new TypeToken<ArrayList<Response.Error>>() {}.getType();

        try {
            return gson.fromJson(errors, listType);
        } catch (ClassCastException classCastException) {
            throw new ApiException.ObjectSerializationException(
                "Failed to parse GraphQL errors.", classCastException);
        }
    }
}

