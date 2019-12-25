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

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreException;

final class MutationAdapter {
    @SuppressWarnings("checkstyle:all") private MutationAdapter() {}

    static <T extends Model> ResultListener<GraphQLResponse<String>, ApiException> instance(
            ResultListener<GraphQLResponse<ModelWithMetadata<T>>, DataStoreException> responseListener,
            Class<T> itemClass,
            ResponseDeserializer responseDeserializer) {

        final Consumer<GraphQLResponse<String>> resultConsumer = result -> {
            if (result.hasErrors()) {
                responseListener.onResult(new GraphQLResponse<>(null, result.getErrors()));
                return;
            }
            responseListener.onResult(responseDeserializer.deserialize(result.getData(), itemClass));
        };

        //noinspection CodeBlock2Expr Keep down line length a bit
        return ResultListener.instance(resultConsumer, error -> {
            responseListener.onError(new DataStoreException("Error during mutation.", error, "Check details."));
        });
    }
}
