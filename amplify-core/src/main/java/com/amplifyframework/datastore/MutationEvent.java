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

import com.amplifyframework.core.model.Model;

import java.util.UUID;

/**
 * A MutationEvent is emitted whenever there is a mutation
 * to an object in the DataStore.
 * @param <T> The type of the object has been mutated
 */
public final class MutationEvent<T extends Model> implements Model {
    private final UUID mutationId;
    private final MutationType mutationType;
    private final T data;
    private final Class<T> dataClass;
    private final Source source;

    MutationEvent(Builder<T> builder) {
        this.mutationId = builder.mutationId();
        this.mutationType = builder.mutationType();
        this.data = builder.data();
        this.dataClass = builder.dataClass();
        this.source = builder.source();
    }

    /**
     * Gets the type of mutation; i.e., deletion, update, insertion.
     * @return Type of mutation
     */
    public MutationType mutationType() {
        return mutationType;
    }

    /**
     * Gets the data that mutated. In the case of an insertion,
     * this is reference to the newly inserted object. In the case of
     * an update, this is a reference to the item as it exists *after*
     * the update. In the case of a deletion, this is an in-memory snapshot
     * of an object that was just deleted, and no longer exists in the DataStore.
     * @return The data that mutated
     */
    public T data() {
        return data;
    }

    /**
     * Gets the class type of the data.
     * @return Class type of data
     */
    public Class<T> dataClass() {
        return dataClass;
    }

    /**
     * Gets the source of this mutation event.
     * @return The source which caused this mutation to occur.
     */
    public Source source() {
        return source;
    }

    @Override
    public String getId() {
        return mutationId.toString();
    }

    /**
     * Gets an instance of the Builder.
     * @param <T> class type of model that has mutated
     * @return Builder instance
     */
    public static <T extends Model> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * A utility to construct instances of MutationEvent.
     * @param <T> Class type of model that has mutated
     */
    public static final class Builder<T extends Model> {
        private final UUID mutationId;
        private MutationType mutationType;
        private T data;
        private Class<T> dataClass;
        private Source source;

        Builder() {
            mutationId = UUID.randomUUID();
        }

        /**
         * Configures the mutation type that will be set in the built MutationEvent.
         * @param mutationType Mutation type to set in MutationEvent
         * @return Current builder instance for fluent chaining
         */
        public Builder<T> mutationType(final MutationType mutationType) {
            this.mutationType = mutationType;
            return this;
        }

        /**
         * Configures the data that will be placed into the built MutationEvent.
         * @param data Data to put into MutationEvent
         * @return Current builder instance for fluent chaining
         */
        public Builder<T> data(final T data) {
            this.data = data;
            return this;
        }

        /**
         * Configures the class type of the data field that will be stored into
         * any new MutationEvent from this builder.
         * @param dataClass The class type of the data
         * @return Current builder instance for fluent chaining
         */
        public Builder<T> dataClass(final Class<T> dataClass) {
            this.dataClass = dataClass;
            return this;
        }

        /**
         * Configures the source of mutation that will be put into the built MutationEvent.
         * @param source Source of mutation
         * @return Current builder instance for fluent chaining
         */
        public Builder<T> source(final Source source) {
            this.source = source;
            return this;
        }

        /**
         * Builds an instance of a MutationEvent using the provided
         * configuration operations currently stored in the the builder.
         * @return A new MutationEvent instance
         */
        public MutationEvent<T> build() {
            return new MutationEvent<>(Builder.this);
        }

        UUID mutationId() {
            return mutationId;
        }

        MutationType mutationType() {
            return mutationType;
        }

        T data() {
            return data;
        }

        Class<T> dataClass() {
            return dataClass;
        }

        Source source() {
            return source;
        }
    }

    /**
     * A mutation is either an insertion, an update, or a deletion.
     */
    public enum MutationType {

        /**
         * An item is newly put into the DataStore. It is determined
         * as "new" based on its class type and its unique ID.
         */
        INSERT,

        /**
         * An existing item has been modified in some way, but it still
         * exists after that modification. The item
         * was determined to be already existing based on its class
         * type and its unique ID.
         */
        UPDATE,

        /**
         * An existing item has been removed from the DataStore.
         */
        DELETE
    }

    /**
     * The source of the event.
     */
    public enum Source {

        /**
         * This event was initiated by the DataStore API.
         */
        DATA_STORE,

        /**
         * This event was initiated by the Sync Engine.
         */
        SYNC_ENGINE
    }
}
