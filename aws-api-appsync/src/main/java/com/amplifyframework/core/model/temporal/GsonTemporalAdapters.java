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

package com.amplifyframework.core.model.temporal;

import androidx.annotation.NonNull;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Collection of serializers and deserializers for
 * <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html">AWS AppSync temporal scalars</a>.
 */
public final class GsonTemporalAdapters {
    private GsonTemporalAdapters() {}

    /**
     * Registers the type adapters into an {@link GsonBuilder}.
     * @param builder A {@link GsonBuilder}
     */
    public static void register(@NonNull GsonBuilder builder) {
        Objects.requireNonNull(builder);
        builder.registerTypeAdapter(Temporal.Date.class, new DateAdapter());
        builder.registerTypeAdapter(Temporal.DateTime.class, new DateTimeAdapter());
        builder.registerTypeAdapter(Temporal.Timestamp.class, new TimestampAdapter());
        builder.registerTypeAdapter(Temporal.Time.class, new TimeAdapter());
        builder.registerTypeAdapter(Date.class, new JavaDateAdapter());
    }

    /**
     * Adapter for Temporal.Date, an extended ISO-8601 Date string, with an optional timezone offset.
     * <p>
     * Based on the <a href=https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html> AWS AppSync AWSDate
     * scalar.</a>
     */
    public static final class DateAdapter implements JsonSerializer<Temporal.Date>, JsonDeserializer<Temporal.Date> {
        @Override
        public JsonElement serialize(Temporal.Date date, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(date.format());
        }

        @Override
        public Temporal.Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            try {
                return new Temporal.Date(json.getAsString());
            } catch (IllegalArgumentException exception) {
                throw new JsonParseException("Failed to deserialize " +
                        json.getAsString() + " as a Temporal.Date due to " + exception);
            }
        }
    }

    /**
     * Adapter for Temporal.DateTime, an extended ISO-8601 DateTime string.  Time zone offset is required.
     * <p>
     * Based on the <a href=https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html>AWS AppSync AWSDateTime
     * scalar.</a>
     */
    public static final class DateTimeAdapter implements
            JsonSerializer<Temporal.DateTime>, JsonDeserializer<Temporal.DateTime> {
        @Override
        public JsonElement serialize(Temporal.DateTime dateTime, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(dateTime.format());
        }

        @Override
        public Temporal.DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                return new Temporal.DateTime(json.getAsString());
            } catch (IllegalArgumentException exception) {
                throw new JsonParseException("Failed to deserialize " +
                        json.getAsString() + " as a Temporal.DateTime due to " + exception);
            }
        }
    }

    /**
     * Adapter for Temporal.Time, an extended ISO-8601 Time string, with an optional timezone offset.
     * <p>
     * Based on the <a href=https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html>AWS AppSync AWSTime
     * scalar.</a>
     */
    public static final class TimeAdapter implements JsonSerializer<Temporal.Time>, JsonDeserializer<Temporal.Time> {
        @Override
        public JsonElement serialize(Temporal.Time time, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(time.format());
        }

        @Override
        public Temporal.Time deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                return new Temporal.Time(json.getAsString());
            } catch (IllegalArgumentException exception) {
                throw new JsonParseException("Failed to deserialize " +
                        json.getAsString() + " as a Temporal.Time due to " + exception);
            }
        }
    }

    /**
     * Adapter for Temporal.Timestamp, a scalar type that represents the number of seconds elapsed
     * since 1970-01-01T00:00Z. Timestamps are serialized and deserialized as numbers. Negative values are also accepted
     * and these represent the number of seconds till 1970-01-01T00:00Z.
     * <p>
     * Based on the <a href=https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html>AWS AppSync AWSTemporal
     * scalar.</a>
     */
    public static final class TimestampAdapter
            implements JsonSerializer<Temporal.Timestamp>, JsonDeserializer<Temporal.Timestamp> {
        @Override
        public JsonElement serialize(Temporal.Timestamp timestamp, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(timestamp.getSecondsSinceEpoch());
        }

        @Override
        public Temporal.Timestamp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return new Temporal.Timestamp(json.getAsLong(), TimeUnit.SECONDS);
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
    public static final class JavaDateAdapter implements JsonSerializer<Date> {
        @Override
        public JsonElement serialize(Date date, Type typeOfSrc, JsonSerializationContext context) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return new JsonPrimitive(dateFormat.format(date));
        }
    }
}
