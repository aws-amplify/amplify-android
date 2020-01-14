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

package com.amplifyframework.api.aws.internal;

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.bind.DateTypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;

/**
 * Custom Type Adapter to override Gson's default {@link DateTypeAdapter}
 * to be able to support seconds field in Time Zone offset to comply
 * with AWSDate and AWSDateTime format.
 *
 * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/scalars.html#awstime">AWSTime Scalar</a>
 */
@SuppressWarnings("ParameterName") // Staying faithful to interface's parameter names
public final class AWSDateTypeAdapter extends TypeAdapter<Date> {
    @Override
    public Date read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        final String json = in.nextString();
        try {
            return AWSDateTimeUtils.parse(json, new ParsePosition(0));
        } catch (ParseException error) {
            throw new JsonSyntaxException(json, error);
        }
    }

    @Override
    public synchronized void write(JsonWriter out, Date value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        String dateFormatAsString = AWSDateTimeUtils.format(value);
        out.value(dateFormatAsString);
    }
}
