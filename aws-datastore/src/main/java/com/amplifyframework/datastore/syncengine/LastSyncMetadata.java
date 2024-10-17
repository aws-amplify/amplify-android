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
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

import java.util.Objects;
import java.util.UUID;

/**
 * Metadata about the last time that a model class was sync'd with AppSync backend using a certain syncExpression.
 * This metadata is persisted locally as a system model. This metadata is inspected
 * whenever the Sync Engine starts up. The system consider the value of
 * {@link LastSyncMetadata#getLastSyncTime()} to decide whether or not it should
 * perform a "Base Sync" or a "Delta Sync".
 */
@ModelConfig(type = Model.Type.SYSTEM)
public final class LastSyncMetadata implements Model {
    private final @ModelField(targetType = "ID", isRequired = true) String id;
    private final @ModelField(targetType = "String", isRequired = true) String modelClassName;
    private final @ModelField(targetType = "AWSTimestamp", isRequired = true) Long lastSyncTime;
    private final @ModelField(targetType = "String", isRequired = true) String lastSyncType;
    private final @ModelField(targetType = "String") QueryPredicate syncExpression;

    @SuppressWarnings("checkstyle:ParameterName") // The field is named "id" in the model; keep it consistent
    private LastSyncMetadata(String id, String modelClassName, Long lastSyncTime,
                             SyncType syncType, @Nullable QueryPredicate syncExpression) {
        this.id = id;
        this.modelClassName = modelClassName;
        this.lastSyncTime = lastSyncTime;
        this.lastSyncType = syncType.name();
        this.syncExpression = syncExpression;
    }

    /**
     * Creates an instance of an {@link LastSyncMetadata}, indicating that the provided
     * model has been base sync'd, and that the last sync occurred at the given time.
     * @param modelClassName Name of model
     * @param lastSyncTime Last time it was synced
     * @param syncExpression the corresponding sync expression being used during last sync
     * @param <T> t type of Model.
     * @return {@link LastSyncMetadata} for the model class
     */
    public static <T extends Model> LastSyncMetadata baseSyncedAt(@NonNull String modelClassName,
                                                                  @Nullable long lastSyncTime,
                                                                  @Nullable QueryPredicate syncExpression) {
        Objects.requireNonNull(modelClassName);
        return create(modelClassName, lastSyncTime, SyncType.BASE, syncExpression);
    }

    /**
     * Creates an instance of an {@link LastSyncMetadata}, indicating that the provided
     * model has been base delta sync'd, and that the last sync occurred at the given time.
     * @param modelClassName Name of model
     * @param lastSyncTime Last time it was synced
     * @param syncExpression the corresponding sync expression being used during last sync
     * @param <T> t type of Model.
     * @return {@link LastSyncMetadata} for the model class
     */
    static <T extends Model> LastSyncMetadata deltaSyncedAt(@NonNull String modelClassName,
                                                            @Nullable long lastSyncTime,
                                                            @Nullable QueryPredicate syncExpression) {
        Objects.requireNonNull(modelClassName);
        return create(modelClassName, lastSyncTime, SyncType.DELTA, syncExpression);
    }

    /**
     * Creates an {@link LastSyncMetadata} indicating that the provided model class
     * has never been synced.
     * @param modelClassName Name of model class to which this metadata applies
     * @param <T> Type of model
     * @return {@link LastSyncMetadata}
     */
    public static <T extends Model> LastSyncMetadata neverSynced(@NonNull String modelClassName) {
        Objects.requireNonNull(modelClassName);
        return create(modelClassName, null, SyncType.BASE, null);
    }

    /**
     * Creates an {@link LastSyncMetadata} for the provided model class.
     * @param modelClassName Name of model class for which metadata pertains
     * @param lastSyncTime Time of last sync; null, if never.
     * @param syncType The type of sync (FULL or DELTA).
     * @param syncExpression the corresponding sync expression being used during last sync
     * @param <T> Type of model
     * @return {@link LastSyncMetadata}
     */
    @SuppressWarnings("WeakerAccess")
    static <T extends Model> LastSyncMetadata create(@NonNull String modelClassName,
                                                     @Nullable Long lastSyncTime,
                                                     @NonNull SyncType syncType,
                                                     @Nullable QueryPredicate syncExpression) {
        Objects.requireNonNull(modelClassName);
        return new LastSyncMetadata(hash(modelClassName), modelClassName, lastSyncTime, syncType, syncExpression);
    }

    @NonNull
    @Override
    public String resolveIdentifier() {
        return this.id;
    }


    /**
     * Gets the Id of the Model.
     * @return The Id of the model.
     */
    @NonNull
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
    public Long getLastSyncTime() {
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
     * Returns the sync expression being used in the last sync.
     * @return A serialized sync expression
     */
    public QueryPredicate getSyncExpression() {
        return this.syncExpression;
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
        if (!ObjectsCompat.equals(syncExpression, that.syncExpression)) {
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
        result = 31 * result + syncExpression.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "LastSyncMetadata{" +
            "id='" + id + '\'' +
            ", modelClassName='" + modelClassName + '\'' +
            ", lastSyncTime=" + lastSyncTime +
            ", lastSyncType=" + lastSyncType +
            ", syncExpression=" + syncExpression +
            '}';
    }
}
