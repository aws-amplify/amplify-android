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

package com.amplifyframework.datastore.syncengine;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;

import java.util.Objects;

/**
 * NOTE: this class is named in a way that makes it sounds broad, but it is NOT.
 * A {@link Mutation} is a depiction of a GraphQL mutation _as received on a subscription_.
 * Subscriptions notify the client of mutations that have occurred on the backend.
 * Other GraphQL mutation data may be represented in additional/other data structures.
 *
 * TODO0: is this even needed? Can we just use {@link ModelWithMetadata} directly?
 * TODO1: is this well named? Can it be called a SubscriptionEvent, instead?
 *
 * @param <T> Type of data that has been mutated
 */
final class Mutation<T extends Model> {
    private final ModelWithMetadata<T> modelWithMetadata;
    private final Class<T> modelClass;
    private final Type type;

    private Mutation(ModelWithMetadata<T> modelWithMetadata, Class<T> modelClass, Type type) {
        this.modelWithMetadata = modelWithMetadata;
        this.modelClass = modelClass;
        this.type = type;
    }

    @NonNull
    ModelWithMetadata<T> modelWithMetadata() {
        return modelWithMetadata;
    }

    @SuppressWarnings("unused")
    @NonNull
    Class<T> modelClass() {
        return modelClass;
    }

    @SuppressWarnings("unused")
    @NonNull
    Type type() {
        return type;
    }

    @NonNull
    static <T extends Model> Builder<T> builder() {
        return new Builder<>();
    }

    static final class Builder<T extends Model> {
        private ModelWithMetadata<T> modelWithMetadata;
        private Class<T> modelClass;
        private Type type;

        @NonNull
        Builder<T> modelWithMetadata(@NonNull ModelWithMetadata<T> modelWithMetadata) {
            this.modelWithMetadata = Objects.requireNonNull(modelWithMetadata);
            return this;
        }

        @NonNull
        Builder<T> modelClass(@NonNull Class<T> modelClass) {
            this.modelClass = Objects.requireNonNull(modelClass);
            return this;
        }

        @NonNull
        Builder<T> type(@NonNull Type type) {
            this.type = Objects.requireNonNull(type);
            return this;
        }

        @SuppressLint("SyntheticAccessor")
        @NonNull
        Mutation<T> build() {
            return new Mutation<>(modelWithMetadata, modelClass, type);
        }
    }

    /**
     * An enumeration of the different type of mutations that may be observed.
     */
    public enum Type {
        /**
         * An item is created.
         */
        CREATE,

        /**
         * An item is updated.
         */
        UPDATE,

        /**
         * An item is deleted.
         */
        DELETE
    }
}
