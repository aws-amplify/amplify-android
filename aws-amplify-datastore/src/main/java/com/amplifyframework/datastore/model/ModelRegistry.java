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

package com.amplifyframework.datastore.model;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility that creates ModelSchema from Model classes.
 */
public class ModelRegistry {
    private static final String TAG = ModelRegistry.class.getSimpleName();

    // ClassName => ModelSchema map
    private static Map<String, ModelSchema> modelSchemaMap;

    private static final Object LOCK = new Object();

    static {
        modelSchemaMap = new HashMap<String, ModelSchema>();
    }

    /**
     * Create the ModelSchema objects for all Model classes.
     * @param models the list that contains all the Model classes.
     */
    public static void createModelSchemaForModels(@NonNull List<Class<? extends Model>> models) {
        synchronized (LOCK) {
            for (Class<? extends Model> modelClass : models) {
                final String modelClassName = modelClass.getName();
                final ModelSchema modelSchema = ModelSchema.fromModelClass(modelClass);
                modelSchemaMap.put(modelClassName, modelSchema);
            }
        }
    }

    /**
     * Retrieve the ModelSchema object for the given Model class.
     * @param className name of the Model class.
     * @return the ModelSchema object for the given Model class.
     */
    public static ModelSchema getModelSchemaForModelClass(@NonNull String className) {
        synchronized (LOCK) {
            return modelSchemaMap.get(className);
        }
    }
}
