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
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.SerializedCustomType;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.util.GsonObjectConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Deserializer for SerializedModel. Helpful to deserialize from the graphql response.
 */
public final class SerializedModelAdapter
        implements JsonDeserializer<SerializedModel>, JsonSerializer<SerializedModel> {
    private SerializedModelAdapter() {
    }

    /**
     * Registers an adapter with a Gson builder.
     *
     * @param builder A gson builder
     */
    public static void register(GsonBuilder builder) {
        builder.registerTypeAdapter(SerializedModel.class, new SerializedModelAdapter());
    }

    @Override
    public JsonElement serialize(SerializedModel src, Type typeOfSrc, JsonSerializationContext context) {
        ModelSchema schema = src.getModelSchema();

        JsonObject result = new JsonObject();
        result.add("id", context.serialize(src.getPrimaryKeyString()));
        result.add("modelSchema", context.serialize(schema));

        JsonObject serializedData = new JsonObject();
        for (Map.Entry<String, Object> entry : src.getSerializedData().entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            if (fieldValue instanceof SerializedModel) {
                SerializedModel serializedModel = (SerializedModel) fieldValue;
                serializedData.add(fieldName, context.serialize(serializedModel.getSerializedData()));
            } else if (fieldValue instanceof SerializedCustomType) {
                // serialize via SerializedCustomTypeAdapter
                serializedData.add(fieldName, context.serialize(fieldValue));
            } else {
                serializedData.add(fieldName, context.serialize(fieldValue));
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
            if (field == null) {
                continue;
            }

            JsonElement fieldValue = item.getValue();
            String fieldName = field.getName();

            // if the field type is a Model - convert the nested data into SerializedModel
            if (field.isModel()) {
                SchemaRegistry schemaRegistry = SchemaRegistry.instance();
                ModelSchema nestedModelSchema = schemaRegistry.getModelSchemaForModelClass(field.getTargetType());
                Gson gson = new Gson();
                Type mapType = new TypeToken<Map<String, Object>>() {
                }.getType();
                serializedData.put(fieldName, SerializedModel.builder()
                        .modelSchema(nestedModelSchema)
                        .serializedData(gson.fromJson(fieldValue, mapType))
                        .build());
            } else if (field.isCustomType()) {
                // if the field type is a CustomType - convert the nested data into SerializedCustomType
                serializedData.put(fieldName, context.deserialize(fieldValue, SerializedCustomType.class));
            }
        }

        return SerializedModel.builder()
                .modelSchema(modelSchema)
                .serializedData(serializedData)
                .build();
    }
}
