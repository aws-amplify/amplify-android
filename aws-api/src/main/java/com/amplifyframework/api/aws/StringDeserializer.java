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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Normally Gson will only deserialize a JsonPrimitive into a String.  This deserializer enables JsonObject to be
 * serialized to a String, instead of throwing an exception.
 */
class StringDeserializer implements JsonDeserializer<String> {
    @Override
    public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context
    ) throws JsonParseException {
        if (json.isJsonPrimitive()) {
            return json.getAsJsonPrimitive().getAsString();
        } else if (json.isJsonObject()) {
            return json.toString();
        } else {
            throw new JsonParseException("Failed to parse String from " + json);
        }
    }
}
