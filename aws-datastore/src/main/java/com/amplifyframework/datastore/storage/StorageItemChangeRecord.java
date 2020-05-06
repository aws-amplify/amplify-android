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
 * A Record of a StorageItemChange is just a StorageItemChange in its serialized form.
 * This is the type which the {@link LocalStorageAdapter} deals with directly.
 */
@ModelConfig(pluralName = "StorageItemChangeRecords")
@Index(fields = "itemClass", name = "itemClassBasedIndex")
public final class StorageItemChangeRecord implements Model {
    @ModelField(targetType = "ID", isRequired = true)
    private final String id;

    @ModelField(targetType = "String", isRequired = true)
    private final String entry;

    @ModelField(targetType = "String", isRequired = true)
    private final String itemClass;

    /**
     * Constructs a Record.
     * Note: by arrangement of the {@link Builder#build()} method, it should be
     * impossible for any null value to be passed into this constructor.
     * @param id ID for the record
     * @param entry entry for record
     * @param itemClass Class of item held in entry
     */
    @SuppressWarnings("checkstyle:ParameterName") // "id" is less than 3 chars, but is name used by model
    private StorageItemChangeRecord(String id, String entry, String itemClass) {
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

        StorageItemChangeRecord record = (StorageItemChangeRecord) thatObject;

        if (!id.equals(record.id)) {
            return false;
        }
        if (!entry.equals(record.entry)) {
            return false;
        }
        return itemClass.equals(record.itemClass);
    }

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
     * Gets an instance of a {@link Builder}.
     * @return A builder for Records
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Factory method to generate a record, given just an entry.
     * A random ID will be assigned.
     * @param entry Record entry
     * @return A record containing the entry, and with random ID.
     */
    @NonNull
    public static StorageItemChangeRecord forEntry(@NonNull String entry) {
        return StorageItemChangeRecord.builder().entry(entry).build();
    }

    /**
     * Utility for fluent construction of Record.
     */

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

        @SuppressWarnings("checkstyle:ParameterName")  // "id" < 3 chars; kept for naming consistency w/ model
        @NonNull
        public Builder id(@NonNull String id) {
            this.id = UUID.fromString(Objects.requireNonNull(id));
            return this;
        }

        /**
         * Configures the Builder to generate a random ID for the next Record.
         * This is the default behavior of the {@link Builder}.
         * @return Current instance of the Builder, for fluent configuration chaining
         */
        @NonNull
        public Builder randomId() {
            this.id = null;
            return this;
        }

        /**
         * Configures the entry for the next Record instance.
         * @param entry Value for the {@link StorageItemChangeRecord}'s entry
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
         * @return A new instance of a {@link StorageItemChangeRecord}.
         */
        @NonNull
        public StorageItemChangeRecord build() {
            final UUID usedId = id == null ? UUID.randomUUID() : id;
            randomId();
            return new StorageItemChangeRecord(
                Objects.requireNonNull(usedId).toString(),
                Objects.requireNonNull(entry),
                Objects.requireNonNull(itemClass)
            );
        }
    }
}
