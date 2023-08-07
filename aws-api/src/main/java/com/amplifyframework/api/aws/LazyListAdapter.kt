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

import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.api.aws.ApiLazyListModel.Companion.createPreloaded
import com.amplifyframework.core.model.LazyList
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.SchemaRegistry
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

@InternalAmplifyApi
class LazyListAdapter<M : Model> : JsonDeserializer<LazyList<M>>, JsonSerializer<LazyList<M>> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): LazyList<M> {
        val pType = typeOfT as ParameterizedType
        val type = pType.actualTypeArguments[0] as Class<M>
        val jsonObject = json.asJsonObject
        val itemsJsonArray = jsonObject.getAsJsonArray("items")

       val items = itemsJsonArray.map {
            context.deserialize<M>(it.asJsonObject, type)
        }

        val primaryKeysIterator: Iterator<String> = SchemaRegistry.instance()
            .getModelSchemaForModelClass(type)
            .primaryIndexFields.iterator()
        val predicateKeyMap: MutableMap<String, Any> = HashMap()
        while (primaryKeysIterator.hasNext()) {
            val key = primaryKeysIterator.next()
            val value = jsonObject.get(key)
            if (value != null) {
                predicateKeyMap[key] = value
            }
        }

        return createPreloaded(type, predicateKeyMap, items)
    }

    override fun serialize(
        src: LazyList<M>, typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement? {
        return null
    }
}