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

package com.amplifyframework.datastore;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.Model;

import java.util.Objects;
import java.util.UUID;

/**
 * A DataStoreItemChange is emitted whenever there is a change to an item in the DataStore.
 * @param <T> The type of the item that underwent changed
 */
public final class DataStoreItemChange<T extends Model> {
    private final UUID uuid;
    private final Type type;
    private final T item;
    private final Class<T> itemClass;
    private final Initiator initiator;

    private DataStoreItemChange(
            UUID uuid,
            Type type,
            T item,
            Class<T> itemClass,
            Initiator initiator) {
        this.uuid = uuid;
        this.type = type;
        this.item = item;
        this.itemClass = itemClass;
        this.initiator = initiator;
    }

    /**
     * Gets a universally unique identifier for this change.
     * @return A universally-unique identifier for this change
     */
    @NonNull
    public UUID uuid() {
        return uuid;
    }

    /**
     * Gets the type of change, e.g. {@link Type#SAVE} or {@link Type#DELETE}.
     * @return Type of change
     */
    @NonNull
    public Type type() {
        return type;
    }

    /**
     * Gets the item that changed. In the case of a save,
     * this is reference to the newly saved object. In the case of a deletion,
     * this is an in-memory snapshot of an item that was just deleted,
     * and no longer exists in the DataStore.
     * @return The item that changed
     */
    @NonNull
    public T item() {
        return item;
    }

    /**
     * Gets the class type of the item.
     * @return Class type of item
     */
    @NonNull
    public Class<T> itemClass() {
        return itemClass;
    }

    /**
     * Gets an identification of the actor who initiated this change.
     * @return An identification of the actor who initiated this change
     */
    @NonNull
    public Initiator initiator() {
        return initiator;
    }

    /**
     * Gets an instance of the Builder.
     * @param <T> class of the item that changed
     * @return Builder instance
     */
    @NonNull
    public static <T extends Model> Builder<T> builder() {
        return new Builder<>();
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        DataStoreItemChange<?> that = (DataStoreItemChange<?>) thatObject;

        if (!uuid.equals(that.uuid)) {
            return false;
        }
        if (type != that.type) {
            return false;
        }
        if (!item.equals(that.item)) {
            return false;
        }
        if (!itemClass.equals(that.itemClass)) {
            return false;
        }
        return initiator == that.initiator;
    }

    @SuppressWarnings("checkstyle:MagicNumber") // 31 is IDE-generated
    @Override
    public int hashCode() {
        int result = uuid.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + item.hashCode();
        result = 31 * result + itemClass.hashCode();
        result = 31 * result + initiator.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DataStoreItemChange{" +
            "uuid=" + uuid +
            ", type=" + type +
            ", item=" + item +
            ", itemClass=" + itemClass +
            ", initiator=" + initiator +
            '}';
    }

    /**
     * A utility to construct instances of DataStoreItemChange.
     * @param <T> Class of the item that changed
     */
    @SuppressWarnings({"UnusedReturnValue", "WeakerAccess"})
    public static final class Builder<T extends Model> {
        private UUID uuid;
        private Type type;
        private T item;
        private Class<T> itemClass;
        private Initiator initiator;

        /**
         * Configures the UUID that will be placed into the next DataStoreItemChange instance.
         * After the next call to {@link #build()}, this value will be thrown out, and
         * random UUIDs will be assigned again.
         * @param uuid A UUID to assign in the next DataStoreChangeEvent that is generated
         * @return Current Builder instance, for fluent method chaining
         */
        public Builder<T> uuid(@NonNull final String uuid) {
            this.uuid = UUID.fromString(Objects.requireNonNull(uuid));
            return this;
        }

        /**
         * Configures the builder to use a random UUID for DataStoreItemChange instances.
         * @return The current instance of the Builder, for fluent method chaining
         */
        public Builder<T> randomUuid() {
            this.uuid = null;
            return this;
        }

        /**
         * Configures the type that will be set in the newly built DataStoreItemChange.
         * @param type Type of change to set in DataStoreItemChange
         * @return Current builder instance for fluent chaining
         */
        @NonNull
        public Builder<T> type(@NonNull final Type type) {
            this.type = Objects.requireNonNull(type);
            return this;
        }

        /**
         * Configures the item that will be placed into the built DataStoreItemChange.
         * @param item The item that has been changed
         * @return Current builder instance for fluent chaining
         */
        @NonNull
        public Builder<T> item(@NonNull final T item) {
            this.item = Objects.requireNonNull(item);
            return this;
        }

        /**
         * Configures the class of the item that will be stored into
         * any new DataStoreItemChange generated from this Builder.
         * @param itemClass The class of the changed item
         * @return Current builder instance for fluent chaining
         */
        @NonNull
        public Builder<T> itemClass(@NonNull final Class<T> itemClass) {
            this.itemClass = Objects.requireNonNull(itemClass);
            return this;
        }

        /**
         * Configures an identification of the initiator of change;
         * this value will be used for all newly built DataStoreItemChange.
         * @param initiator Initiator of change
         * @return Current builder instance for fluent chaining
         */
        public Builder<T> initiator(final Initiator initiator) {
            this.initiator = initiator;
            return this;
        }

        /**
         * Builds an instance of a DataStoreItemChange using the provided
         * configuration options currently stored in the the builder.
         * @return A new DataStoreItemChange instance
         */
        public DataStoreItemChange<T> build() {
            final UUID usedId = uuid == null ? UUID.randomUUID() : uuid;
            randomUuid();
            return new DataStoreItemChange<>(
                Objects.requireNonNull(usedId),
                Objects.requireNonNull(type),
                Objects.requireNonNull(item),
                Objects.requireNonNull(itemClass),
                Objects.requireNonNull(initiator)
            );
        }
    }

    /**
     * A DataStoreItemChange can either be caused by a call to one of the
     * {@link DataStoreCategoryBehavior#save(Model)} family of methods,
     * or to one of the {@link DataStoreCategoryBehavior#delete(Model)}
     * methods.
     */
    public enum Type {
        /**
         * An item has been saved into the DataStore.
         */
        SAVE,

        /**
         * An existing item has been deleted from the DataStore.
         */
        DELETE
    }

    /**
     * The initiator of the change.
     */
    public enum Initiator {

        /**
         * The change was initiated by a call to the {@link DataStoreCategory} API.
         */
        LOCAL,

        /**
         * The change was initiated by a remote system, to which we listen for change notifications.
         */
        REMOTE
    }
}
