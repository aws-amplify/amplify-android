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

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.ParseException;

/**
 * Type Adapter to be registered with GSON when dealing with custom
 * parsing logic. This type adapter will serialize an object via its
 * .toString() method, and it will deserialize an object by the custom
 * parsing logic that is registered during the initialization of this
 * adapter instance.
 */
@SuppressWarnings("ParameterName") // Staying faithful to original method parameter names
final class NullSafeParseAdapter<T> extends TypeAdapter<T> {
    private final Parser<T> parser;

    NullSafeParseAdapter(Parser<T> parser) {
        this.parser = parser;
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        String formatted = value.toString();
        out.value(formatted);
    }

    @Override
    public T read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        final String json = in.nextString();
        try {
            return parser.parse(json);
        } catch (ParseException error) {
            throw new JsonSyntaxException("Encountered an error while parsing " + json, error);
        }
    }

    interface Parser<T> {
        T parse(String string) throws ParseException;
    }
}
