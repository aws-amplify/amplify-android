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

package com.amplifyframework.api.aws.scalar;

import com.amplifyframework.core.types.scalar.AWSDate;
import com.amplifyframework.core.types.scalar.AWSDateTime;
import com.amplifyframework.core.types.scalar.AWSTemporal;
import com.amplifyframework.core.types.scalar.AWSTime;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.ParseException;

/**
 * Factory to be registered with GSON when dealing with AWS AppSync
 * scalars. This adapter will correctly de/serialize strings that are
 * correctly formatted according to the specifications for AWSDate,
 * AWSTime, and AWSDateTime.
 *
 * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html">AWS AppSync Defined Scalars</a>
 */
@SuppressWarnings("ParameterName") // Staying faithful to original method parameter names
public final class AWSTemporalTypeAdapter implements TypeAdapterFactory {
    @Override
    @SuppressWarnings("unchecked") // Returns null if T isn't assignable from AWSTemporal
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<T> rawType = (Class<T>) type.getRawType();
        if (!AWSTemporal.class.isAssignableFrom(rawType)) {
            return null;
        }

        return (TypeAdapter<T>) new TypeAdapter<AWSTemporal>() {
            @Override
            public void write(JsonWriter out, AWSTemporal value) throws IOException {
                if (value == null) {
                    out.nullValue();
                    return;
                }
                String dateFormatAsString = value.toString();
                out.value(dateFormatAsString);
            }

            @Override
            public AWSTemporal read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                final String json = in.nextString();
                try {
                    if (AWSDate.class.equals(rawType)) {
                        return AWSDate.parse(json);
                    }
                    if (AWSTime.class.equals(rawType)) {
                        return AWSTime.parse(json);
                    }
                    if (AWSDateTime.class.equals(rawType)) {
                        return AWSDateTime.parse(json);
                    }
                    return null;
                } catch (ParseException error) {
                    throw new JsonSyntaxException("Encountered an error while parsing " + json, error);
                }
            }
        };
    }
}
