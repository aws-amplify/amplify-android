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

import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.core.model.AWSDate;
import com.amplifyframework.core.model.AWSDateTime;
import com.amplifyframework.core.model.AWSTime;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of a GraphQL Request serializer for the variables map using Gson.
 */
public final class GsonVariablesSerializer implements GraphQLRequest.VariablesSerializer {
    @Override
    public String serialize(Map<String, Object> variables) {
        return new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateSerializer())
                .registerTypeAdapter(AWSDate.class, new AWSDateSerializer())
                .registerTypeAdapter(AWSDateTime.class, new AWSDateTimeSerializer())
                .registerTypeAdapter(AWSTime.class, new AWSTimeSerializer())
                .create()
                .toJson(variables);
    }

    /**
     * Serializer of AWSDate, an extended ISO-8601 Date string, with an optional timezone offset.
     *
     * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html
     */
    static class AWSDateSerializer implements JsonSerializer<AWSDate> {
        @Override
        public JsonElement serialize(AWSDate date, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(date.format());
        }
    }

    /**
     * Serializer of AWSDateTime, an extended ISO-8601 DateTime string.  Time zone offset is required.
     *
     * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html
     */
    static class AWSDateTimeSerializer implements JsonSerializer<AWSDateTime> {
        @Override
        public JsonElement serialize(AWSDateTime dateTime, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(dateTime.format());
        }
    }

    /**
     * Serializer of AWSTime, an extended ISO-8601 Time string, with an optional timezone offset.
     *
     * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html
     */
    static class AWSTimeSerializer implements JsonSerializer<AWSTime> {
        @Override
        public JsonElement serialize(AWSTime time, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(time.format());
        }
    }

    /**
     * Serializer of AWSTimestamp, an AppSync scalar type that represents the number of seconds elapsed since
     * 1970-01-01T00:00Z. Timestamps are serialized as numbers. Negative values are also accepted and these represent
     * the number of seconds till 1970-01-01T00:00Z.
     *
     * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html
     */
    static class DateSerializer implements JsonSerializer<Date> {
        @Override
        public JsonElement serialize(Date date, Type typeOfSrc, JsonSerializationContext context) {
            long timeInMillis = date.getTime();
            long timeInSeconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis);
            return new JsonPrimitive(timeInSeconds);
        }
    }
}
