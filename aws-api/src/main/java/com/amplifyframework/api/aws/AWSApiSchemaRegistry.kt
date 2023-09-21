package com.amplifyframework.api.aws

import com.amplifyframework.api.ApiException
import com.amplifyframework.core.model.CustomTypeSchema
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelSchema

internal object AWSApiSchemaRegistry {
    private val modelSchemaMap = mutableMapOf<String, ModelSchema>()
    // CustomType name => CustomTypeSchema map
    private val customTypeSchemaMap = mutableMapOf<String, CustomTypeSchema>()

    init {
        val modelProvider = ModelProviderLocator.locate()
        register(modelProvider.modelSchemas(), modelProvider.customTypeSchemas())
    }

    @Synchronized
    fun getModelSchemaForModelClass(classSimpleName: String): ModelSchema {
        return modelSchemaMap[classSimpleName] ?: throw ApiException(
            "Model type of `$classSimpleName` not found.",
            "Please regenerate codegen models and verify the class is found in AmplifyModelProvider."
        )
    }

    @Synchronized
    fun <T : Model> getModelSchemaForModelClass(modelClass: Class<T>): ModelSchema {
        return getModelSchemaForModelClass(modelClass.simpleName)
    }

    @Synchronized
    fun register(
        modelSchemas: Map<String, ModelSchema>,
        customTypeSchemas: Map<String, CustomTypeSchema>
    ) {
        modelSchemaMap.putAll(modelSchemas)
        customTypeSchemaMap.putAll(customTypeSchemas)
    }
}
