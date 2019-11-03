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

import com.amplifyframework.datastore.model.Model;

import java.util.Objects;

/**
 * A MutationEvent is emitted whenever there is a mutation
 * to an object in the DataStore.
 * @param <T> The type of the object has been mutated
 */
public final class MutationEvent<T extends Model> {
    private final MutationType mutationType;
    private final T data;

    private MutationEvent(MutationType mutationType, T data) {
        this.mutationType = mutationType;
        this.data = data;
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
     * Static factory method to aid in creation a deletion-type mutation.
     * @param data The data that was deleted from the DataStore.
     * @param <T> The type of the data that was deleted
     * @return A MutationEvent representing the deletion.
     */
    public static <T extends Model> MutationEvent<T> deleted(@NonNull T data) {
        return new MutationEvent<>(MutationType.DELETE, Objects.requireNonNull(data));
    }

    /**
     * Static factory method to aid in creation of an update-type mutation.
     * @param data The data that was updated
     * @param <T> The type of the updated data
     * @return A MutationEvent representing the update
     */
    public static <T extends Model> MutationEvent<T> updated(@NonNull T data) {
        return new MutationEvent<>(MutationType.UPDATE, Objects.requireNonNull(data));
    }

    /**
     * Static factory method to aid in the creation of an insert-type mutation.
     * @param data The data that was inserted to the DataStore
     * @param <T> The type of the {@see data} that was inserted
     * @return A MutationEvent modeling the mutation
     */
    public static <T extends Model> MutationEvent<T> inserted(@NonNull T data) {
        return new MutationEvent<>(MutationType.INSERT, Objects.requireNonNull(data));
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
}
