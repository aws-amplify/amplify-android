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

package com.amplifyframework.datastore.network;

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.model.Model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused") // remove me later ...
final class GsonResponseDeserializer implements ResponseDeserializer {
    private final Gson gson;

    GsonResponseDeserializer() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(List.class, new GsonListDeserializer())
                .create();
    }

    @Override
    public <T extends Model> GraphQLResponse<ModelWithMetadata<T>> deserialize(String json, Class<T> clazz) {
        ModelWithMetadata<T> modelWithMetadata = toModelWithMetadata(json, clazz);
        return new GraphQLResponse<>(modelWithMetadata, Collections.emptyList());
    }

    @Override
    public <T extends Model> GraphQLResponse<Iterable<ModelWithMetadata<T>>> deserialize(
            Iterable<String> jsons, Class<T> memberClazz) {
        List<ModelWithMetadata<T>> modelWithMetadataList = new ArrayList<>();
        for (String jsonItem : jsons) {
            modelWithMetadataList.add(toModelWithMetadata(jsonItem, memberClazz));
        }
        return new GraphQLResponse<>(modelWithMetadataList, Collections.emptyList());
    }

    private <T extends Model> ModelWithMetadata<T> toModelWithMetadata(String json, Class<T> clazz) {
        T model = gson.fromJson(json, clazz);
        ModelMetadata metadata = gson.fromJson(json, ModelMetadata.class);
        return new ModelWithMetadata<>(model, metadata);
    }
}
