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

import android.annotation.SuppressLint;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreException;

/**
 * An adapter to go between the response listener type which listens to
 * String repsonses on the {@link ApiCategoryBehavior}, and one which listens for
 * {@link ModelWithMetadata}, as needed by the {@link AppSyncEndpoint} interface.
 * @param <T> Type of model being sync'd
 */
final class SyncAdapter<T extends Model> implements ResultListener<GraphQLResponse<Iterable<String>>> {

    private final ResultListener<GraphQLResponse<Iterable<ModelWithMetadata<T>>>> responseListener;
    private final Class<T> itemClass;
    private final ResponseDeserializer responseDeserializer;

    SyncAdapter(
            ResultListener<GraphQLResponse<Iterable<ModelWithMetadata<T>>>> responseListener,
            Class<T> itemClass,
            ResponseDeserializer responseDeserializer) {
        this.responseListener = responseListener;
        this.itemClass = itemClass;
        this.responseDeserializer = responseDeserializer;
    }

    @SuppressLint("SyntheticAccessor")
    @Override
    public void onResult(GraphQLResponse<Iterable<String>> resultFromApiQuery) {
        if (resultFromApiQuery.hasErrors()) {
            responseListener.onError(new DataStoreException(
                "Failure performing sync query to AppSync: " + resultFromApiQuery.getErrors().toString(),
                AmplifyException.TODO_RECOVERY_SUGGESTION
            ));
        } else {
            responseListener.onResult(responseDeserializer.deserialize(resultFromApiQuery.getData(), itemClass));
        }
    }

    @Override
    public void onError(Throwable error) {
        responseListener.onError(new DataStoreException(
            "Failure performing sync query to AppSync.",
            error, AmplifyException.TODO_RECOVERY_SUGGESTION
        ));
    }
}
