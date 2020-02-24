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

import java.util.Objects;

/**
 * NOTE: this class is named in a way that makes it sounds broad, but it is NOT.
 * A {@link Mutation} is a depiction of a GraphQL mutation _as received on a subscription_.
 * Subscriptions notify the client of mutations that have occurred on the backend.
 * Other GraphQL mutation data may be represented in additional/other data structures.
 * @param <T> Type of data that has been mutated
 */
final class Mutation<T extends Model> {
    private final T model;
    private final Class<T> modelClass;
    private final Type type;

    private Mutation(T model, Class<T> modelClass, Type type) {
        this.model = model;
        this.modelClass = modelClass;
        this.type = type;
    }

    @NonNull
    T model() {
        return model;
    }

    @SuppressWarnings("unused")
    @NonNull
    Class<T> modelClass() {
        return modelClass;
    }

    @NonNull
    Type type() {
        return type;
    }

    @NonNull
    static <T extends Model> Builder<T> builder() {
        return new Builder<>();
    }

    static final class Builder<T extends Model> {
        private T model;
        private Class<T> modelClass;
        private Type type;

        @NonNull
        Builder<T> model(@NonNull T model) {
            this.model = Objects.requireNonNull(model);
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
            return new Mutation<>(model, modelClass, type);
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
