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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreException;

/**
 * Utility to create an adapter for sync responses. The Adapter listens to
 * String responses on the {@link ApiCategoryBehavior}, and emits them to a listener of
 * {@link ModelWithMetadata}, as needed by the {@link AppSyncEndpoint} interface.
 */
final class SyncAdapter {
    @SuppressWarnings("checkstyle:all") private SyncAdapter() {}

    static <T extends Model> ResultListener<GraphQLResponse<Iterable<String>>> instance(
            ResultListener<GraphQLResponse<Iterable<ModelWithMetadata<T>>>> responseListener,
            Class<T> itemClass,
            ResponseDeserializer responseDeserializer) {
        final Consumer<GraphQLResponse<Iterable<String>>> resultConsumer = resultFromApiQuery -> {
            if (resultFromApiQuery.hasErrors()) {
                responseListener.onError(new DataStoreException(
                    "Failure performing sync query to AppSync: " + resultFromApiQuery.getErrors().toString(),
                    AmplifyException.TODO_RECOVERY_SUGGESTION
                ));
            } else {
                responseListener.onResult(responseDeserializer.deserialize(resultFromApiQuery.getData(), itemClass));
            }
        };
        @SuppressWarnings("CodeBlock2Expr") // Block is more readable
        final Consumer<Throwable> errorConsumer = error -> {
            responseListener.onError(new DataStoreException(
                "Failure performing sync query to AppSync.",
                error, AmplifyException.TODO_RECOVERY_SUGGESTION
            ));
        };
        return ResultListener.instance(resultConsumer, errorConsumer);
    }
}
