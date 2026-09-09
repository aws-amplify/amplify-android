/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.appsync

import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.ModelSchema

/**
 * Looks up the schema for a model type, which is what supplies the primary key field names a lazy
 * reference needs in order to build its own query.
 *
 * The schemas are resolved on first lookup, not at construction, so a consumer whose responses carry
 * no lazily-loaded relationships never triggers the reflective search for the code-generated provider.
 */
internal class AppSyncSchemaRegistry(
    private val modelProvider: () -> ModelProvider = { AppSyncModelProviderLocator.locate() }
) {
    private val schemas: Map<String, ModelSchema> by lazy { modelProvider().modelSchemas() }

    fun <M : Model> schemaFor(modelClass: Class<M>): ModelSchema = schemaFor(modelClass.simpleName)

    fun schemaFor(modelName: String): ModelSchema = schemas[modelName] ?: throw AppSyncInvalidConfigException(
        message = "No schema for the model type $modelName.",
        recoverySuggestion = "Regenerate the models and verify $modelName is listed in AmplifyModelProvider."
    )
}
