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

import com.amplifyframework.AmplifyException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Defines the contract for retrieving information about the
 * models generated for DataStoreCategoryBehavior.
 */
public interface ModelProvider {
    /**
     * Get a set of the model classes.
     * @return a set of the model classes.
     */
    Set<Class<? extends Model>> models();

    /**
     * Get the version of the models.
     * @return the version string of the models.
     */
    String version();

    /**
     * A default method to keep backwards compatibility with models
     * that do not provide modelSchemas. This method iterates over
     * all the models that _this_ modelProvider provides and returns
     * modelName to modelSchema map.
     * @return the map of model name to schema of all the models.
     */
    default Map<String, ModelSchema> modelSchemas() {
        final Map<String, ModelSchema> modelSchemaMap = new HashMap<>();
        if (models() == null) {
            return modelSchemaMap;
        }
        for (Class<? extends Model> modelClass : models()) {
            try {
                final String modelClassName = modelClass.getSimpleName();
                final ModelSchema modelSchema = ModelSchema.fromModelClass(modelClass);
                modelSchemaMap.put(modelClassName, modelSchema);
            } catch (AmplifyException exception) {
                exception.printStackTrace();
            }
        }
        return modelSchemaMap;
    }

    /**
     * A default helper method to return all the model names.
     * @return a set of all model names.
     */
    default Set<String> modelNames() {
        final Set<String> modelNames = new HashSet<>();
        if (models() == null) {
            return modelNames;
        }
        for (Class<? extends Model> modelClass : models()) {
            modelNames.add(modelClass.getSimpleName());
        }
        return modelNames;
    }
}
