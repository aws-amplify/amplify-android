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

package com.amplifyframework.datastore.model;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A simple immutable implementation of the {@link ModelProvider} contract, which
 * will provide the model classes you pass to it, at the version you pass to it.
 */
public final class SimpleModelProvider implements ModelProvider {
    private final String version;
    private final LinkedHashSet<Class<? extends Model>> modelClasses = new LinkedHashSet<>();
    private final Map<String, ModelSchema> modelSchemaMap = new HashMap<>();

    private SimpleModelProvider(String version, LinkedHashSet<Class<? extends Model>> modelClasses) {
        this.version = version;
        this.modelClasses.addAll(modelClasses);
    }

    private SimpleModelProvider(String version, Map<String, ModelSchema> modelSchemaMap) {
        this.version = version;
        this.modelSchemaMap.putAll(modelSchemaMap);
    }

    /**
     * Creates a simple model provider. The provider will return the given
     * version and model classes.
     * @param version Version for the new model provider to return
     * @param classes Model classes for the new model provider to return
     * @return A simple model provider, proving the given model classes, at the given version
     */
    @NonNull
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static SimpleModelProvider instance(@NonNull String version, @NonNull Class<? extends Model>... classes) {
        Objects.requireNonNull(version);
        Objects.requireNonNull(classes);
        LinkedHashSet<Class<? extends Model>> modelClasses = new LinkedHashSet<>();
        Collections.addAll(modelClasses, classes);
        return new SimpleModelProvider(version, modelClasses);
    }

    /**
     * Creates a simple model provider. The provider will return the given version
     * and model classes.
     * @param version Version of the new model provider to return
     * @param classes The set of model classes that the provider will provide
     * @return A simple model provider, providing the given versions and model classes
     */
    @NonNull
    public static SimpleModelProvider instance(@NonNull String version, @NonNull Set<Class<? extends Model>> classes) {
        Objects.requireNonNull(version);
        Objects.requireNonNull(classes);
        return new SimpleModelProvider(version, new LinkedHashSet<>(classes));
    }

    /**
     * Creates a simple model provider with model schema. The provider will return the given version
     * and model classes.
     * @param version Version of the new model provider to return
     * @param modelSchemaMap The map of model name to schema that the provider will provide
     * @return A simple model provider, providing the given versions and model classes
     */
    @NonNull
    public static SimpleModelProvider instance(
            @NonNull String version,
            @NonNull Map<String, ModelSchema> modelSchemaMap) {
        Objects.requireNonNull(version);
        Objects.requireNonNull(modelSchemaMap);
        return new SimpleModelProvider(version, modelSchemaMap);
    }

    /**
     * Creates a {@link SimpleModelProvider} which will provide the given model classes.
     * A random version will be used for the provider.
     * @param modelClasses Classes that the provider will provide
     * @return A SimpleModelProvider that provides the given classes at a new, random version
     */
    @NonNull
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static SimpleModelProvider withRandomVersion(@NonNull final Class<? extends Model>... modelClasses) {
        Objects.requireNonNull(modelClasses);
        return SimpleModelProvider.builder()
            .version(UUID.randomUUID().toString())
            .addModels(modelClasses)
            .build();
    }

    @NonNull
    @Override
    public Set<Class<? extends Model>> models() {
        return Collections.unmodifiableSet(modelClasses);
    }

    @NonNull
    @Override
    public String version() {
        return version;
    }

    @Override
    public Map<String, ModelSchema> modelSchemas() {
        return modelSchemaMap.size() > 0 ? modelSchemaMap : ModelProvider.super.modelSchemas();
    }

    @Override
    public Set<String> modelNames() {
        return modelSchemaMap.size() > 0 ? modelSchemaMap.keySet() : ModelProvider.super.modelNames();
    }

    /**
     * Begin building a new SimpleModelProvider.
     * @return A SimpleModelProvider builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        SimpleModelProvider that = (SimpleModelProvider) thatObject;

        if (!version.equals(that.version)) {
            return false;
        }
        return this.modelClasses.equals(that.modelClasses);
    }

    @Override
    public int hashCode() {
        int result = version.hashCode();
        result = 31 * result + modelClasses.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SimpleModelProvider{" +
            "version='" + version + '\'' +
            ", modelClasses=" + modelClasses +
            '}';
    }

    /**
     * Configures and builds instances of SimpleModelProvider.
     */
    @SuppressWarnings("WeakerAccess")
    public static final class Builder {
        private final LinkedHashSet<Class<? extends Model>> modelClasses;
        private String version;

        Builder() {
            this.modelClasses = new LinkedHashSet<>();
        }

        /**
         * Adds a single model to the set of model classes that the newly-build
         * {@link SimpleModelProvider} will provide.
         * @param modelClass A model class that will be provided in newly-built provider
         * @param <T> Type of model that will be provided
         * @return Current builder instance, with which to make chained builder configuration calls
         */
        @SuppressWarnings("UnusedReturnValue")
        public <T extends Model> Builder addModel(@NonNull Class<T> modelClass) {
            Objects.requireNonNull(modelClass);
            Builder.this.modelClasses.add(modelClass);
            return Builder.this;
        }

        /**
         * Adds a variable number of model classes to the set of model classes that will
         * be provided by the newly-build {@link SimpleModelProvider}.
         * @param modelClasses These classes will be provided by the newly-build {@link SimpleModelProvider}
         * @return Current builder instance, with which to make chained builder configuration calls
         */
        @SafeVarargs
        public final Builder addModels(@NonNull Class<? extends Model>... modelClasses) {
            Objects.requireNonNull(modelClasses);
            for (Class<? extends Model> clazz : modelClasses) {
                Objects.requireNonNull(clazz);
                Builder.this.addModel(clazz);
            }
            return Builder.this;
        }

        /**
         * Configures the version that will be used in the newly-built {@link SimpleModelProvider}.
         * @param version Version for model provider
         * @return Current builder instance, with which to make chained builder configuration calls
         */
        public Builder version(@NonNull String version) {
            Builder.this.version = Objects.requireNonNull(version);
            return Builder.this;
        }

        /**
         * Builds a new {@link SimpleModelProvider}.
         * @return A {@link SimpleModelProvider}
         */
        @SuppressLint("SyntheticAccessor")
        public SimpleModelProvider build() {
            return SimpleModelProvider.instance(version, modelClasses);
        }
    }
}
