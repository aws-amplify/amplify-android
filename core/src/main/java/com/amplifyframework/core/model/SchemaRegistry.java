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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.util.Immutable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A utility that creates ModelSchema from Model classes.
 */
public final class SchemaRegistry {
    private static SchemaRegistry instance;
    // Model ClassName => ModelSchema map
    private final Map<String, ModelSchema> modelSchemaMap;
    // CustomType name => CustomTypeSchema map
    private final Map<String, CustomTypeSchema> customTypeSchemaMap;

    private SchemaRegistry() {
        modelSchemaMap = new HashMap<>();
        customTypeSchemaMap = new HashMap<>();
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
            SchemaRegistryUtils.registerSchema(modelSchemaMap, modelClassName, modelSchema);
        }
    }

    /**
     * Registers the modelSchemas provided.
     * @param modelSchemas the map that contains mapping of ModelName to ModelSchema.
     */
    public synchronized void register(@NonNull Map<String, ModelSchema> modelSchemas) {
        SchemaRegistryUtils.registerSchemas(modelSchemaMap, modelSchemas);
    }

    /**
     * Register the modelSchemas and customTypeSchemas provided.
     * This method is consumed with Flutter use cases.
     * @param modelSchemas the map that contains mapping of ModelName to ModelSchema.
     * @param customTypeSchemas the map that contains mapping of CustomTypeName to CustomTypeSchema.
     */
    public synchronized void register(
            @NonNull Map<String, ModelSchema> modelSchemas,
            @NonNull Map<String, CustomTypeSchema> customTypeSchemas) {
        SchemaRegistryUtils.registerSchemas(modelSchemaMap, modelSchemas);
        customTypeSchemaMap.putAll(customTypeSchemas);
    }

    /**
     * Registers the modelSchema for the given modelName.
     * @param modelName name of the model
     * @param modelSchema schema of the model to be registered.
     */
    public synchronized void register(@NonNull String modelName, @NonNull ModelSchema modelSchema) {
        SchemaRegistryUtils.registerSchema(modelSchemaMap, modelName, modelSchema);
    }

    /**
     * Registers the customTypeSchema for the given customTypeName.
     * @param customTypeName name of the model
     * @param customTypeSchema schema of the model to be registered.
     */
    public synchronized void register(@NonNull String customTypeName, @NonNull CustomTypeSchema customTypeSchema) {
        customTypeSchemaMap.put(customTypeName, customTypeSchema);
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
     * Retrieve the CustomTypeSchema object for the given custom type name.
     * @param customTypeName name of the custom type retrieved through field target type
     *                       {@link CustomTypeField#getTargetType()}
     * @return the ModelSchema object for the given Model class (non-model type).
     */
    public synchronized CustomTypeSchema getCustomTypeSchemaForCustomTypeClass(@NonNull String customTypeName) {
        return customTypeSchemaMap.get(customTypeName);
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
     * Retrieve the map of Model CustomTypeName => CustomTypeSchema.
     * @return an immutable map of Model CustomTypeName => CustomTypeSchema
     */
    @NonNull
    public Map<String, CustomTypeSchema> getCustomTypeSchemaMap() {
        return Immutable.of(customTypeSchemaMap);
    }

    /**
     * Creates a new instance.
     * @return A new instance
     */
    @NonNull
    public static synchronized SchemaRegistry instance() {
        if (SchemaRegistry.instance == null) {
            SchemaRegistry.instance = new SchemaRegistry();
        }

        return SchemaRegistry.instance;
    }

    /**
     * Clears the registry.
     */
    public void clear() {
        this.modelSchemaMap.clear();
        this.customTypeSchemaMap.clear();
    }
}
