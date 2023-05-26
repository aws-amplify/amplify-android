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

package com.amplifyframework.api.aws;

import android.util.Log;


import com.amplifyframework.core.model.LazyModel;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LazyModelAdapter<M extends Model> implements JsonDeserializer<LazyModel<M>>,
        JsonSerializer<LazyModel<M>> {

    @SuppressWarnings("unchecked")
    @Override
    public LazyModel<M> deserialize(JsonElement json, Type typeOfT,
                                    JsonDeserializationContext context) throws JsonParseException {
        ParameterizedType pType = (ParameterizedType) typeOfT;
        Class<M> type = (Class<M>) pType.getActualTypeArguments()[0];

        Log.d("LazyModelAdapter", "json: "+ json + " typeOfT " + typeOfT +
                " typeOfT type name" + type + " context " +
                context);
        Map<String, Object> predicateKeyMap = new HashMap<>();
        Iterator<String> primaryKeysIterator = SchemaRegistry.instance()
                .getModelSchemaForModelClass(type)
                .getPrimaryIndexFields().iterator();
        JsonObject jsonObject = (JsonObject) json;
        while (primaryKeysIterator.hasNext()){
            String primaryKey = primaryKeysIterator.next();
            predicateKeyMap.put(primaryKey, jsonObject.get(primaryKey));
        }
        return new AppSyncLazyModel<>(type, predicateKeyMap, new AppSyncLazyQueryPredicate<>());
    }

    @Override
    public JsonElement serialize(LazyModel<M> src, Type typeOfSrc,
                                 JsonSerializationContext context) {
        return null;
    }
}