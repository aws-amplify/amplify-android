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
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;

final class MutationAdapter<T extends Model> implements ResultListener<GraphQLResponse<String>> {
    private final Class<T> itemClass;
    private final ResponseDeserializer responseDeserializer;
    private final ResultListener<GraphQLResponse<ModelWithMetadata<T>>> responseListener;

    MutationAdapter(
            ResultListener<GraphQLResponse<ModelWithMetadata<T>>> responseListener,
            Class<T> itemClass,
            ResponseDeserializer responseDeserializer) {
        this.responseListener = responseListener;
        this.itemClass = itemClass;
        this.responseDeserializer = responseDeserializer;
    }

    @Override
    public void onResult(GraphQLResponse<String> result) {
        if (result.hasErrors()) {
            responseListener.onResult(new GraphQLResponse<>(null, result.getErrors()));
            return;
        }
        responseListener.onResult(responseDeserializer.deserialize(result.getData(), itemClass));

    }

    @Override
    public void onError(Throwable error) {
        responseListener.onError(error);
    }
}
