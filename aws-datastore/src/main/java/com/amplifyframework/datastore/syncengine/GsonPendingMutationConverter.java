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
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.util.GsonFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

/**
 * A utility to convert between {@link PendingMutation} and {@link PendingMutation.PersistentRecord}.
 */
final class GsonPendingMutationConverter implements PendingMutation.Converter {
    private final Gson gson;

    /**
     * Constructs a new instance of hte {@link GsonPendingMutationConverter}.
     */
    GsonPendingMutationConverter() {
        this.gson = GsonFactory.instance()
            .newBuilder()
            .registerTypeAdapter(TimeBasedUuid.class, new TimeBasedUuidTypeAdapter())
            .create();
    }

    @NonNull
    @Override
    public <T extends Model> PendingMutation.PersistentRecord toRecord(@NonNull PendingMutation<T> mutation) {
        return PendingMutation.PersistentRecord.builder()
            .containedModelId(mutation.getMutatedItem().getId())
            .containedModelClassName(mutation.getClassOfMutatedItem().getName())
            .serializedMutationData(gson.toJson(mutation))
            .mutationId(mutation.getMutationId())
            .build();
    }

    @NonNull
    @Override
    public <T extends Model> PendingMutation<T> fromRecord(
            @NonNull PendingMutation.PersistentRecord record) throws DataStoreException {
        final Class<?> itemClass;
        try {
            itemClass = Class.forName(record.getContainedModelClassName());
        } catch (ClassNotFoundException classNotFoundException) {
            throw new DataStoreException(
                "Could not find a class with the name " + record.getContainedModelClassName(),
                classNotFoundException,
                "Verify that you have built this model into your project."
            );
        }
        final Type itemType =
            TypeToken.getParameterized(PendingMutation.class, itemClass).getType();
        return gson.fromJson(record.getSerializedMutationData(), itemType);
    }
}
