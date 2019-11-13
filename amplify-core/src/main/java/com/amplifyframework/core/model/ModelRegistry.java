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

package com.amplifyframework.core.model;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Immutable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility that creates ModelSchema from Model classes.
 */
public final class ModelRegistry {
    // Singleton instance
    private static ModelRegistry modelRegistryInstance;

    // Model ClassName => ModelSchema map
    private final Map<String, ModelSchema> modelSchemaMap;

    private ModelRegistry() {
        modelSchemaMap = new HashMap<>();
    }

    /**
     * Create the ModelSchema objects for all Model classes.
     * @param models the list that contains all the Model classes.
     */
    public synchronized void load(@NonNull List<Class<? extends Model>> models) {
        for (Class<? extends Model> modelClass : models) {
            final String modelClassName = modelClass.getSimpleName();
            final ModelSchema modelSchema = ModelSchema.fromModelClass(modelClass);
            modelSchemaMap.put(modelClassName, modelSchema);
        }
    }

    /**
     * Retrieve the ModelSchema object for the given Model class.
     * @param classSimpleName name of the Model class retrieved through
     *                        {@link Class#getSimpleName()} method.
     * @return the ModelSchema object for the given Model class.
     */
    public synchronized ModelSchema getModelSchemaForModelClass(@NonNull String classSimpleName) {
        return modelSchemaMap.get(classSimpleName);
    }

    /**
     * Retrieve the map of Model ClassName => ModelSchema.
     * @return an immutable map of Model ClassName => ModelSchema
     */
    public Map<String, ModelSchema> getModelSchemaMap() {
        return Immutable.of(modelSchemaMap);
    }

    /**
     * Returns the singleton instance.
     * @return the singleton instance of the ModelRegistry.
     */
    public static synchronized ModelRegistry getInstance() {
        if (modelRegistryInstance == null) {
            modelRegistryInstance = new ModelRegistry();
        }
        return modelRegistryInstance;
    }
}
