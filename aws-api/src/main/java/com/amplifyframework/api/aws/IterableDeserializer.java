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

final class IterableDeserializer implements JsonDeserializer<Iterable<Object>> {
    private static final String APP_SYNC_ITEMS_KEY = "items";

    @Override
    @SuppressWarnings("unchecked") // Cast from Type to Class<Object>
    public Iterable<Object> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context
    ) throws JsonParseException {
        final Class<Object> templateClassType;
        if (typeOfT instanceof ParameterizedType) {
            // Because this is an Iterable and typeOfT is ParameterizedType we can be sure this is a safe cast.
            templateClassType = (Class<Object>) ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
        } else {
            throw new JsonParseException("Expected a parameterized type during list deserialization.");
        }

        // If the json we got is not really a list and the list has a generics type...
        if (json.isJsonObject()) {
            /**
             * Parses JSON such as the following:
             *   {
             *      "items" : [
             *          {
             *              "description": null,
             *              "id": "92863611-684a-424d-b3e5-94d42c4914c9",
             *              "name": "some name"
             *          }
             *      ],
             *   }
             */

            JsonObject jsonObject = json.getAsJsonObject();
            // ...and it is in the format we expect from AppSync for a list of objects in a relationship
            if (jsonObject.has(APP_SYNC_ITEMS_KEY) && jsonObject.get(APP_SYNC_ITEMS_KEY).isJsonArray()) {
                JsonArray items = jsonObject.get(APP_SYNC_ITEMS_KEY).getAsJsonArray();
                return toList(items, templateClassType, context);
            } else {
                throw new JsonParseException(
                    "Got JSON from an API call which was supposed to go with a List " +
                            "but is in the form of an object rather than an array. It also is not in the standard " +
                            "format of having an items property with the actual array of data so we do not know how " +
                            "to deserialize it."
                );
            }
        } else if (json.isJsonArray()) {
            return toList(json.getAsJsonArray(), templateClassType, context);
        }
        throw new JsonParseException(
                "Got a JSON value that was not an object or a list. " +
                        "Refusing to deserialize into a Java Iterable."
        );
    }

    private Iterable<Object> toList(JsonArray jsonArray, Type type, JsonDeserializationContext context) {
        final List<Object> items = new ArrayList<>();
        for (JsonElement item : jsonArray) {
            items.add(context.deserialize(item, type));
        }
        return items;
    }
}
