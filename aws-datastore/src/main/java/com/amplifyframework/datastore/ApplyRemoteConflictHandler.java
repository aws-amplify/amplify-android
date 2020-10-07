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

package com.amplifyframework.datastore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;

import java.util.Objects;

/**
 * A default implementation of the {@link DataStoreConflictHandler},
 * which discards the local data, in favor of whatever was on the server.
 */
public final class ApplyRemoteConflictHandler implements DataStoreConflictHandler {
    private final DataStoreCategoryBehavior dataStore;
    private final DataStoreErrorHandler dataStoreErrorHandler;

    private ApplyRemoteConflictHandler(
            DataStoreCategoryBehavior dataStore,
            DataStoreErrorHandler dataStoreErrorHandler) {
        this.dataStore = dataStore;
        this.dataStoreErrorHandler = dataStoreErrorHandler;
    }

    /**
     * Creates a new instance of the {@link ApplyRemoteConflictHandler}.
     * This handler discards local data in preference of remote data.
     * @param dataStoreErrorHandler DataStore error handler
     * @return A {@link ApplyRemoteConflictHandler}
     */
    @NonNull
    public static ApplyRemoteConflictHandler instance(@NonNull DataStoreErrorHandler dataStoreErrorHandler) {
        Objects.requireNonNull(dataStoreErrorHandler);
        return new ApplyRemoteConflictHandler(Amplify.DataStore, dataStoreErrorHandler);
    }

    /**
     * Creates a new instance of the {@link ApplyRemoteConflictHandler}.
     * @param dataStore DataStore instance to interact with when resolving conflict
     * @param errorHandler DataStore error handler instance
     * @return A {@link ApplyRemoteConflictHandler}
     */
    @NonNull
    public static ApplyRemoteConflictHandler instance(
            @NonNull DataStoreCategoryBehavior dataStore, @NonNull DataStoreErrorHandler errorHandler) {
        Objects.requireNonNull(dataStore);
        Objects.requireNonNull(errorHandler);
        return new ApplyRemoteConflictHandler(dataStore, errorHandler);
    }

    @Override
    public <T extends Model> void resolveConflict(
            @NonNull DataStoreConflictData<T> conflictData,
            @NonNull Consumer<DataStoreConflictHandlerResult> onConflictResolved) {
        dataStore.delete(conflictData.getLocal().getModel(),
            deleted -> dataStore.save(conflictData.getRemote().getModel(),
                saved -> onConflictResolved.accept(DataStoreConflictHandlerResult.APPLY_REMOTE),
                dataStoreErrorHandler
            ),
            dataStoreErrorHandler
        );
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }
        ApplyRemoteConflictHandler that = (ApplyRemoteConflictHandler) thatObject;
        return ObjectsCompat.equals(dataStoreErrorHandler, that.dataStoreErrorHandler);
    }

    @Override
    public int hashCode() {
        return dataStoreErrorHandler != null ? dataStoreErrorHandler.hashCode() : 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "ApplyRemoteConflictHandler{" +
            "dataStoreErrorHandler=" + dataStoreErrorHandler +
            '}';
    }
}
