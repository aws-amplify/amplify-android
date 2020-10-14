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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;

import java.util.Objects;

/**
 * Represents a conflict between two models and their associated
 * metadata, that has occurred during model synchronization. One was found locally,
 * and another found in the remote system.
 * @param <T> The type of the model instances that conflict (they are both of the same type.)
 */
public final class ConflictData<T extends Model> {
    private final ModelWithMetadata<T> local;
    private final ModelWithMetadata<T> remote;

    private ConflictData(ModelWithMetadata<T> local, ModelWithMetadata<T> remote) {
        this.local = local;
        this.remote = remote;
    }

    /**
     * Creates a new {@link ConflictData}.
     * @param local The instance of a model (and its metadata) that was found locally
     * @param remote The instance of a model (and its metadata) that was found on the server
     * @param <T> The type of the model instances experiencing conflict
     * @return Data about a model conflict
     */
    @NonNull
    public static <T extends Model> ConflictData<T> create(
            @NonNull ModelWithMetadata<T> local, @NonNull ModelWithMetadata<T> remote) {
        Objects.requireNonNull(local);
        Objects.requireNonNull(remote);
        return new ConflictData<>(local, remote);
    }

    /**
     * Gets the local model and its associated metadata.
     * @return Local model and its associated metadata
     */
    @NonNull
    public ModelWithMetadata<T> getLocal() {
        return this.local;
    }

    /**
     * Gets the remote model and its associated metadata.
     * @return Remote model and its associated metadata
     */
    @NonNull
    public ModelWithMetadata<T> getRemote() {
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

        ConflictData<?> that = (ConflictData<?>) thatObject;

        if (!getLocal().equals(that.getLocal())) {
            return false;
        }
        return getRemote().equals(that.getRemote());
    }

    @Override
    public int hashCode() {
        int result = getLocal().hashCode();
        result = 31 * result + getRemote().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DataStoreConflictData{" +
            "local=" + local +
            ", remote=" + remote +
            '}';
    }
}
