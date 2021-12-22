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

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.CustomTypeSchema;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A ModelProvider which is created by composing together some other model providers.
 *
 * This is useful in the AWS DataStore implementation, as we have a user-provided
 * ModelProvider (from the CLI tool), and an internal System models provider.
 * When we initialize the DataStore, we don't distinguish between the two, we just need
 * to prepare storage to accommodate all the models. At this time, we want an
 * {@link CompoundModelProvider} to aggregate things.
 *
 * Buildings a stable {@link CompoundModelProvider#version()} is a key achievement of
 * this class. For input versions A, B, C, we want to repeatably and reliably produce
 * the same stable output value D, by means of some hashing function.
 */
public final class CompoundModelProvider implements ModelProvider {
    private final SimpleModelProvider delegateProvider;

    private CompoundModelProvider(SimpleModelProvider delegateProvider) {
        this.delegateProvider = delegateProvider;
    }

    /**
     * Gets an {@link CompoundModelProvider} that provides all of the same models as the
     * constituent {@link ModelProvider} that are provided. The version of the returned
     * {@link CompoundModelProvider} shall be stable UUID hash of the versions of all
     * provided {@link ModelProvider}s.
     * @param modelProviders model providers
     * @return A compound provider, which provides the models of all of the input providers
     */
    @NonNull
    public static CompoundModelProvider of(@NonNull ModelProvider... modelProviders) {
        final Map<String, ModelSchema> modelSchemaMap = new HashMap<>();
        final Map<String, CustomTypeSchema> customTypeSchemaMap = new HashMap<>();
        StringBuilder componentVersionBuffer = new StringBuilder();
        for (ModelProvider componentProvider : modelProviders) {
            componentVersionBuffer.append(componentProvider.version());
            modelSchemaMap.putAll(componentProvider.modelSchemas());
            customTypeSchemaMap.putAll(componentProvider.customTypeSchemas());
        }
        String version = UUID.nameUUIDFromBytes(componentVersionBuffer.toString().getBytes()).toString();
        SimpleModelProvider delegateProvider =
                SimpleModelProvider.instance(version, modelSchemaMap, customTypeSchemaMap);
        return new CompoundModelProvider(delegateProvider);
    }

    @NonNull
    @Override
    public Set<Class<? extends Model>> models() {
        return delegateProvider.models();
    }

    @Override
    public Map<String, ModelSchema> modelSchemas() {
        return delegateProvider.modelSchemas();
    }

    @Override
    public Set<String> modelNames() {
        return delegateProvider.modelNames();
    }

    @Override
    public Map<String, CustomTypeSchema> customTypeSchemas() {
        return delegateProvider.customTypeSchemas();
    }

    @Override
    public Set<String> customTypeNames() {
        return delegateProvider.customTypeNames();
    }

    @NonNull
    @Override
    public String version() {
        return delegateProvider.version();
    }

    @Override
    public boolean equals(Object thatObject) {
        if (!(thatObject instanceof ModelProvider)) {
            return false;
        }

        ModelProvider thatProvider = (ModelProvider) thatObject;
        return version().equals(thatProvider.version());
    }

    @Override
    public int hashCode() {
        return version().hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "CompoundModelsProvider{" +
            "delegateProvider=" + delegateProvider +
            '}';
    }
}
