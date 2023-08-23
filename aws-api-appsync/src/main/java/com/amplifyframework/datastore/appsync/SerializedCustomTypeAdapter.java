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

package com.amplifyframework.datastore.appsync;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.CustomTypeField;
import com.amplifyframework.core.model.CustomTypeSchema;
import com.amplifyframework.core.model.SerializedCustomType;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.GsonObjectConverter;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Deserializer for SerializedCustomType. Helpful to deserialize from the graphql response.
 */
public final class SerializedCustomTypeAdapter
        implements JsonDeserializer<SerializedCustomType>, JsonSerializer<SerializedCustomType> {
    private static final Logger LOGGER = Amplify.Logging.logger(
            CategoryType.DATASTORE, SerializedCustomTypeAdapter.class.getName());

    private SerializedCustomTypeAdapter() {
    }

    /**
     * Registers an adapter with a Gson builder.
     *
     * @param builder A gson builder
     */
    public static void register(GsonBuilder builder) {
        builder.registerTypeAdapter(SerializedCustomType.class, new SerializedCustomTypeAdapter());
    }

    @Override
    public JsonElement serialize(SerializedCustomType src, Type typeOfSrc, JsonSerializationContext context) {
        LOGGER.verbose(String.format("serialize: src=%s, typeOfSrc=%s", src, typeOfSrc));
        CustomTypeSchema schema = src.getCustomTypeSchema();
        JsonObject result = new JsonObject();
        result.add("customTypeSchema", context.serialize(schema));

        JsonObject serializedData = new JsonObject();

        for (Map.Entry<String, Object> entry : src.getSerializedData().entrySet()) {
            Object fieldValue = entry.getValue();
            if (fieldValue instanceof SerializedCustomType) {
                // serialize by type SerializedCustomType
                serializedData.add(entry.getKey(), context.serialize((SerializedCustomType) fieldValue));
            } else {
                serializedData.add(entry.getKey(), context.serialize(fieldValue));
            }
        }

        result.add("serializedData", serializedData);
        return result;
    }

    @Override
    public SerializedCustomType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        LOGGER.verbose(String.format("deserialize: json=%s, typeOfT=%s", json, typeOfT));
        JsonObject object = json.getAsJsonObject();
        CustomTypeSchema schema = context.deserialize(
                object.get("customTypeSchema"), CustomTypeSchema.class);

        JsonObject serializedDataObject = object.get("serializedData").getAsJsonObject();
        Map<String, Object> serializedData = new HashMap<>(GsonObjectConverter.toMap(serializedDataObject));

        // Patch up nested models as SerializedCustomTypes themselves.
        for (Map.Entry<String, JsonElement> entry : serializedDataObject.entrySet()) {
            CustomTypeField field = schema.getFields().get(entry.getKey());
            if (field == null) {
                continue;
            }

            JsonElement fieldValue = entry.getValue();
            String fieldName = field.getName();

            if (field.isCustomType()) {
                if (!field.isArray() && fieldValue.isJsonObject()) {
                    serializedData.put(
                            fieldName, context.deserialize(fieldValue, SerializedCustomType.class));
                } else if (field.isArray() && fieldValue.isJsonArray()) {
                    JsonArray arrayList = fieldValue.getAsJsonArray();
                    ArrayList<Object> nestedList = new ArrayList<>();
                    for (int i = 0; i < arrayList.size(); i++) {
                        JsonElement item = arrayList.get(i);
                        nestedList.add(context.deserialize(item, SerializedCustomType.class));
                    }

                    serializedData.put(fieldName, nestedList);
                }
            }
        }

        return SerializedCustomType.builder()
                .serializedData(serializedData)
                .customTypeSchema(schema)
                .build();
    }
}
