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

package com.amplifyframework.datastore.syncengine;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Adapter to serialize and deserialize {@link TimeBasedUuid} to and from
 * JSON.
 */
public final class TimeBasedUuidTypeAdapter extends TypeAdapter<TimeBasedUuid> {
    /**
     * Registers this adapter with a GsonBuilder.
     * @param builder GsonBuilder
     */
    public static void register(GsonBuilder builder) {
        builder.registerTypeAdapter(TimeBasedUuid.class, new TimeBasedUuidTypeAdapter());
    }

    @Override
    public void write(JsonWriter jsonWriter, TimeBasedUuid value) throws IOException {
        jsonWriter.jsonValue(value.toString());
    }

    @Override
    public TimeBasedUuid read(JsonReader jsonReader) throws IOException {
        return TimeBasedUuid.fromString(jsonReader.nextString());
    }
}
