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
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;

import java.util.Objects;
import java.util.UUID;

/**
 * Models a change that is waiting to be uploaded to a remote system.
 * Where-as the {@link com.amplifyframework.datastore.storage.LocalStorageAdapter}
 * emits {@link com.amplifyframework.datastore.storage.StorageItemChange} when its items change,
 * the {@link MutationOutbox} stores {@link PendingMutation} on a queue, and applies de-duplication
 * logic onto them, before ultimately uploading to a remote endpoint.
 * @param <T> Type of model that has experienced a mutation
 */
public final class PendingMutation<T extends Model> implements Comparable<PendingMutation<? extends Model>> {
    private final T mutatedItem;
    private final Class<T> classOfMutatedItem;
    private final Type mutationType;
    private final TimeBasedUuid mutationId;

    private PendingMutation(TimeBasedUuid mutationId, T mutatedItem, Class<T> classOfMutatedItem, Type mutationType) {
        this.mutationId = mutationId;
        this.mutatedItem = mutatedItem;
        this.classOfMutatedItem = classOfMutatedItem;
        this.mutationType = mutationType;
    }

    /**
     * Creates a {@link PendingMutation}, using a provided mutation ID.
     * @param mutationId A globally-unique, *time-based* ID for this mutation event;
     *                   the ID is used for temporal ordering of mutations
     * @param mutatedItem The item that undergone a mutation
     * @param classOfMutatedItem The class of the item that has undergone mutation
     * @param mutationType Type of mutation
     * @param <T> The type of the item that has undergone mutation
     * @return A {@link PendingMutation}
     */
    static <T extends Model> PendingMutation<T> instance(
            @NonNull TimeBasedUuid mutationId,
            @NonNull T mutatedItem,
            @NonNull Class<T> classOfMutatedItem,
            @NonNull Type mutationType) {
        return new PendingMutation<>(
            Objects.requireNonNull(mutationId),
            Objects.requireNonNull(mutatedItem),
            Objects.requireNonNull(classOfMutatedItem),
            Objects.requireNonNull(mutationType)
        );
    }

    /**
     * Creates a {@link PendingMutation}, using a newly generated mutation ID.
     * @param mutatedItem The item that undergone a mutation
     * @param classOfMutatedItem The class of the item that has undergone mutation
     * @param mutationType Type of mutation
     * @param <T> The type of the item that has undergone mutation
     * @return A {@link PendingMutation}
     */
    static <T extends Model> PendingMutation<T> instance(
            @NonNull T mutatedItem, @NonNull Class<T> classOfMutatedItem, @NonNull Type mutationType) {
        return instance(TimeBasedUuid.create(), mutatedItem, classOfMutatedItem, mutationType);
    }

    /**
     * Creates a {@link PendingMutation} that represents the creation of a model.
     * @param createdItem The model that was created
     * @param classOfCreatedItem The class of the created model
     * @param <T> The type of created model
     * @return A PendingMutation representing the model creation
     */
    static <T extends Model> PendingMutation<T> creation(@NonNull T createdItem, @NonNull Class<T> classOfCreatedItem) {
        return instance(createdItem, classOfCreatedItem, Type.CREATE);
    }

    /**
     * Creates a {@link PendingMutation} that represents an update to a model.
     * @param updatedItem The model that was updated
     * @param classOfUpdatedItem The class of the updated model
     * @param <T> The type of updated model
     * @return A PendingMutation representing the model update
     */
    static <T extends Model> PendingMutation<T> update(@NonNull T updatedItem, @NonNull Class<T> classOfUpdatedItem) {
        return instance(updatedItem, classOfUpdatedItem, Type.UPDATE);
    }

    /**
     * Creates a {@link PendingMutation} that represents the deletion of a model.
     * @param deletedItem The model that was deleted
     * @param classOfDeletedItem The class of the deleted model
     * @param <T> The type of model that was deleted
     * @return A PendingMutation representing the model deletion
     */
    static <T extends Model> PendingMutation<T> deletion(@NonNull T deletedItem, @NonNull Class<T> classOfDeletedItem) {
        return instance(deletedItem, classOfDeletedItem, Type.DELETE);
    }

    /**
     * Gets the ID of this mutation.
     * @return Mutation ID
     */
    @NonNull
    TimeBasedUuid getMutationId() {
        return mutationId;
    }

    /**
     * Gets the item that has been mutated.
     * @return The mutated item
     */
    @NonNull
    T getMutatedItem() {
        return mutatedItem;
    }

    /**
     * Gets the class of the item that been mutated.
     * @return Class of the item that has been mutated
     */
    @NonNull
    Class<T> getClassOfMutatedItem() {
        return classOfMutatedItem;
    }

    /**
     * Gets the type of mutation this is.
     * @return Type of mutation
     */
    @NonNull
    Type getMutationType() {
        return mutationType;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        PendingMutation<?> that = (PendingMutation<?>) thatObject;

        return ObjectsCompat.equals(mutationId, that.mutationId) &&
            ObjectsCompat.equals(mutatedItem, that.mutatedItem) &&
            ObjectsCompat.equals(classOfMutatedItem, that.classOfMutatedItem) &&
            ObjectsCompat.equals(mutationType, that.mutationType);
    }

