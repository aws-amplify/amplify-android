/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.api.aws

import com.amplifyframework.core.model.LoadedModelReferenceImpl
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelIdentifier
import com.amplifyframework.core.model.ModelSchema
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.io.Serializable

/**
 * This class is used to inject values into lazy model/list reference types when the fields were not included
 * in the json response.
 *
 * If a ModelReference is not included in the response json, it means the reference value is null.
 * If a ModelList is not included in the response json, it means that the list must be lazily loaded.
 * We must create the ModelList type, injecting required values such as query keys, api name.
 */
internal class ModelPostProcessingTypeAdapter(
    private val apiName: String?,
    private val schemaRegistry: AWSApiSchemaRegistry
) : TypeAdapterFactory {
    override fun <M> create(gson: Gson, type: TypeToken<M>): TypeAdapter<M> {
        val delegate = gson.getDelegateAdapter(this, type)

        return object : TypeAdapter<M>() {
            @Throws(IOException::class)
            override fun write(out: JsonWriter, value: M) {
                delegate.write(out, value)
            }

            @Throws(IOException::class)
            override fun read(`in`: JsonReader): M {
                val obj = delegate.read(`in`)
                (obj as? Model)?.let { injectLazyValues(it) }
                return obj
            }

            fun injectLazyValues(parent: Model) {
                val parentType = parent.javaClass.simpleName
                val parentModelSchema = ModelSchema.fromModelClass(parent.javaClass)

                parentModelSchema.fields.filter { it.value.isModelList || it.value.isModelReference }.map { fieldMap ->
                    val fieldToUpdate = parent.javaClass.getDeclaredField(fieldMap.key)
                    fieldToUpdate.isAccessible = true
                    if (fieldToUpdate.get(parent) == null) {
                        val lazyField = fieldMap.value

                        when {
                            fieldMap.value.isModelReference -> {
                                val modelReference = LoadedModelReferenceImpl(null)
                                fieldToUpdate.set(parent, modelReference)
                            }
                            fieldMap.value.isModelList -> {
                                val lazyFieldModelSchema = schemaRegistry
                                    .getModelSchemaForModelClass(lazyField.targetType)

                                val lazyFieldTargetNames = lazyFieldModelSchema
                                    .associations
                                    .values
                                    .first { it.associatedType == parentType }
                                    .targetNames

                                val parentIdentifiers = parent.getSortedIdentifiers()

                                val queryKeys = lazyFieldTargetNames.mapIndexed { idx, name ->
                                    name to parentIdentifiers[idx]
                                }.toMap()

                                val modelList = ApiLazyModelList(lazyFieldModelSchema.modelClass, queryKeys, apiName)

                                fieldToUpdate.set(parent, modelList)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Model.getSortedIdentifiers(): List<Serializable> {
    return when (val identifier = resolveIdentifier()) {
        is ModelIdentifier<*> -> { listOf(identifier.key()) + identifier.sortedKeys() }
        else -> listOf(identifier.toString())
    }
}
