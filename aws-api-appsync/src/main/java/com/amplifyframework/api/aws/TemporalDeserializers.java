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

import com.amplifyframework.core.model.temporal.Temporal;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

/**
 * Collection of deserializers for <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html">AWS
 * AppSync temporal scalars</a>.
 */
public final class TemporalDeserializers {

    /**
     * Deserializer of Temporal.Date, an extended ISO-8601 Date string, with an optional timezone offset.
     * <p>
     * Based on the <a href=https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html> AWS AppSync AWSDate
     * scalar.</a>
     */
    public static final class DateDeserializer implements JsonDeserializer<Temporal.Date> {
        @Override
        public Temporal.Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            try {
                return new Temporal.Date(json.getAsString());
            } catch (IllegalArgumentException exception) {
                throw new JsonParseException("Failed to deserialize " + json.getAsString() + " as a Temporal.Date due to " + exception);
            }

        }
    }

    /**
     * Deserializer of Temporal.DateTime, an extended ISO-8601 DateTime string.  Time zone offset is required.
     * <p>
     * Based on the <a href=https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html>AWS AppSync AWSDateTime
     * scalar.</a>
     */
    public static final class DateTimeDeserializer implements JsonDeserializer<Temporal.DateTime> {
        @Override
        public Temporal.DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            try {
                return new Temporal.DateTime(json.getAsString());
            } catch (IllegalArgumentException exception) {
                throw new JsonParseException("Failed to deserialize " + json.getAsString() + " as a Temporal.DateTime due to " + exception);
            }

        }
    }

    /**
     * Deserializer of Temporal.Time, an extended ISO-8601 Time string, with an optional timezone offset.
     * <p>
     * Based on the <a href=https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html>AWS AppSync AWSTime
     * scalar.</a>
     */
    public static final class TimeDeserializer implements JsonDeserializer<Temporal.Time> {
        @Override
        public Temporal.Time deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            try {
                return new Temporal.Time(json.getAsString());
            } catch (IllegalArgumentException exception) {
                throw new JsonParseException("Failed to deserialize " + json.getAsString() + " as a Temporal.Time due to " + exception);
            }

        }
    }

    /**
     * Deserializer of Temporal.Timestamp, a scalar type that represents the number of seconds elapsed
     * since 1970-01-01T00:00Z. Timestamps are serialized and deserialized as numbers. Negative values are also accepted
     * and these represent the number of seconds till 1970-01-01T00:00Z.
     * <p>
     * Based on the <a href=https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html>AWS AppSync AWSTemporal
     * scalar.</a>
     */
    public static final class TimestampDeserializer implements JsonDeserializer<Temporal.Timestamp> {
        @Override
        public Temporal.Timestamp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return new Temporal.Timestamp(json.getAsLong(), TimeUnit.SECONDS);
        }
    }
}
