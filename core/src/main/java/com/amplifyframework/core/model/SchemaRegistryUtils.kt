/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core.model

import com.amplifyframework.core.model.annotations.ModelConfig
import com.amplifyframework.datastore.DataStoreException.IrRecoverableException
import java.lang.NullPointerException

internal object SchemaRegistryUtils {

    /**
     * Registers the ModelSchema's while filtering out unsupported lazy types
     */
    @JvmStatic
    fun registerSchemas(
        modelSchemaMap: MutableMap<String, ModelSchema>,
        modelSchemas: Map<String, ModelSchema>? = null,
    ) {
        modelSchemas?.forEach { (name, schema) ->
            registerSchema(modelSchemaMap, name, schema)
        }
    }

    /**
     * Registers the ModelSchema while filtering out unsupported lazy types
     */
    @JvmStatic
    fun registerSchema(
        modelSchemaMap: MutableMap<String, ModelSchema>,
        modelName: String,
        modelSchema: ModelSchema
    ) {

        try {
            if (modelSchema.modelClass.getAnnotation(ModelConfig::class.java)?.hasLazySupport == true) {
                throw IrRecoverableException(
                    "Unsupported model type. Lazy model types are not yet supported on DataStore.",
                    "Regenerate models with generatemodelsforlazyloadandcustomselectionset=false."
                )
            }
        } catch (npe: NullPointerException) {
            /*
            modelSchema.modelClass could throw if modelClass was not set from builder.
            This is likely not a valid scenario, as modelClass should be required, but
            we have a number of test cases that don't provide one. Since the builder is public and
            modelClass isn't a mandatory builder param, we add this block for additional safety.
            */
        }

        modelSchemaMap[modelName] = modelSchema
    }
}
