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

package com.amplifyframework.datastore;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.util.Immutable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

final class SimpleModelProvider implements ModelProvider {
    private String version;
    private Set<Class<? extends Model>> modelClasses;

    private SimpleModelProvider(@NonNull String version, @NonNull Set<Class<? extends Model>> modelClasses) {
        this.version = version;
        this.modelClasses = modelClasses;
    }

    private static SimpleModelProvider instance(@NonNull String version, @NonNull Set<Class<? extends Model>> classes) {
        return new SimpleModelProvider(version, classes);
    }

    @Override
    public Set<Class<? extends Model>> models() {
        return Immutable.of(modelClasses);
    }

    @Override
    public String version() {
        return version;
    }

    static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings({"UnusedReturnValue", "unused"})
    static final class Builder {
        private Set<Class<? extends Model>> modelClasses;
        private String version;

        Builder() {
            this.modelClasses = new HashSet<>();
        }

        <T extends Model> Builder addModel(@NonNull Class<T> modelClass) {
            Objects.requireNonNull(modelClass);
            Builder.this.modelClasses.add(modelClass);
            return Builder.this;
        }

        @SafeVarargs
        final Builder addModels(@NonNull Class<? extends Model>... modelClasses) {
            Objects.requireNonNull(modelClasses);
            for (Class<? extends Model> clazz : modelClasses) {
                Objects.requireNonNull(clazz);
                Builder.this.addModel(clazz);
            }
            return Builder.this;
        }

        Builder version(@NonNull String version) {
            Builder.this.version = Objects.requireNonNull(version);
            return Builder.this;
        }

        SimpleModelProvider build() {
            return SimpleModelProvider.instance(version, modelClasses);
        }
    }
}
