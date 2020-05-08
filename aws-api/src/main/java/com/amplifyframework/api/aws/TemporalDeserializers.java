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

import com.amplifyframework.core.model.AWSDate;
import com.amplifyframework.core.model.AWSDateTime;
import com.amplifyframework.core.model.AWSTime;
import com.amplifyframework.core.model.AWSTimestamp;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

final class TemporalDeserializers {
    /**
     * Deserializer of AWSDate, an extended ISO-8601 Date string, with an optional timezone offset.
     *
     * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html
     */
    static class AWSDateDeserializer implements JsonDeserializer<AWSDate> {
        @Override
        public AWSDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return new AWSDate(json.getAsString());
        }
    }

    /**
     * Deserializer of AWSDateTime, an extended ISO-8601 DateTime string.  Time zone offset is required.
     *
     * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html
     */
    static class AWSDateTimeDeserializer implements JsonDeserializer<AWSDateTime> {
        @Override
        public AWSDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return new AWSDateTime(json.getAsString());
        }
    }

    /**
     * Deserializer of AWSTime, an extended ISO-8601 Time string, with an optional timezone offset.
     *
     * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html
     */
    static class AWSTimeDeserializer implements JsonDeserializer<AWSTime> {
        @Override
        public AWSTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return new AWSTime(json.getAsString());
        }
    }

    /**
     * Deserializer of AWSTimestamp, an AppSync scalar type that represents the number of seconds elapsed
     * since 1970-01-01T00:00Z. Timestamps are serialized and deserialized as numbers. Negative values are also accepted
     * and these represent the number of seconds till 1970-01-01T00:00Z.
     *
     * https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html
     */
    static class AWSTimestampDeserializer implements JsonDeserializer<AWSTimestamp> {
        @Override
        public AWSTimestamp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return new AWSTimestamp(json.getAsLong(), TimeUnit.SECONDS);
        }
    }
}
