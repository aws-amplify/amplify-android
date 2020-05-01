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

import com.amplifyframework.core.model.Model;

import java.util.Objects;

/**
 * Contains data pertaining to a conflict between two models, that has occurred
 * during model synchronization. One was found locally, and another found in the remote system.
 * @param <T> The type of the model instances that conflict (the are both of the same type.)
 */
public final class DataStoreConflictData<T extends Model> {
    private final T local;
    private final T remote;

    private DataStoreConflictData(T local, T remote) {
        this.local = local;
        this.remote = remote;
    }

    /**
     * Creates a new {@link DataStoreConflictData}.
     * @param local The instance of a model that was found locally
     * @param remote The instance of a model that was found on the server
     * @param <T> The type of the model instances experiencing conflict
     * @return Data about a model conflict
     */
    @NonNull
    public static <T extends Model> DataStoreConflictData<T> create(@NonNull T local, @NonNull T remote) {
        Objects.requireNonNull(local);
        Objects.requireNonNull(remote);
        return new DataStoreConflictData<>(local, remote);
    }

    /**
     * Gets the local model.
     * @return Local model
     */
    @NonNull
    public T getLocal() {
        return this.local;
    }

    /**
     * Gets the remote model.
     * @return Remote model
     */
    @NonNull
    public T getRemote() {
        return this.remote;
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        DataStoreConflictData<?> that = (DataStoreConflictData<?>) thatObject;

        if (!ObjectsCompat.equals(local, that.local)) {
            return false;
        }

        return ObjectsCompat.equals(remote, that.remote);
    }

    @Override
    public int hashCode() {
        int result = local.hashCode();
        result = 31 * result + remote.hashCode();
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "DataStoreConflictData{" +
            "local=" + local +
            ", remote=" + remote +
            '}';
    }
}
