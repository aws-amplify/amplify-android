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

package com.amplifyframework.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility to convert Gson types into pure Java types.
 */
public final class GsonObjectConverter {
    private GsonObjectConverter() {
        throw new UnsupportedOperationException("No instances allowed.");
    }

    /**
     * Converts A Gson {@link JsonObject} into a Java String-to-Object map.
     * @param object JsonObject to convert
     * @return Java String-to-Object Map
     */
    public static Map<String, Object> toMap(JsonObject object) {
        Map<String, Object> map = new HashMap<>();
        for (String key : object.keySet()) {
            JsonElement element = object.get(key);
            map.put(key, toObject(element));
        }
        return Immutable.of(map);
    }

    /**
     * Converts a Gson {@link JsonArray} into a Java list of Object.
     * @param array Gson {@link JsonArray} to convert
     * @return Java list of Object
     */
    public static List<Object> toList(JsonArray array) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            list.add(toObject(element));
        }
        return Immutable.of(list);
    }

    private static Object toObject(JsonElement element) {
        if (element != null) {
            if (element.isJsonArray()) {
                return toList(element.getAsJsonArray());
            } else if (element.isJsonObject()) {
                return toMap(element.getAsJsonObject());
            } else if (element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isString()) {
                    return primitive.getAsString();
                } else if (primitive.isNumber()) {
                    Number number = primitive.getAsNumber();
                    if (number.floatValue() == number.intValue()) {
                        return number.intValue();
                    } else if (number.floatValue() == number.doubleValue()) {
                        return number.floatValue();
                    } else {
                        return number.doubleValue();
                    }
                } else if (primitive.isBoolean()) {
                    return primitive.getAsBoolean();
                }
            }
        }
        return null;
    }
}
