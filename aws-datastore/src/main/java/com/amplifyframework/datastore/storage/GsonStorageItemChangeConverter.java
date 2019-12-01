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

package com.amplifyframework.datastore.storage;

import com.amplifyframework.core.model.Model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * A utility to convert between {@link StorageItemChange} and {@link StorageItemChange.Record}.
 */
public final class GsonStorageItemChangeConverter implements
        StorageItemChange.RecordFactory, StorageItemChange.StorageItemChangeFactory {
    private final Gson gson;

    /**
     * Constructs a new instance of hte {@link GsonStorageItemChangeConverter}.
     */
    public GsonStorageItemChangeConverter() {
        this.gson = new GsonBuilder()
            .registerTypeAdapterFactory(new ClassTypeAdapterFactory())
            .create();
    }

    @Override
    public <T extends Model> StorageItemChange.Record toRecord(StorageItemChange<T> storageItemChange) {
        return StorageItemChange.Record.builder()
            .id(storageItemChange.changeId().toString())
            .entry(gson.toJson(storageItemChange))
            .itemClass(storageItemChange.itemClass().getName())
            .build();
    }

    @Override
    public <T extends Model> StorageItemChange<T> fromRecord(StorageItemChange.Record record) {
        Class<?> itemClass;
        try {
            itemClass = Class.forName(record.getItemClass());
        } catch (ClassNotFoundException classNotFoundException) {
            throw new IllegalStateException(classNotFoundException);
        }
        final Type itemType =
            TypeToken.getParameterized(StorageItemChange.class, itemClass).getType();
        return gson.fromJson(record.getEntry(), itemType);
    }

    /**
     * A {@link TypeAdapterFactory} which generates a {@link TypeAdapter} for use
     * with {@link Class}-type objects.
     */
    static final class ClassTypeAdapterFactory implements TypeAdapterFactory {
        @SuppressWarnings("unchecked")
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            if (!Class.class.isAssignableFrom(typeToken.getRawType())) {
                return null;
            }
            return (TypeAdapter<T>) new ClassTypeAdapter();
        }
    }

    /**
     * {@link StorageItemChange} contains an {@link Class} member, but Gson doesn't
     * know what to do with it. So, we need this custom {@link TypeAdapter}.
     */
    static final class ClassTypeAdapter extends TypeAdapter<Class<?>> {
        @Override
        public void write(JsonWriter jsonWriter, Class<?> clazz) throws IOException {
            if (clazz == null) {
                jsonWriter.nullValue();
                return;
            }
            jsonWriter.value(clazz.getName());
        }

        @Override
        public Class<?> read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            Class<?> clazz;
            try {
                clazz = Class.forName(jsonReader.nextString());
            } catch (ClassNotFoundException exception) {
                throw new IOException(exception);
            }
            return clazz;
        }
    }
}
