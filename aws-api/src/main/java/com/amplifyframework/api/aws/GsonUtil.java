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

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.model.scalar.AWSDate;
import com.amplifyframework.core.model.scalar.AWSDateTime;
import com.amplifyframework.core.model.scalar.AWSTime;
import com.amplifyframework.util.Immutable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class GsonUtil {
    private static Gson gson;

    private GsonUtil() {
        throw new UnsupportedOperationException("No instances allowed.");
    }

    static Map<String, Object> toMap(JsonObject object) {
        Map<String, Object> map = new HashMap<>();
        for (String key : object.keySet()) {
            JsonElement element = object.get(key);
            map.put(key, toObject(element));
        }
        return Immutable.of(map);
    }

    static List<Object> toList(JsonArray array) {
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

    public static Gson getGson() {
        if (gson == null) {
            gson = new GsonBuilder()
                    .registerTypeAdapter(GraphQLResponse.Error.class, new GsonErrorDeserializer())
                    .registerTypeAdapter(Date.class, new DateAdapter())
                    .registerTypeAdapter(AWSDate.class, new AWSDateAdapter())
                    .registerTypeAdapter(AWSDateTime.class, new AWSDateTimeAdapter())
                    .registerTypeAdapter(AWSTime.class, new AWSTimeAdapter())
                    .create();
        }
        return gson;
    }

    static class AWSDateAdapter implements JsonDeserializer<AWSDate>, JsonSerializer<AWSDate> {
        @Override
        public AWSDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return new AWSDate(json.getAsString());
        }

        @Override
        public JsonElement serialize(AWSDate date, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(date.format());
        }
    }

    static class AWSDateTimeAdapter implements JsonDeserializer<AWSDateTime>, JsonSerializer<AWSDateTime> {
        @Override
        public AWSDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return new AWSDateTime(json.getAsString());
        }

        @Override
        public JsonElement serialize(AWSDateTime dateTime, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(dateTime.format());
        }
    }

    static class AWSTimeAdapter implements JsonDeserializer<AWSTime>, JsonSerializer<AWSTime> {
        @Override
        public AWSTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return new AWSTime(json.getAsString());
        }

        @Override
        public JsonElement serialize(AWSTime time, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(time.format());
        }
    }

    /**
     * Used for deserializing AWSTimestamp, an AppSync scalar type that represents the number of seconds elapsed since
     * 1970-01-01T00:00Z. Timestamps are serialized and deserialized as numbers. Negative values are also accepted and
     * these represent the number of seconds till 1970-01-01T00:00Z.
     *
     * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html
     */
    static class DateAdapter implements JsonDeserializer<Date>, JsonSerializer<Date> {
        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            long epochTimeInSeconds = json.getAsLong();
            long epochTimeInMillis = TimeUnit.SECONDS.toMillis(epochTimeInSeconds);
            return new Date(epochTimeInMillis);
        }

        @Override
        public JsonElement serialize(Date date, Type typeOfSrc, JsonSerializationContext context) {
            long timeInMillis = date.getTime();
            long timeInSeconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis);
            return new JsonPrimitive(timeInSeconds);
        }
    }
}
