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

import com.amplifyframework.api.graphql.GraphQLResponse;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class GraphQLResponseDeserializer implements JsonDeserializer<GraphQLResponse<Object>> {
    private static final String DATA_KEY = "data";
    private static final String ERRORS_KEY = "errors";

    @Override
    public GraphQLResponse<Object> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context
    ) throws JsonParseException {
        if (!json.isJsonObject()) {
            throw new JsonParseException("Expected a JsonObject while deserializing GraphQLResponse but found " + json);
        }
        JsonObject jsonObject = json.getAsJsonObject();

        JsonElement jsonData = null;
        JsonElement jsonErrors = null;

        if (jsonObject.has(DATA_KEY)) {
            jsonData = skipQueryLevel(jsonObject.get(DATA_KEY));
        }
        if (jsonObject.has(ERRORS_KEY)) {
            jsonErrors = jsonObject.get(ERRORS_KEY);
        }

        List<GraphQLResponse.Error> errors = parseErrors(jsonErrors, context);

        if (!(typeOfT instanceof ParameterizedType)) {
            throw new JsonParseException("Expected a parameterized type during list deserialization.");
        }

        // Because typeOfT is ParameterizedType we can be sure this is a safe cast.
        final Type templateClassType = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];

        if (jsonData == null || jsonData.isJsonNull()) {
            return new GraphQLResponse<>(null, errors);
        } else {
            Object data = context.deserialize(jsonData, templateClassType);
            return new GraphQLResponse<>(data, errors);
        }
    }

    // Skips a JSON level to get content of query, not query itself
    private JsonElement skipQueryLevel(JsonElement jsonData) throws JsonParseException {
        if (jsonData == null || jsonData.isJsonNull()) {
            return null;
        }

        JsonObject data = jsonData.getAsJsonObject();
        if (data.size() == 0) {
            throw new JsonParseException(
                    "Amplify encountered an error while serializing/deserializing an object.  " +
                            "Please add a single top level field in your query."
            );
        } else if (data.size() > 1) {
            throw new JsonParseException(
                    "Amplify encountered an error while serializing/deserializing an object.  " +
                            "Please reduce your query to a single top level field."
            );
        }
        return data.get(data.keySet().iterator().next());
    }

    private List<GraphQLResponse.Error> parseErrors(JsonElement jsonErrors, JsonDeserializationContext context) {
        if (jsonErrors == null || jsonErrors.isJsonNull()) {
            return Collections.emptyList();
        }
        Type listType = new TypeToken<ArrayList<GraphQLResponse.Error>>() {}.getType();
        return context.deserialize(jsonErrors, listType);
    }
}
