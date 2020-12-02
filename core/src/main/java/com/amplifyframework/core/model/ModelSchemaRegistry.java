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
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.util.Immutable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A utility that creates ModelSchema from Model classes.
 */
public final class ModelSchemaRegistry {
    // Model ClassName => ModelSchema map
    private final Map<String, ModelSchema> modelSchemaMap;

    private ModelSchemaRegistry() {
        modelSchemaMap = new HashMap<>();
    }

    /**
     * Computes ModelSchema for each of the provided models, and registers them.
     * @param models the set that contains all the Model classes.
     * @throws AmplifyException if unable to create a Model Schema for a model
     */
    public synchronized void register(@NonNull Set<Class<? extends Model>> models) throws AmplifyException {
        for (Class<? extends Model> modelClass : models) {
            final String modelClassName = modelClass.getSimpleName();
            final ModelSchema modelSchema = ModelSchema.fromModelClass(modelClass);
            modelSchemaMap.put(modelClassName, modelSchema);
        }
    }

    /**
     * Registers the modelSchemas provided.
     * @param modelSchemas the map that contains mapping of ModelName to ModelSchema.
     */
    public synchronized void register(@NonNull Map<String, ModelSchema> modelSchemas) {
        modelSchemaMap.putAll(modelSchemas);
    }

    /**
     * Registers the modelSchema for the given modelName.
     * @param modelName name of the model
     * @param modelSchema schema of the model to be registered.
     */
    public synchronized void register(@NonNull String modelName, @NonNull ModelSchema modelSchema) {
        modelSchemaMap.put(modelName, modelSchema);
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
     * Retrieve the ModelSchema object for the given Model class.
     * @param modelClass A model class
     * @param <T> Type of item for which a schema is being built
     * @return the ModelSchema object for the given Model class.
     */
    public synchronized <T extends Model> ModelSchema getModelSchemaForModelClass(@NonNull Class<T> modelClass) {
        return modelSchemaMap.get(modelClass.getSimpleName());
    }

    /**
     * Retrieve the ModelSchema object for the given Model instance.
     * @param modelInstance instance of the Model class
     * @param <T> type of the model instance
     * @return the ModelSchema object for the given Model instance.
     */
    public synchronized <T extends Model> ModelSchema getModelSchemaForModelInstance(@NonNull T modelInstance) {
        return modelSchemaMap.get(modelInstance.getClass().getSimpleName());
    }

    /**
     * Retrieve the map of Model ClassName => ModelSchema.
     * @return an immutable map of Model ClassName => ModelSchema
     */
    @NonNull
    public Map<String, ModelSchema> getModelSchemaMap() {
        return Immutable.of(modelSchemaMap);
    }

    /**
     * Creates a new instance.
     * @return A new instance
     */
    @NonNull
    public static synchronized ModelSchemaRegistry instance() {
        return new ModelSchemaRegistry();
    }

    /**
     * Clears the registry.
     */
    public void clear() {
        this.modelSchemaMap.clear();
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }
        ModelSchemaRegistry registry = (ModelSchemaRegistry) thatObject;
        return getModelSchemaMap().equals(registry.getModelSchemaMap());
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(getModelSchemaMap());
    }

    @NonNull
    @Override
    public String toString() {
        return "ModelSchemaRegistry{" +
            "modelSchemaMap=" + modelSchemaMap +
            '}';
    }
}
