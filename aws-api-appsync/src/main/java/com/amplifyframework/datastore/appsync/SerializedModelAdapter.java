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

import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.util.GsonObjectConverter;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Deserializer for SerializedModel. Helpful to deserialize from the graphql response.
 */
public final class SerializedModelAdapter
        implements JsonDeserializer<SerializedModel>, JsonSerializer<SerializedModel> {
    private SerializedModelAdapter() {}

    /**
     * Registers an adapter with a Gson builder.
     * @param builder A gson builder
     */
    public static void register(GsonBuilder builder) {
        builder.registerTypeAdapter(SerializedModel.class, new SerializedModelAdapter());
    }

    @Override
    public JsonElement serialize(SerializedModel src, Type typeOfSrc, JsonSerializationContext context) {
        ModelSchema schema = src.getModelSchema();

        JsonObject result = new JsonObject();
        result.add("id", context.serialize(src.getId()));
        result.add("modelSchema", context.serialize(schema));

        JsonObject serializedData = new JsonObject();
        for (Map.Entry<String, Object> entry : src.getSerializedData().entrySet()) {
            if (entry.getValue() instanceof SerializedModel) {
                SerializedModel serializedModel = (SerializedModel) entry.getValue();
                serializedData.add(entry.getKey(), new JsonPrimitive(serializedModel.getId()));
            } else {
                serializedData.add(entry.getKey(), context.serialize(entry.getValue()));
            }
        }
        result.add("serializedData", serializedData);
        return result;
    }

    @Override
    public SerializedModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        ModelSchema modelSchema = context.deserialize(object.get("modelSchema"), ModelSchema.class);

        JsonObject serializedDataObject = object.get("serializedData").getAsJsonObject();
        Map<String, Object> serializedData = new HashMap<>(GsonObjectConverter.toMap(serializedDataObject));

        // Patch up nested models as SerializedModels themselves.
        for (Map.Entry<String, JsonElement> item : serializedDataObject.entrySet()) {
            ModelField field = modelSchema.getFields().get(item.getKey());
            if (field != null && field.isModel()) {
                serializedData.put(field.getName(), SerializedModel.builder()
                    .serializedData(Collections.singletonMap("id", item.getValue().getAsString()))
                    .modelSchema(null)
                    .build());
            }
        }

        return SerializedModel.builder()
            .serializedData(serializedData)
            .modelSchema(modelSchema)
            .build();
    }
}
