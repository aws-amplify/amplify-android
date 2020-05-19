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
import com.amplifyframework.core.model.temporal.Temporal;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of a GraphQL Request serializer for the variables map using Gson.
 */
final class GsonVariablesSerializer implements GraphQLRequest.VariablesSerializer {
    private final Gson gson;

    GsonVariablesSerializer() {
        gson = GsonFactory.create();
    }

    @Override
    public String serialize(Map<String, Object> variables) {
        return gson.toJson(variables);
    }

    /**
     * Serializer of {@link Temporal.Date}, an extended ISO-8601 Date string, with an optional timezone offset.
     *
     * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html
     */
    static class TemporalDateSerializer implements JsonSerializer<Temporal.Date> {
        @Override
        public JsonElement serialize(Temporal.Date date, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(date.format());
        }
    }

    /**
     * Serializer of {@link Temporal.DateTime}, an extended ISO-8601 DateTime string.
     * Time zone offset is required.
     *
     * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html
     */
    static class TemporalDateTimeSerializer implements JsonSerializer<Temporal.DateTime> {
        @Override
        public JsonElement serialize(Temporal.DateTime dateTime, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(dateTime.format());
        }
    }

    /**
     * Serializer of {@link Temporal.Time}, an extended ISO-8601 Time string, with an optional timezone offset.
     *
     * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html
     */
    static class TemporalTimeSerializer implements JsonSerializer<Temporal.Time> {
        @Override
        public JsonElement serialize(Temporal.Time time, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(time.format());
        }
    }

    /**
     * Serializer of {@link Temporal.Timestamp}, an AppSync scalar type that represents
     * the number of seconds elapsed since 1970-01-01T00:00Z. Timestamps are serialized as numbers.
     * Negative values are also accepted and these represent the number of seconds till 1970-01-01T00:00Z.
     *
     * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html
     */
    static class TemporalTimestampSerializer implements JsonSerializer<Temporal.Timestamp> {
        @Override
        public JsonElement serialize(Temporal.Timestamp timestamp, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(timestamp.getSecondsSinceEpoch());
        }
    }

    /**
     * Earlier versions of the model gen used to use Java's {@link Date} to represent all of the
     * temporal types. This led to challenges while trying to decode/encode the timezone,
     * among other things. The model gen will now spit out {@link Temporal.Date}, {@link Temporal.DateTime},
     * {@link Temporal.Time}, and {@link Temporal.Timestamp}, instead. This DateSerializer is left for
     * compat, until such a time as it can be safely removed (that is, when all models no longer
     * use a raw Date type.)
     */
    static class DateSerializer implements JsonSerializer<Date> {
        @Override
        public JsonElement serialize(Date date, Type typeOfSrc, JsonSerializationContext context) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return new JsonPrimitive(dateFormat.format(date));
        }
    }
}
