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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom list deserializer since some lists come back not as an array of the items but as an object which contains
 * an items property with the list of items and a nextToken property for pagination purposes.
 */
public final class GsonListDeserializer implements JsonDeserializer<List<Object>> {
    @Override
    @SuppressWarnings("unchecked")
    public List<Object> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        // If the json we got is not really a List and this List has a generics type...
        if (json.isJsonObject() && typeOfT instanceof ParameterizedType) {
            // Because this is a list and typeOfT is ParameterizedType we can be sure this is a safe cast.
            Class<Object> clazz = (Class<Object>) ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
            JsonObject jsonObject = json.getAsJsonObject();
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(List.class, new GsonListDeserializer())
                    .create();

            // ...and it is in the format we expect from AppSync for a list of objects in a relationship
            if (jsonObject.has("items") && jsonObject.get("items").isJsonArray()) {
                List<Object> response = new ArrayList<>();
                JsonArray items = jsonObject.get("items").getAsJsonArray();

                if (items.size() == 0) {
                    return null;
                } else {
                    for (JsonElement item : items) {
                        response.add(gson.fromJson(item, clazz));
                    }

                    return response;
                }
            } else {
                throw new JsonParseException("Got JSON from an API call which was supposed to go with a List " +
                        "but is in the form of an object rather than an array. It also is not in the standard " +
                        "format of having an items property with the actual array of data so we do not know how " +
                        "to deserialize it.");
            }
        }

        return new Gson().fromJson(json, typeOfT);
    }
}
