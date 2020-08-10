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
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;

import java.util.Objects;
import java.util.UUID;

/**
 * Metadata about the last time that a model class was sync'd with AppSync backend.
 * This metadata is persisted locally as a system model. This metadata is inspected
 * whenever the Sync Engine starts up. The system consider the value of
 * {@link LastSyncMetadata#getLastSyncTime()} to decide whether or not it should
 * perform a "Base Sync" or a "Delta Sync".
 */
@ModelConfig
public final class LastSyncMetadata implements Model {
    private final @ModelField(targetType = "ID", isRequired = true) String id;
    private final @ModelField(targetType = "String", isRequired = true) String modelClassName;
    private final @ModelField(targetType = "AWSTimestamp", isRequired = true) Long lastSyncTime;
    private final @ModelField(targetType = "String", isRequired = true) String lastSyncType;

    @SuppressWarnings("checkstyle:ParameterName") // The field is named "id" in the model; keep it consistent
    private LastSyncMetadata(String id, String modelClassName, Long lastSyncTime, SyncType syncType) {
        this.id = id;
        this.modelClassName = modelClassName;
        this.lastSyncTime = lastSyncTime;
        this.lastSyncType = syncType.name();
    }

    /**
     * Creates an instance of an {@link LastSyncMetadata}, indicating that the provided
     * model has been sync'd, and that the last sync occurred at the given time.
     * @param modelClass Class of model
     * @param lastSyncTime Last time it was synced
     * @return {@link LastSyncMetadata} for the model class
     */
    static <T extends Model> LastSyncMetadata lastSyncedAt(@NonNull Class<T> modelClass,
                                                           @Nullable long lastSyncTime,
                                                           @NonNull SyncType syncType) {
        Objects.requireNonNull(modelClass);
        return create(modelClass, lastSyncTime, syncType);
    }

    /**
     * Creates an {@link LastSyncMetadata} indicating that the provided model class
     * has never been synced.
     * @param modelClass Class of model to which this metadata applies
     * @param <T> Type of model
     * @return {@link LastSyncMetadata}
     */
    static <T extends Model> LastSyncMetadata neverSynced(@NonNull Class<T> modelClass) {
        Objects.requireNonNull(modelClass);
        return create(modelClass, null, SyncType.BASE);
    }

    /**
     * Creates an {@link LastSyncMetadata} for the provided model class.
     * @param modelClass Class of model for which metadata pertains
     * @param lastSyncTime Time of last sync; null, if never.
     * @param syncType The type of sync (FULL or DELTA).
     * @param <T> Type of model
     * @return {@link LastSyncMetadata}
     */
    @SuppressWarnings("WeakerAccess")
    static <T extends Model> LastSyncMetadata create(
            @NonNull Class<T> modelClass, @Nullable Long lastSyncTime, @NonNull SyncType syncType) {
        Objects.requireNonNull(modelClass);
        String modelClassName = modelClass.getSimpleName();
        return new LastSyncMetadata(hash(modelClassName), modelClassName, lastSyncTime, syncType);
    }

    @NonNull
    @Override
    public String getId() {
        return this.id;
    }

    /**
     * Gets the name of the model class to which this metadata applies.
     * @return Name of model class associated with this metadata
     */
    @NonNull
    String getModelClassName() {
        return this.modelClassName;
    }

    /**
     * Gets the last time at which the model of name {@link #getModelClassName()}
     * was sync'd.
     * @return Last sync time for model
     */
    Long getLastSyncTime() {
        return this.lastSyncTime;
    }

    /**
     * Returns the type of sync that was last performed.
     * @return Either BASE or FULL.
     */
    public String getLastSyncType() {
        return lastSyncType;
    }

    /**
     * Computes a stable hash for a model class, by its name.
     * Since {@link Model}s have to have unique IDs, we need an ID for this class.
     * However, ideally, we wouldn't use it. Ideally, we'd use the model class name.
     * @param modelClassName Name of model class
     * @return hash for model class
     */
    private static String hash(@NonNull String modelClassName) {
        return UUID.nameUUIDFromBytes(modelClassName.getBytes()).toString();
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        LastSyncMetadata that = (LastSyncMetadata) thatObject;

        if (!ObjectsCompat.equals(id, that.id)) {
            return false;
        }
        if (!ObjectsCompat.equals(modelClassName, that.modelClassName)) {
            return false;
        }
        if (!ObjectsCompat.equals(lastSyncType, that.lastSyncType)) {
            return false;
        }
        return ObjectsCompat.equals(lastSyncTime, that.lastSyncTime);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + modelClassName.hashCode();
        result = 31 * result + lastSyncTime.hashCode();
        result = 31 * result + lastSyncType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "LastSyncMetadata{" +
            "id='" + id + '\'' +
            ", modelClassName='" + modelClassName + '\'' +
            ", lastSyncTime=" + lastSyncTime +
            ", lastSyncType=" + lastSyncType +
            '}';
    }
}
