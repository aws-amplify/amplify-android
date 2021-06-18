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

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.util.GsonObjectConverter;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

/**
 * Deserializes JSON into {@link ModelWithMetadata}.
 */
public final class ModelWithMetadataAdapter implements
        JsonDeserializer<ModelWithMetadata<? extends Model>>,
        JsonSerializer<ModelWithMetadata<? extends Model>> {
    /**
     * Register this deserializer into a {@link GsonBuilder}.
     * @param builder A {@link GsonBuilder}
     */
    public static void register(@NonNull GsonBuilder builder) {
        Objects.requireNonNull(builder);
        builder.registerTypeAdapter(ModelWithMetadata.class, new ModelWithMetadataAdapter());
    }

    @Override
    @SuppressWarnings("unchecked") // Cast Type to Class<? extends Model>
    public ModelWithMetadata<? extends Model> deserialize(
            JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        final Class<? extends Model> modelClassType;
        if (typeOfT instanceof ParameterizedType) {
            // Because typeOfT is ParameterizedType we can be sure this is a safe cast.
            modelClassType = (Class<? extends Model>) ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
        } else {
            throw new JsonParseException("Expected a parameterized type during ModelWithMetadata deserialization.");
        }

        final Model model;
        if (modelClassType == SerializedModel.class) {
            model = SerializedModel.builder()
                .serializedData(GsonObjectConverter.toMap((JsonObject) json))
                .modelSchema(null)
                .build();
        } else {
            model = context.deserialize(json, modelClassType);
        }
        ModelMetadata metadata = context.deserialize(json, ModelMetadata.class);
        return new ModelWithMetadata<>(model, metadata);
    }

    @Override
    public JsonElement serialize(
            ModelWithMetadata<? extends Model> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();

        // Flatten out the fields of the model and its metadata into a flat key-value map.
        // To do this, serialize each individually, and then add the key/value pairs for each
        // object into a new container.
        JsonObject serializedMetadata = (JsonObject) context.serialize(src.getSyncMetadata());
        for (Map.Entry<java.lang.String, JsonElement> entry : serializedMetadata.entrySet()) {
            result.add(entry.getKey(), entry.getValue());
        }
        JsonObject serializedModel = (JsonObject) context.serialize(src.getModel());
        for (Map.Entry<java.lang.String, JsonElement> entry : serializedModel.entrySet()) {
            result.add(entry.getKey(), entry.getValue());
        }

        return result;
    }
}