    @Override
    public int hashCode() {
        int result = mutationId.hashCode();
        result = 31 * result + mutatedItem.hashCode();
        result = 31 * result + classOfMutatedItem.hashCode();
        result = 31 * result + mutationType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PendingMutation{" +
            "mutatedItem=" + mutatedItem +
            ", classOfMutatedItem=" + classOfMutatedItem +
            ", mutationType=" + mutationType +
            ", mutationId=" + mutationId +
            '}';
    }

    /**
     * Mutations may be ordered according to their {@link TimeBasedUuid} field.
     * {@link TimeBasedUuid} is itself {@link Comparable}.
     * @param another Some other PendingMutation.
     * @return -1, 0, 1 if this mutation is smaller, same, or bigger than another
     */
    @Override
    public int compareTo(@NonNull PendingMutation<? extends Model> another) {
        Objects.requireNonNull(another);
        return this.mutationId.compareTo(another.getMutationId());
    }

    /**
     * A persistent record of a PendingMutation.
     *
     * This type is persisted to a {@link LocalStorageAdapter}.
     *
     * Since a PendingMutation contains a reference to an actual model, and we don't want to refer
     * to that model via a foreign key, we first convert the {@link PendingMutation} to a
     * {@link PendingMutation.PersistentRecord}.
     *
     * A PersistentRecord is something the storage adapter can handle easily, since the
     * referenced model is just a Gson-serialized String version of itself -- handled external
     * to the data modeling system.
     */
    @SuppressWarnings("unused")
    @ModelConfig(pluralName = "PersistentRecords")
    @Index(fields = "decodedModelClassName", name = "decodedModelClassNameBasedIndex")
    public static final class PersistentRecord implements Model, Comparable<PersistentRecord> {
        @ModelField(targetType = "ID", isRequired = true)
        private final String id;

        @ModelField(targetType = "String", isRequired = true)
        private final String decodedModelId;

        @ModelField(targetType = "String", isRequired = true)
        private final String encodedModelData;

        @ModelField(targetType = "String", isRequired = true)
        private final String decodedModelClassName;

        /**
         * Constructs a Record.
         * Note: by arrangement of the {@link PersistentRecord.Builder#build()} method, it should be
         * impossible for any null value to be passed into this constructor.
         * @param id ID for the mutation record
         * @param decodedModelId The id of the model that is encoded into this record
         * @param encodedModelData entry for record
         * @param decodedModelClassName Class of item held in entry
         */
        @SuppressWarnings("checkstyle:ParameterName") // "id" is less than 3 chars, but is name used by model
        private PersistentRecord(
                String id,
                String decodedModelId,
                String encodedModelData,
                String decodedModelClassName) {
            this.id = id;
            this.decodedModelId = decodedModelId;
            this.encodedModelData = encodedModelData;
            this.decodedModelClassName = decodedModelClassName;
        }

        /**
         * Gets the ID of the record. This ID does *NOT* match the encoded item.
         * Instead, this is a unique time-based UUID for the mutation itself.
         * For the ID of the model (in its decoded form), use {@link #getDecodedModelId()}.
         * @return The record id
         */
        @NonNull
        @Override
        public String getId() {
            return this.id;
        }

        /**
         * Gets the ID of the model (in the model's decoded form).
         * This is not the same as {@link #getId()}, which is an ID for the mutation itself.
         * The mutation ID is a v1 UUID which can be compared by timestamp. This current ID
         * is a v4 ID which is a better choice choice for UUID since it does not rely on flaky
         * notions of time.
         * @return The ID of the model, that would be returned if that model were
         *         decoded and {@link Model#getId()} were called.
         */
        @NonNull
        String getDecodedModelId() {
            return this.decodedModelId;
        }

        /**
         * Gets the encoded form of the model. The model is encoded into a JSON data structure,
         * stored as String.
         * @return The model data, in its encoded string form
         */
        @NonNull
        String getEncodedModelData() {
            return this.encodedModelData;
        }

        /**
         * Gets the class of the model, in its decoded form.
         * The encoded data is always of type String, but the decoded class
         * is some model type.
         * @return Class of encoded model
         */
        @NonNull
        String getDecodedModelClassName() {
            return decodedModelClassName;
        }

        @Override
        public boolean equals(@Nullable Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            PersistentRecord record = (PersistentRecord) thatObject;

            return ObjectsCompat.equals(id, record.id) &&
                ObjectsCompat.equals(decodedModelId, record.decodedModelId) &&
                ObjectsCompat.equals(encodedModelData, record.encodedModelData) &&
                ObjectsCompat.equals(decodedModelClassName, record.decodedModelClassName);
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + decodedModelId.hashCode();
            result = 31 * result + encodedModelData.hashCode();
            result = 31 * result + decodedModelClassName.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Record{" +
                "id='" + id + '\'' +
                ", decodedModelId='" + decodedModelId + '\'' +
                ", encodedModelData='" + encodedModelData + '\'' +
                ", decodedModelClassName='" + decodedModelClassName + '\'' +
                '}';
        }

        /**
         * Gets an instance of a {@link PersistentRecord.Builder}.
         * @param <T> Type of model that has experienced mutation
         * @return A builder for {@link PersistentRecord}
         */
        @NonNull
        public static <T extends Model> PersistentRecord.Builder<T> builder() {
            return new PersistentRecord.Builder<>();
        }

        /**
         * Determines an ordering for this record with respect to another.
         * This is achieved by considering the record ID as a v1 UUID,
         * and comparing those. (See {@link TimeBasedUuid#compareTo(TimeBasedUuid)},
         * to which this delegates.
         * @param another Some other pending mutation
         * @return -1, 0, 1 if this current mutation is smaller, same, or bigger than another
         */
        @Override
        public int compareTo(PersistentRecord another) {
            return TimeBasedUuid.fromString(getId())
                .compareTo(TimeBasedUuid.fromString(another.getId()));
        }

        /**
         * Utility for construction of {@link PersistentRecord} through chained configurator calls.
         * @param <T> Type of model for which a pending mutation is being built
         */
        static final class Builder<T extends Model> {
            private TimeBasedUuid recordId;
            private UUID decodedModelId;
            private String encodedModelData;
            private String decodedModelClassName;

            /**
             * Configures the Record ID to a specified TimeBasedUUID value.
             * @param timeBasedUuid A time based UUID for the record.
             * @return Current builder, for fluent configuration chaining
             */
            @NonNull
            PersistentRecord.Builder<T> recordId(@NonNull TimeBasedUuid timeBasedUuid) {
                Objects.requireNonNull(timeBasedUuid);
                this.recordId = timeBasedUuid;
                return this;
            }

            /**
             * Configures the ID that is associated with the decoded form of the model
             * that is encoded into this record. For example, if this record encodes
             * a Blog object, then this ID value is the same as the value returned by calling
             * getId() on that object.
             * @param decodedModelId The ID of the model in it decoded state
             * @return The ID of the model, when that model is in its decoded form
             */
            @NonNull
            PersistentRecord.Builder<T> decodedModelId(@NonNull String decodedModelId) {
                Objects.requireNonNull(decodedModelId);
                // This is stored this way for the purpose of validating hte input.
                this.decodedModelId = UUID.fromString(decodedModelId);
                return this;
            }

            /**
             * Configures the encoded data associated with an item that has undergone a mutation.
             * This value will be used in the next-build {@link PersistentRecord}.
             * @param encodedModelData Value for the {@link PersistentRecord}'s encoded model data
             * @return Current Builder instance, for fluent configuration chaining
             */
            @NonNull
            PersistentRecord.Builder<T> encodedModelData(@NonNull String encodedModelData) {
                this.encodedModelData = Objects.requireNonNull(encodedModelData);
                return this;
            }

            /**
             * Configures the model class of item that has undergone a mutation. This is the model's class
             * in its decoded form, before being stored into this structure. The type of the class
             * in encoded form is String.class.
             * @param decodedModelClassName The name of the model class to which the encoded model data
             *                              may be decoded
             * @return Current builder for fluent method chaining
             */
            @NonNull
            PersistentRecord.Builder<T> decodedModelClassName(@NonNull String decodedModelClassName) {
                this.decodedModelClassName = Objects.requireNonNull(decodedModelClassName);
                return this;
            }

            /**
             * Builds a {@link PersistentRecord} using the configured properties.
             * @return A new instance of a {@link PersistentRecord}.
             */
            @NonNull
            PendingMutation.PersistentRecord build() {
                return new PendingMutation.PersistentRecord(
                    Objects.requireNonNull(recordId).toString(),
                    Objects.requireNonNull(decodedModelId).toString(),
                    Objects.requireNonNull(encodedModelData),
                    Objects.requireNonNull(decodedModelClassName)
                );
            }
        }
    }

    /**
     * The type of mutation this is.
     */
    enum Type {
        /**
         * A model-creation mutation.
         */
        CREATE,

        /**
         * Any change to an already-existing model, that does not *delete* the model.
         */
        UPDATE,

        /**
         * The removal of a previously-created model.
         */
        DELETE
    }

    /**
     * Converts {@link PendingMutation} to {@link PendingMutation.PersistentRecord} and vice-versa.
     */
    interface Converter {
        /**
         * Converts a {@link PendingMutation} into a {@link PendingMutation.PersistentRecord}.
         * @param pendingMutation Mutation to convert to record
         * @param <T> Type of object that has undergone mutation
         * @return A record representation of the mutation
         */
        <T extends Model> PersistentRecord toRecord(PendingMutation<T> pendingMutation);

        /**
         * Converts a {@link PendingMutation.PersistentRecord} to an {@link PendingMutation}.
         * @param record Record to be converted to PersistentMutation
         * @param <T> Type of data that has undergone mutation
         * @return A {@link PendingMutation} corresponding to provided record
         * @throws DataStoreException If conversion fails
         */
        <T extends Model> PendingMutation<T> fromRecord(PersistentRecord record) throws DataStoreException;
    }
}
