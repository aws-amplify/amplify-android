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
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.util.Immutable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A ModelProvider that works in the way we expect of hybrid platform's implementations.
 *
 * The SchemaProvider provides an empty set of {@link Model}s via
 * {@link ModelProvider#models()}. Instead, it returns a non-empty set of
 * {@link ModelSchema} via {@link ModelProvider#modelSchemas()}.
 */
final class SchemaProvider implements ModelProvider {
    private final String version;
    private final Map<String, ModelSchema> schemas;

    private SchemaProvider(String version, Map<String, ModelSchema> schemas) {
        this.version = version;
        this.schemas = schemas;
    }

    /**
     * Creates a SchemaProvider which will provide the given schemas.
     * @param version A version for the collection of model schema
     * @param schemas A variable-argument list of schema that the created provider should vend
     * @return A SchemaProvider
     */
    @NonNull
    static SchemaProvider of(@NonNull String version, @NonNull ModelSchema... schemas) {
        Objects.requireNonNull(version);
        Objects.requireNonNull(schemas);
        Map<String, ModelSchema> map = new HashMap<>();
        for (ModelSchema schema : schemas) {
            map.put(schema.getName(), schema);
        }
        return new SchemaProvider(version, Immutable.of(map));
    }

    /**
     * Creates a SchemaProvider which will provide the given schemas.
     * Assigns a random version string for the created provider.
     * @param schemas A variable-argument list of schema that the created provider should vend
     * @return A SchemaProvider
     */
    @NonNull
    static SchemaProvider of(@NonNull ModelSchema... schemas) {
        Objects.requireNonNull(schemas);
        return of(randomVersion(), schemas);
    }

    /**
     * Creates a SchemaProvider by extracting {@link ModelSchema} from the provided
     * {@link ModelProvider}.
     * @param modelOnlyProvider A provider which provides only models
     * @return A provider which provides only schema
     */
    @NonNull
    static SchemaProvider from(@NonNull ModelProvider modelOnlyProvider) throws AmplifyException {
        Objects.requireNonNull(modelOnlyProvider);
        Map<String, ModelSchema> schemas = new HashMap<>();
        for (Class<? extends Model> clazz : modelOnlyProvider.models()) {
            ModelSchema schema = ModelSchema.fromModelClass(clazz);
            schemas.put(schema.getName(), schema);
        }
        return new SchemaProvider(randomVersion(), schemas);
    }

    @Override
    public Set<Class<? extends Model>> models() {
        return Collections.emptySet();
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public Map<String, ModelSchema> modelSchemas() {
        return schemas;
    }

    @Override
    public Set<String> modelNames() {
        return schemas.keySet();
    }

    private static String randomVersion() {
        return UUID.randomUUID().toString();
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }
        SchemaProvider that = (SchemaProvider) thatObject;
        return this.version.equals(that.version) && this.schemas.equals(that.schemas);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(version, schemas);
    }

    @NonNull
    @Override
    public String toString() {
        return "SchemaProvider{" +
            "version='" + version + '\'' +
            ", schemas=" + schemas +
            '}';
    }
}
