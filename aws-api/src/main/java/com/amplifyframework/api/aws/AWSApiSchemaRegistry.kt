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

package com.amplifyframework.api.aws

import com.amplifyframework.api.ApiException
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelSchema

/**
 * This registry is only used for API category and is capable of registering models with lazy support.
 * The DataStore schema registry restricts to non-lazy types
 */
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
