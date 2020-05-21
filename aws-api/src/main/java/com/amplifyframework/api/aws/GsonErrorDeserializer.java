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

import com.amplifyframework.api.graphql.GraphQLLocation;
import com.amplifyframework.api.graphql.GraphQLPathSegment;
import com.amplifyframework.api.graphql.GraphQLResponse;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
final class GsonErrorDeserializer implements JsonDeserializer<GraphQLResponse.Error> {
    private static final String MESSAGE_KEY = "message";
    private static final String LOCATIONS_KEY = "locations";
    private static final String PATH_KEY = "path";
    private static final String EXTENSIONS_KEY = "extensions";

    @Override
    public GraphQLResponse.Error deserialize(JsonElement json,
                 Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!(json.isJsonObject())) {
            throw new JsonParseException("Expected a JSONObject but found a " +
                    json.getClass().getName() + " while deserializing error");
        }

        String message = null;
        List<GraphQLLocation> locations = null;
        List<GraphQLPathSegment> path = null;
        Map<String, Object> extensions = null;
        JsonObject extensionsJson = new JsonObject();
        JsonObject nonSpecifiedData = new JsonObject();

        JsonObject error = json.getAsJsonObject();

        for (String key : error.keySet()) {
            JsonElement value = error.get(key);
            if (value == null) {
                continue;
            }
            switch (key) {
                case MESSAGE_KEY:
                    message = context.deserialize(value, String.class);
                    break;
                case LOCATIONS_KEY:
                    Type locationsType = TypeMaker.getParameterizedType(List.class, GraphQLLocation.class);
                    locations = context.deserialize(value, locationsType);
                    break;
                case PATH_KEY:
                    path = getPath(value);
                    break;
                case EXTENSIONS_KEY:
                    extensionsJson = value.getAsJsonObject();
                    break;
                default:
                    nonSpecifiedData.add(key, value);
                    break;
            }
        }

        // Merge nonSpecifiedData into extensions, but don't overwrite anything already there.
        for (String key : nonSpecifiedData.keySet()) {
            if (!extensionsJson.has(key)) {
                extensionsJson.add(key, nonSpecifiedData.get(key));
            }
        }

        // Deserialize the extensions JSON to a Map
        if (extensionsJson.size() > 0) {
            extensions = GsonUtil.toMap(extensionsJson);
        }

        return new GraphQLResponse.Error(message, locations, path, extensions);
    }

    private List<GraphQLPathSegment> getPath(JsonElement pathElement) {
        List<GraphQLPathSegment> path = new ArrayList<>();
        if (pathElement.isJsonNull()) {
            return null;
        }
        if (!pathElement.isJsonArray()) {
            throw new JsonParseException("Expected a JsonArray but found a " +
                    pathElement.getClass().getName() + " while deserializing path");
        }
        for (JsonElement element : pathElement.getAsJsonArray()) {
            JsonPrimitive primitive = (JsonPrimitive) element;
            if (primitive.isNumber()) {
                path.add(new GraphQLPathSegment(primitive.getAsInt()));
            } else if (primitive.isString()) {
                path.add(new GraphQLPathSegment(primitive.getAsString()));
            } else {
                throw new JsonParseException("Expected a String or int, but found a " +
                        primitive.getClass().getSimpleName() + " while deserializing path segment");
            }
        }
        return path;
    }
}
