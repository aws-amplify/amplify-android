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

import com.amplifyframework.core.model.SerializedCustomType;
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
import java.util.List;
import java.util.Map;

/**
 * Deserializer for SerializedCustomType. Helpful to deserialize from the graphql response.
 */
public final class SerializedCustomTypeAdapter
        implements JsonDeserializer<SerializedCustomType>, JsonSerializer<SerializedCustomType> {
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
        return context.serialize(src.getSerializedData());
    }

    @Override
    public SerializedCustomType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject serializedDataObject = json.getAsJsonObject();
        Map<String, Object> serializedData = new HashMap<>(GsonObjectConverter.toMap(serializedDataObject));

        // Patch up nested models as SerializedCustomTypes themselves.
        for (Map.Entry<String, JsonElement> entry : serializedDataObject.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                serializedData.put(entry.getKey(), SerializedCustomType.builder()
                        .serializedData(deserialize(entry.getValue(), typeOfT, context).getSerializedData())
                        .customTypeSchema(null)
                        .build());
            } else if (entry.getValue().isJsonArray()) {
                JsonArray arrayList = entry.getValue().getAsJsonArray();
                ArrayList<Object> nestedList = new ArrayList<>();
                for (int i = 0; i < arrayList.size(); i++) {
                    JsonElement item = arrayList.get(i);
                    if (item.isJsonObject()) {
                        nestedList.add(SerializedCustomType.builder()
                                .serializedData(deserialize(item, typeOfT, context).getSerializedData())
                                .customTypeSchema(null)
                                .build());
                    } else {
                        @SuppressWarnings("unchecked")
                        List<Object> serializedList = (List<Object>) serializedData.get(entry.getKey());

                        if (serializedList != null) {
                            nestedList.add(serializedList.get(i));
                        }
                    }
                }
                serializedData.put(entry.getKey(), nestedList);
            } else {
                serializedData.put(entry.getKey(), serializedData.get(entry.getKey()));
            }
        }

        return SerializedCustomType.builder()
                .serializedData(serializedData)
                .customTypeSchema(null)
                .build();
    }
}
