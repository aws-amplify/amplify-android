package com.amplifyframework.core.model

import com.amplifyframework.core.model.annotations.ModelConfig
import com.amplifyframework.datastore.DataStoreException.IrRecoverableException

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
        modelSchema: ModelSchema) {
        if (modelSchema.modelClass.getAnnotation(ModelConfig::class.java)?.hasLazySupport == true) {
            throw IrRecoverableException(
                "Unsupported model type. Lazy model types are not yet supported on DataStore.",
                "Regenerate models with generatemodelsforlazyloadandcustomselectionset=false."
            )
        } else {
            modelSchemaMap[modelName] = modelSchema
        }
    }
}