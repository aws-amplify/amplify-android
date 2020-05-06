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

package com.amplifyframework.datastore.syncengine;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.model.OperatorInterfaceAdapter;
import com.amplifyframework.datastore.model.PredicateInterfaceAdapter;

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
 * A utility to convert between {@link PendingMutation} and {@link PendingMutation.PersistentRecord}.
 */
public final class GsonPendingMutationConverter implements PendingMutation.Converter {
    private final Gson gson;

    /**
     * Constructs a new instance of hte {@link GsonPendingMutationConverter}.
     */
    public GsonPendingMutationConverter() {
        this.gson = new GsonBuilder()
            .registerTypeAdapterFactory(new ClassTypeAdapterFactory())
            .registerTypeAdapter(QueryPredicate.class, new PredicateInterfaceAdapter())
            .registerTypeAdapter(QueryOperator.class, new OperatorInterfaceAdapter())
            .create();
    }

    @NonNull
    @Override
    public <T extends Model> PendingMutation.PersistentRecord toRecord(@NonNull PendingMutation<T> mutation) {
        return PendingMutation.PersistentRecord.builder()
            .decodedModelClassName(mutation.getClassOfMutatedItem().getName())
            .encodedModelData(gson.toJson(mutation))
            .id(mutation.getMutatedItem().getId())
            .build();
    }

    @NonNull
    @Override
    public <T extends Model> PendingMutation<T> fromRecord(
            @NonNull PendingMutation.PersistentRecord record) throws DataStoreException {
        final Class<?> itemClass;
        try {
            itemClass = Class.forName(record.getDecodedModelClassName());
        } catch (ClassNotFoundException classNotFoundException) {
            throw new DataStoreException(
                "Could not find a class with the name " + record.getDecodedModelClassName(),
                classNotFoundException,
                "Verify that you have built this model into your project."
            );
        }
        final Type itemType =
            TypeToken.getParameterized(PendingMutation.class, itemClass).getType();
        return gson.fromJson(record.getEncodedModelData(), itemType);
    }

    /**
     * A {@link TypeAdapterFactory} which generates a {@link TypeAdapter} for use
     * with {@link Class}-type objects.
     */
    static final class ClassTypeAdapterFactory implements TypeAdapterFactory {
        @SuppressWarnings("unchecked") // (TypeAdapter<T>)
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            if (!Class.class.isAssignableFrom(typeToken.getRawType())) {
                return null;
            }
            return (TypeAdapter<T>) new ClassTypeAdapter();
        }
    }

    /**
     * {@link PendingMutation} contains an {@link Class} member, but Gson doesn't
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
            final Class<?> clazz;
            try {
                clazz = Class.forName(jsonReader.nextString());
            } catch (ClassNotFoundException exception) {
                throw new IOException(exception);
            }
            return clazz;
        }
    }
}
