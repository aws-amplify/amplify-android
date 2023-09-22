package com.amplifyframework.api.aws

import com.amplifyframework.api.ApiException
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelSchema

internal class AWSApiSchemaRegistry {
    private val modelSchemaMap: MutableMap<String, ModelSchema> by lazy {
        val modelProvider = ModelProviderLocator.locate()
        modelProvider.modelSchemas()
    }

    fun getModelSchemaForModelClass(classSimpleName: String): ModelSchema {
        return modelSchemaMap[classSimpleName] ?: throw ApiException(
            "Model type of `$classSimpleName` not found.",
            "Please regenerate codegen models and verify the class is found in AmplifyModelProvider."
        )
    }

    fun <T : Model> getModelSchemaForModelClass(modelClass: Class<T>): ModelSchema {
        return getModelSchemaForModelClass(modelClass.simpleName)
    }
}
