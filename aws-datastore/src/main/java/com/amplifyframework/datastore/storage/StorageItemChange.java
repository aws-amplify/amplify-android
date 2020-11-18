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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

import java.util.Objects;
import java.util.UUID;

/**
 * A StorageItemChange is a notification of a change to an item in an {@link LocalStorageAdapter}
 * implementation. StorageItemChange events should be emitted by the LocalStorageAdapter whenever
 * a stored item is changed.
 * @param <T> The type of the item that has changed
 */
public final class StorageItemChange<T extends Model> {
    private final UUID changeId;
    private final Initiator initiator;
    private final Type type;
    private final QueryPredicate predicate;
    private final T item;
    private final ModelSchema modelSchema;

    private StorageItemChange(
            UUID changeId,
            Initiator initiator,
            Type type,
            QueryPredicate predicate,
            T item,
            ModelSchema modelSchema) {
        this.changeId = changeId;
        this.initiator = initiator;
        this.type = type;
        this.predicate = predicate;
        this.item = item;
        this.modelSchema = modelSchema;
    }

    /**
     * Gets the ID of this change.
     * @return Unique ID for this change
     */
    @NonNull
    public UUID changeId() {
        return changeId;
    }

    /**
     * Gets an identification of the actor who initiated this change.
     * @return Identification of actor who initiated the change
     */
    @NonNull
    public Initiator initiator() {
        return initiator;
    }

    /**
     * Gets the type of change.
     * @return Type of change
     */
    @NonNull
    public Type type() {
        return type;
    }

    /**
     * Gets the predicate that was applied before this item change.
     * @return Predicate applied for this item change
     */
    @NonNull
    public QueryPredicate predicate() {
        return predicate;
    }

    /**
     * Gets a representation of the item that changed.
     * For saves, this is the item as it was after the save.
     * For deletions, this is the item as it was before the deletion.
     * @return Representation of item that changed
     */
    @NonNull
    public T item() {
        return item;
    }

    /**
     * Gets the schema of the changed item.
     * @return Schema of changed item
     */
    @NonNull
    public ModelSchema modelSchema() {
        return modelSchema;
    }

    /**
     * Gets an instance of an {@link StorageItemChange.Builder}.
     * @param <T> Type of item that has changed
     * @return A builder of StorageItemChange.
     */
    @NonNull
    public static <T extends Model> Builder<T> builder() {
        return new Builder<>();
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        StorageItemChange<?> that = (StorageItemChange<?>) thatObject;

        if (!changeId.equals(that.changeId)) {
            return false;
        }
        if (initiator != that.initiator) {
            return false;
        }
        if (type != that.type) {
            return false;
        }
        if (!predicate.equals(that.predicate)) {
            return false;
        }
        if (!item.equals(that.item)) {
            return false;
        }
        return modelSchema.equals(that.modelSchema);
    }

    @Override
    public int hashCode() {
        int result = changeId.hashCode();
        result = 31 * result + initiator.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + predicate.hashCode();
        result = 31 * result + item.hashCode();
        result = 31 * result + modelSchema.hashCode();
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "StorageItemChange{" +
                "changeId=" + changeId +
                ", initiator=" + initiator +
                ", type=" + type +
                ", predicate=" + predicate +
                ", item=" + item +
                ", modelSchema=" + modelSchema +
                '}';
    }

    /**
     * A utility for fluent construction of {@link StorageItemChange}.
     * @param <T> Type of item to be included in built StorageItemChanges.
     */
    public static final class Builder<T extends Model> {
        private UUID changeId;
        private Initiator initiator;
        private Type type;
        private QueryPredicate predicate;
        private T item;
        private ModelSchema modelSchema;

        /**
         * Use a particular ID as the change ID.
         * This value will be used for the next {@link #build()} call only.
         * After that, the default behavior of using a random UUID will be used.
         * @param changeId A string representation of a Java UUID
         * @return Current Builder instance for fluent configuration chaining
         */
        @NonNull
        public Builder<T> changeId(@NonNull String changeId) {
            this.changeId = UUID.fromString(Objects.requireNonNull(changeId));
            return this;
        }

        /**
         * Go back to the default behavior of choosing a new random UUID for
         * each newly generated {@link StorageItemChange}.
         * @return Current Builder instance for fluent configuration chaining
         */
        @SuppressWarnings({"UnusedReturnValue", "WeakerAccess"})
        @NonNull
        public Builder<T> randomChangeId() {
            this.changeId = null;
            return this;
        }

        /**
         * Configures an identification of the initiator of this change.
         * @param initiator An identification of the initiator of this change
         * @return Current Builder instance for fluent configuration chaining
         */
        @NonNull
        public Builder<T> initiator(@NonNull Initiator initiator) {
            this.initiator = Objects.requireNonNull(initiator);
            return this;
        }

        /**
         * Configures the type of the change, e.g., {@link Type#CREATE}, etc.
         * @param type The type of change, e.g. {@link Type#DELETE}
         * @return Current Builder instance for fluent configuration chaining
         */
        @NonNull
        public Builder<T> type(@NonNull Type type) {
            this.type = Objects.requireNonNull(type);
            return this;
        }

        /**
         * Configures the predicate that was applied before making the change.
         * @param predicate The predicate that was applied for this change.
         * @return Current Builder instance for fluent configuration chainging
         */
        @NonNull
        public Builder<T> predicate(@NonNull QueryPredicate predicate) {
            this.predicate = Objects.requireNonNull(predicate);
            return this;
        }

        /**
         * Configures the item that changed.
         * For a save, this is the item after the save.
         * For a delete, this is the item before the delete.
         * @param item Representation of the item that changed
         * @return Current Builder instance for fluent configuration chaining
         */
        @NonNull
        public Builder<T> item(@NonNull T item) {
            this.item = Objects.requireNonNull(item);
            return this;
        }

        /**
         * Configures the schema of the item that changed.
         * @param modelSchema Schema of the item that changed
         * @return Current Builder instance for fluent configuration chaining
         */
        @NonNull
        public Builder<T> modelSchema(@NonNull ModelSchema modelSchema) {
            this.modelSchema = Objects.requireNonNull(modelSchema);
            return this;
        }

        /**
         * Builds an instance of a StorageItemChange.
         * @return A new StorageItemChange instance.
         */
        @NonNull
        public StorageItemChange<T> build() {
            final UUID usedId = changeId == null ? UUID.randomUUID() : changeId;
            randomChangeId();
            return new StorageItemChange<>(
                Objects.requireNonNull(usedId),
                Objects.requireNonNull(initiator),
                Objects.requireNonNull(type),
                Objects.requireNonNull(predicate),
                Objects.requireNonNull(item),
                Objects.requireNonNull(modelSchema)
            );
        }
    }

    /**
     * An identification of the initiator of the change; in other words,
     * this is an identification of who caused the item to be changed.
     */
    public enum Initiator {
        /**
         * The Sync Engine requested this change.
         */
        SYNC_ENGINE,

        /**
         * This change was propagated down from the DataStore API.
         */
        DATA_STORE_API
    }

    /**
     * An outcome of one of the save/delete operations on the {@link LocalStorageAdapter}.
     */
    public enum Type {
        /**
         * A new item was created into storage.
         */
        CREATE,

        /**
         * An existing item was updated.
         */
        UPDATE,

        /**
         * An item was deleted from storage.
         */
        DELETE
    }
}
