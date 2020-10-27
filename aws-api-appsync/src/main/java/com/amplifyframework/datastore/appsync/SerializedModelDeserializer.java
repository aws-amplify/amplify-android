/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.datastore.appsync;

import com.amplifyframework.api.aws.AppSyncGraphQLRequest;
import com.amplifyframework.util.GsonObjectConverter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Deserializer for SerializedModel. Helpful to deserialize from the graphql response.
 * @param <T> type parameter
 */
public class SerializedModelDeserializer<T> implements JsonDeserializer<SerializedModel> {
    private AppSyncGraphQLRequest<T> request;

    /**
     * Holds a reference of the graphql request which is later used to determine
     * the model type during deserialization.
     * @param request original graphql request
     */
    public SerializedModelDeserializer(AppSyncGraphQLRequest<T> request) {
        this.request = request;
    }

    /**
     * Deserialize SerializedModel.
     * Todo: strip off system metadata fields
     */
    @Override
    public SerializedModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        return SerializedModel.builder()
                .serializedData(GsonObjectConverter.toMap(object))
                .id(object.get("id").getAsString())
                .modelName(request.getModelSchema().getName())
                .build();
    }
}
