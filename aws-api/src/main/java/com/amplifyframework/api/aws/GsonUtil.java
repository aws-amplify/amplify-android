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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

final class GsonUtil {

    private GsonUtil() {
        throw new UnsupportedOperationException("No instances allowed.");
    }

    static HashMap<String, Object> toMap(JsonObject object) {
        HashMap<String, Object> map = new HashMap<>();
        for (String key : object.keySet()) {
            JsonElement element = object.get(key);
            map.put(key, toObject(element));
        }
        return map;
    }

    static List<Object> toList(JsonArray array) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            list.add(toObject(element));
        }
        return list;
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
                    } else {
                        return number.floatValue();
                    }
                } else if (primitive.isBoolean()) {
                    return primitive.getAsBoolean();
                }
            }
        }
        return null;
    }
}
