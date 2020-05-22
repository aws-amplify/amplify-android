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
    @NonNull
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
    @NonNull
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
    @NonNull
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
    @NonNull
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
    @NonNull
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
    public T getMutatedItem() {
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
    public boolean equals(@Nullable Object thatObject) {
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

    @NonNull
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
     * Used for storing PendingMutations into a {@link LocalStorageAdapter}.
     *
     * Since a PendingMutation contains a reference to an ACTUAL, non-system model,
     * and we don't want to refer to that model via a foreign key, we first convert
     * the {@link PendingMutation} to String, and stuff it into an
     * {@link PendingMutation.PersistentRecord}.
     *
     * The mutation ID, model ID and model class are stored alongside the serialized mutation,
     * to facilitate record search/retrieval.
     */
    @ModelConfig(pluralName = "PersistentRecords")
    @Index(fields = "containedModelClassName", name = "containedModelClassNameBasedIndex")
    public static final class PersistentRecord implements Model, Comparable<PersistentRecord> {
        @ModelField(targetType = "ID", isRequired = true)
        private final String id;

        @ModelField(targetType = "String", isRequired = true)
        private final String containedModelId;

        @ModelField(targetType = "String", isRequired = true)
        private final String serializedMutationData;

        @ModelField(targetType = "String", isRequired = true)
        private final String containedModelClassName;

        /**
         * Constructs a Record.
         * Note: by arrangement of the {@link PersistentRecord.Builder#build()} method, it should be
         * impossible for any null value to be passed into this constructor.
         * @param id ID for the mutation record
         * @param containedModelId The id of the model that is contained inside of the serialized mutation data
         * @param serializedMutationData A serialized form of a PendingMutation, itself containing model data.
         * @param containedModelClassName Class name of the model inside the serialized mutation data
         */
        @SuppressWarnings("checkstyle:ParameterName") // "id" is less than 3 chars, but is name used by model
        private PersistentRecord(
                String id,
                String containedModelId,
                String serializedMutationData,
                String containedModelClassName) {
            this.id = id;
            this.containedModelId = containedModelId;
            this.serializedMutationData = serializedMutationData;
            this.containedModelClassName = containedModelClassName;
        }

        /**
         * Gets the ID of the record. This ID is *NOT* the same as the ID of the model contained in the
         * mutation. Instead, this is a unique time-based UUID for the mutation itself.
         * For the ID of the model (in its original, non-encapsulated form), use {@link #getContainedModelId()}.
         * @return The ID of the persistent record, *NOT* the ID of the model contained within it.
         */
        @NonNull
        @Override
        public String getId() {
            return this.id;
        }

        /**
         * Gets the ID of the Model instance that is contained within the mutation data.
         * This is not the same as {@link #getId()}, which is an ID for the mutation itself.
         * The mutation ID is a v1 UUID which can be compared by timestamp. This current ID
         * is a v4 ID which is a better choice choice for UUID since it does not rely on flaky
         * notions of time.
         * @return The ID of the model, that would be returned if that model were
         *         extracted from the record, and {@link Model#getId()} were called. on it.
         */
        @SuppressWarnings("unused")
        @NonNull
        String getContainedModelId() {
            return this.containedModelId;
        }

        /**
         * Gets a JSON-serialized representation of a {@link PendingMutation}, as String.
         * @return The PendingMutation, in a JSON-serialized form, as String
         */
        @NonNull
        String getSerializedMutationData() {
            return this.serializedMutationData;
        }

        /**
         * Gets the class of the model that is contained within the mutation data.
         * The mutation data is always of type String. The decoded data is a PendingMutation of T.
         * The "contained model class name" is the class name for T.
         * @return Class of the Model contained within the mutation data
         */
        @NonNull
        String getContainedModelClassName() {
            return containedModelClassName;
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
                ObjectsCompat.equals(containedModelId, record.containedModelId) &&
                ObjectsCompat.equals(serializedMutationData, record.serializedMutationData) &&
                ObjectsCompat.equals(containedModelClassName, record.containedModelClassName);
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + containedModelId.hashCode();
            result = 31 * result + serializedMutationData.hashCode();
            result = 31 * result + containedModelClassName.hashCode();
            return result;
        }

        @NonNull
        @Override
        public String toString() {
            return "Record{" +
                "id='" + id + '\'' +
                ", containedModelId='" + containedModelId + '\'' +
                ", serializedMutationData='" + serializedMutationData + '\'' +
                ", containedModelClassName='" + containedModelClassName + '\'' +
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
        public int compareTo(@NonNull PersistentRecord another) {
            return TimeBasedUuid.fromString(getId())
                .compareTo(TimeBasedUuid.fromString(another.getId()));
        }

        /**
         * Utility for construction of {@link PersistentRecord} through chained configurator calls.
         * @param <T> Type of model for which a pending mutation is being built
         */
        static final class Builder<T extends Model> {
            private TimeBasedUuid mutationId;
            private UUID containedModelId;
            private String serializedMutationData;
            private String containedModelClassName;

            /**
             * Configures the ID for the record itself, which should match the ID of pending
             * mutation stored in the serialized data.
             * @param mutationId A time based UUID for the record.
             * @return Current builder, for fluent configuration chaining
             */
            @NonNull
            PersistentRecord.Builder<T> mutationId(@NonNull TimeBasedUuid mutationId) {
                Objects.requireNonNull(mutationId);
                this.mutationId = mutationId;
                return this;
            }

            /**
             * Configures the ID that is associated with the model buried inside of this
             * mutation record. For example, if this record contains serialzied data for a mutation of a
             * Blog object, then this ID value is the same as that returned by calling
             * `getId()` on that Model object.
             * @param containedModelId The ID of the model nested inside the serialized mutation
             * @return Current builder, for fluent configuration chaining
             */
            @NonNull
            PersistentRecord.Builder<T> containedModelId(@NonNull String containedModelId) {
                Objects.requireNonNull(containedModelId);
                // This is stored this way for the purpose of validating hte input.
                this.containedModelId = UUID.fromString(containedModelId);
                return this;
            }

            /**
             * Configures the serialized data that represents a pending mutation.
             * This value will be used in the next-build {@link PersistentRecord}.
             * @param serializedMutationData Value for the {@link PersistentRecord}'s serialized mutation
             * @return Current Builder instance, for fluent configuration chaining
             */
            @NonNull
            PersistentRecord.Builder<T> serializedMutationData(@NonNull String serializedMutationData) {
                this.serializedMutationData = Objects.requireNonNull(serializedMutationData);
                return this;
            }

            /**
             * Configures the model class of item that has undergone a mutation.
             * The serialized mutation data is a JSON string, and it represents a PendingMutation.
             * Inside of that (logical) mutation is data about a model, and that model has _this_
             * "contained model class name" as its type.
             * @param containedModelClassName The class name of the model buried inside the serialized mutation
             * @return Current builder for fluent method chaining
             */
            @NonNull
            PersistentRecord.Builder<T> containedModelClassName(@NonNull String containedModelClassName) {
                this.containedModelClassName = Objects.requireNonNull(containedModelClassName);
                return this;
            }

            /**
             * Builds a {@link PersistentRecord} using the configured properties.
             * @return A new instance of a {@link PersistentRecord}.
             */
            @NonNull
            PendingMutation.PersistentRecord build() {
                return new PendingMutation.PersistentRecord(
                    Objects.requireNonNull(mutationId).toString(),
                    Objects.requireNonNull(containedModelId).toString(),
                    Objects.requireNonNull(serializedMutationData),
                    Objects.requireNonNull(containedModelClassName)
                );
            }
        }
    }

    /**
     * The type of mutation.
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
         * @param <T> Type of Model that has undergone mutation - the reason for having a PendingMutation, to begin
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
