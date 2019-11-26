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

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;

import java.util.Objects;
import java.util.UUID;

/**
 * A StorageItemChange is a notification of a change to an item in an {@link LocalStorageAdapter}
 * implementation. StorageItemChange events should be emitted by the LocalStorageAdapter whenever
 * a stored item is changed.
 * @param <T> The type of the item that has changed
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class StorageItemChange<T extends Model> {
    private final UUID changeId;
    private final Initiator initiator;
    private final Type type;
    private final T item;
    private final Class<T> itemClass;

    private StorageItemChange(
            @NonNull UUID changeId,
            @NonNull Initiator initiator,
            @NonNull Type type,
            @NonNull T item,
            @NonNull Class<T> itemClass) {
        this.changeId = changeId;
        this.initiator = initiator;
        this.type = type;
        this.item = item;
        this.itemClass = itemClass;
    }

    /**
     * Gets the ID of this change.
     * @return Unique ID for this change
     */
    public UUID changeId() {
        return changeId;
    }

    /**
     * Gets an identification of the actor who initiated this change.
     * @return Identification of actor who initiated the change
     */
    public Initiator initiator() {
        return initiator;
    }

    /**
     * Gets the type of change.
     * @return Type of change
     */
    public Type type() {
        return type;
    }

    /**
     * Gets a representation of the item that changed.
     * For saves, this is the item as it was after the save.
     * For deletions, this is the item as it was before the deletion.
     * @return Representation of item that changed
     */
    public T item() {
        return item;
    }

    /**
     * Gets the class of the changed item.
     * @return Class of changed item
     */
    public Class<T> itemClass() {
        return itemClass;
    }

    /**
     * Gets an instance of an {@link StorageItemChange.Builder}.
     * @param <T> Type of item that has changed
     * @return A builder of StorageItemChange.
     */
    public static <T extends Model> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Convert this instance to a {@link StorageItemChange.Record} using the
     * supplied {@link RecordFactory} as a conversion strategy.
     * @param recordFactory A component capable of generating records.
     * @return A Record representation of this instance.
     */
    public Record toRecord(@NonNull RecordFactory recordFactory) {
        return Objects.requireNonNull(recordFactory).toRecord(this);
    }

    @Override
    public boolean equals(Object thatObject) {
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
        if (!item.equals(that.item)) {
            return false;
        }
        return itemClass.equals(that.itemClass);
    }

    @SuppressWarnings("checkstyle:MagicNumber") // 31 is IDE-generated
    @Override
    public int hashCode() {
        int result = changeId.hashCode();
        result = 31 * result + initiator.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + item.hashCode();
        result = 31 * result + itemClass.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "StorageItemChange{" +
            "changeId=" + changeId +
            ", initiator=" + initiator +
            ", type=" + type +
            ", item=" + item +
            ", itemClass=" + itemClass +
            '}';
    }

    /**
     * A utility for fluent construction of {@link StorageItemChange}.
     * @param <T> Type of item to be included in built StorageItemChanges.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static final class Builder<T extends Model> {
        private UUID changeId;
        private Initiator initiator;
        private Type type;
        private T item;
        private Class<T> itemClass;

        /**
         * Use a particular ID as the change ID.
         * This value will be used for the next {@link #build()} call only.
         * After that, the default behavior of using a random UUID will be used.
         * @param changeId A string representation of a Java UUID
         * @return Current Builder instance for fluent configuration chaining
         */
        public Builder<T> changeId(@NonNull String changeId) {
            this.changeId = UUID.fromString(Objects.requireNonNull(changeId));
            return this;
        }

        /**
         * Go back to the default behavior of choosing a new random UUID for
         * each newly generated {@link StorageItemChange}.
         * @return Current Builder instance for fluent configuration chaining
         */
        public Builder<T> randomChangeId() {
            this.changeId = null;
            return this;
        }

        /**
         * Configures an identification of the initiator of this change.
         * @param initiator An identification of the initiator of this change
         * @return Current Builder instance for fluent configuration chaining
         */
        public Builder<T> initiator(@NonNull Initiator initiator) {
            this.initiator = Objects.requireNonNull(initiator);
            return this;
        }

        /**
         * Configures the type of the change, e.g., {@link Type#SAVE}, etc.
         * @param type The type of change, e.g. {@link Type#DELETE}
         * @return Current Builder instance for fluent configuration chaining
         */
        public Builder<T> type(@NonNull Type type) {
            this.type = Objects.requireNonNull(type);
            return this;
        }

        /**
         * Configures the item that changed.
         * For a save, this is the item after the save.
         * For a delete, this is the item before the delete.
         * @param item Representation of the item that changed
         * @return Current Builder instance for fluent configuration chaining
         */
        public Builder<T> item(T item) {
            this.item = Objects.requireNonNull(item);
            return this;
        }

        /**
         * Configures the class of the item that changed, e.g., YourType.class.
         * @param itemClass Class of the item that changed
         * @return Current Builder instance for fluent configuration chaining
         */
        public Builder<T> itemClass(Class<T> itemClass) {
            this.itemClass = Objects.requireNonNull(itemClass);
            return this;
        }

        /**
         * Builds an instance of a StorageItemChange.
         * @return A new StorageItemChange instance.
         */
        public StorageItemChange<T> build() {
            final UUID usedId = changeId == null ? UUID.randomUUID() : changeId;
            randomChangeId();
            return new StorageItemChange<>(
                Objects.requireNonNull(usedId),
                Objects.requireNonNull(initiator),
                Objects.requireNonNull(type),
                Objects.requireNonNull(item),
                Objects.requireNonNull(itemClass)
            );
        }
    }

    /**
     * A Record of a StorageItemChange is just a StorageItemChange in its serialized form.
     * This is the type which the {@link LocalStorageAdapter} deals with directly.
     */
    @SuppressWarnings("unused")
    @ModelConfig(targetName = "StorageItemChangeRecord")
    @Index(fields = {"itemClass"}, name = "itemClassBasedIndex")
    public static final class Record implements Model {
        @ModelField(targetName = "id", targetType = "ID", isRequired = true)
        private final String id;

        @ModelField(targetName = "entry", targetType = "String", isRequired = true)
        private final String entry;

        @ModelField(targetName = "itemClass", targetType = "String", isRequired = true)
        private final String itemClass;

        /**
         * Constructs a Record.
         * Note: by arrangement of the {@link Builder#build()} method, it should be
         * impossible for any null value to be passed into this constructor.
         * @param id ID for the record
         * @param entry entry for record
         * @param itemClass Class of item held in entry
         */
        @SuppressWarnings("ParameterName") // "id" required by model parsing code
        private Record(String id, String entry, String itemClass) {
            this.id = id;
            this.entry = entry;
            this.itemClass = itemClass;
        }

        /**
         * Gets the ID of the record.
         * @return ID for record
         */
        @NonNull
        public String getId() {
            return this.id;
        }

        /**
         * Gets the record entry.
         * @return Record entry
         */
        @NonNull
        public String getEntry() {
            return this.entry;
        }

        /**
         * Gets the class of the item in the entry.
         * @return Class of item in entry
         */
        @NonNull
        public String getItemClass() {
            return itemClass;
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            Record record = (Record) thatObject;

            if (!id.equals(record.id)) {
                return false;
            }
            if (!entry.equals(record.entry)) {
                return false;
            }
            return itemClass.equals(record.itemClass);
        }

        @SuppressWarnings("checkstyle:MagicNumber") // 31 is IDE-generated
        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + entry.hashCode();
            result = 31 * result + itemClass.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Record{" +
                "id='" + id + '\'' +
                ", entry='" + entry + '\'' +
                ", itemClass='" + itemClass + '\'' +
                '}';
        }

        /**
         * Converts this record into a storage item change, using the provided factory implementation
         * as a conversion strategy.
         * @param factory A factory that can build storage item change from record
         * @param <T> The type of item being modeled in the change
         * @return A StorageItemChange representation of the record
         */
        public <T extends Model> StorageItemChange<T> toStorageItemChange(
                @NonNull StorageItemChangeFactory factory) {
            return Objects.requireNonNull(factory).fromRecord(this);
        }

        /**
         * Gets an instance of a {@link Record.Builder}.
         * @return A builder for Records
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Factory method to generate a record, given just an entry.
         * A random ID will be assigned.
         * @param entry Record entry
         * @return A record containing the entry, and with random ID.
         */
        public static Record forEntry(@NonNull String entry) {
            return Record.builder().entry(entry).build();
        }

        /**
         * Utility for fluent construction of Record.
         */
        @SuppressWarnings("UnusedReturnValue")
        public static final class Builder {
            private UUID id;
            private String entry;
            private String itemClass;

            /**
             * Configures the Record ID to a specified UUID value.
             * The value will be used up until the next invocation of {@link #build()}.
             * A random UUID will be used, after that, unless you explicitly use this
             * call again.
             * @param id A string interpretation of a Java UUID.
             * @return Current builder, for fluent configuration chaining
             */
            @SuppressWarnings("ParameterName") // "id" required by model parsing code; keep all same
            @NonNull
            public Builder id(@NonNull String id) {
                this.id = UUID.fromString(Objects.requireNonNull(id));
                return this;
            }

            /**
             * Configures the Builder to generate a random ID for the next Record.
             * This is the default behavior of the {@link Record.Builder}.
             * @return Current instance of the Builder, for fluent configuration chaining
             */
            @NonNull
            public Builder randomId() {
                this.id = null;
                return this;
            }

            /**
             * Configures the entry for the next Record instance.
             * @param entry Value for the {@link Record}'s entry
             * @return Current Builder instance, for fluent configuration chaining
             */
            @NonNull
            public Builder entry(@NonNull String entry) {
                this.entry = Objects.requireNonNull(entry);
                return this;
            }

            /**
             * Configures the class of item contained in Record.
             * @param itemClass Class of item in record
             * @return Current builder for fluent method chaining
             */
            @NonNull
            public Builder itemClass(@NonNull String itemClass) {
                this.itemClass = Objects.requireNonNull(itemClass);
                return this;
            }

            /**
             * Builds a Record using the configured properties.
             * @return A new instance of a {@link Record}.
             */
            @NonNull
            public Record build() {
                final UUID usedId = id == null ? UUID.randomUUID() : id;
                randomId();
                return new Record(
                    Objects.requireNonNull(usedId).toString(),
                    Objects.requireNonNull(entry),
                    Objects.requireNonNull(itemClass)
                );
            }
        }
    }

    /**
     * An identification of the initiator of the change; in other words,
     * this is an identification of who caused the item to be changed.
     */
    @SuppressWarnings("unused")
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
     * A type of change, e.g., one of the possible operations that can be
     * invoked on {@link LocalStorageAdapter} to change a stored item.
     */
    public enum Type {
        /**
         * An item was saved into storage.
         * This may have been an insert or an update ("upsert").
         */
        SAVE,

        /**
         * An item was deleted from storage.
         */
        DELETE
    }

    /**
     * An abstraction for an external actor who has the ability to transform
     * instances of {@link StorageItemChange} into {@link StorageItemChange.Record}.
     */
    public interface RecordFactory {
        /**
         * Serializes a StorageItemChange into a StorageItemChange.Record.
         * @param storageItemChange A storage item change instance, to be made into a record
         * @return A Record corresponding to the storage item change.
         * @param <T> Type of item being kept in the StorageItemChange.
         */
        <T extends Model> Record toRecord(StorageItemChange<T> storageItemChange);
    }

    /**
     * An abstraction for an external actor who has the ability to transform
     * instances of {@link Record} into {@link StorageItemChange}.
     */
    public interface StorageItemChangeFactory {

        /**
         * De-serializes a {@link Record} into a {@link StorageItemChange} with
         * an wildcard parameter type.
         * @param record Record to deserialize
         * @param <T> Type of item represented inside of the change record
         * @return A {@link StorageItemChange} representation of provided record
         */
        <T extends Model> StorageItemChange<T> fromRecord(Record record);
    }
}
