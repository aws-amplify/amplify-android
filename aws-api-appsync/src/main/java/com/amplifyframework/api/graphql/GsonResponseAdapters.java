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

package com.amplifyframework.api.graphql;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelPage;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.util.GsonObjectConverter;
import com.amplifyframework.util.TypeMaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A collection of {@link Gson} type adapters to facilitate
 * deserialization of an GraphQL JSON response.
 */
public final class GsonResponseAdapters {
    private GsonResponseAdapters() {}

    /**
     * Registers the adapters with a Gson instance.
     * @param builder A GsonBuilder instance
     */
    public static void register(GsonBuilder builder) {
        builder
            .registerTypeAdapter(GraphQLResponse.class, new ResponseDeserializer())
            .registerTypeAdapter(GraphQLResponse.Error.class, new ErrorDeserializer());
    }

    /**
     * Deserializes GraphQL response JSON into modeled {@link GraphQLResponse}s.
     */
    public static final class ResponseDeserializer implements JsonDeserializer<GraphQLResponse<Object>> {
        private static final String DATA_KEY = "data";
        private static final String ERRORS_KEY = "errors";

        @Override
        public GraphQLResponse<Object> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (!json.isJsonObject()) {
                throw new JsonParseException(
                    "Expected a JsonObject while deserializing GraphQLResponse but found " + json
                );
            }
            JsonObject jsonObject = json.getAsJsonObject();

            JsonElement jsonData = null;
            JsonElement jsonErrors = null;

            if (jsonObject.has(DATA_KEY)) {
                jsonData = jsonObject.get(DATA_KEY);
            }

            if (jsonObject.has(ERRORS_KEY)) {
                jsonErrors = jsonObject.get(ERRORS_KEY);
            }

            List<GraphQLResponse.Error> errors = parseErrors(jsonErrors, context);

            if (!(typeOfT instanceof ParameterizedType)) {
                throw new JsonParseException("Expected a parameterized type during GraphQLResponse deserialization.");
            }

            // Because typeOfT is ParameterizedType we can be sure this is a safe cast.
            final Type templateClassType = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
            if (shouldSkipQueryLevel(templateClassType)) {
                jsonData = skipQueryLevel(jsonData);
            }

            if (jsonData == null || jsonData.isJsonNull()) {
                return new GraphQLResponse<>(null, errors);
            } else {
                Object data = context.deserialize(jsonData, templateClassType);
                return new GraphQLResponse<>(data, errors);
            }
        }

        private boolean shouldSkipQueryLevel(Type type) {
            if (type instanceof ParameterizedType) {
                final Type rawType = ((ParameterizedType) type).getRawType();
                if (ModelWithMetadata.class.equals(rawType)) {
                    return true;
                }
                if (Iterable.class.isAssignableFrom((Class<?>) rawType)) {
                    return true;
                }
                if (ModelPage.class.isAssignableFrom((Class<?>) rawType)) {
                    return true;
                }
            } else {
                if (Model.class.isAssignableFrom((Class<?>) type)) {
                    return true;
                }
            }

            return false;
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
            Type listType = TypeMaker.getParameterizedType(ArrayList.class, GraphQLResponse.Error.class);
            return context.deserialize(jsonErrors, listType);
        }
    }

    /**
     * Deserializes an error in a GraphQL response JSON into a modeled
     * {@link GraphQLResponse.Error}.
     */
    public static final class ErrorDeserializer implements JsonDeserializer<GraphQLResponse.Error> {
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

            if (message == null) {
                message = "Message was null or missing while deserializing error";
            }

            // Merge nonSpecifiedData into extensions, but don't overwrite anything already there.
            for (String key : nonSpecifiedData.keySet()) {
                if (!extensionsJson.has(key)) {
                    extensionsJson.add(key, nonSpecifiedData.get(key));
                }
            }

            // Deserialize the extensions JSON to a Map
            if (extensionsJson.size() > 0) {
                extensions = GsonObjectConverter.toMap(extensionsJson);
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
}
