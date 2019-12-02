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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

/**
 * Convenience class to extend API query and mutate capabilities to also support versioning.
 */
public final class ModelSync {
    private ModelSync() { }

    /**
     * Uses Amplify API category to get a list of changes which have happened since a last sync time.
     * @param apiName The name of a configured API
     * @param modelClass The class of the Model we are querying on
     * @param predicate Filtering conditions for the query
     * @param lastSync The time you last synced - all changes since this time are retrieved.
     * @param responseListener Invoked when response data/errors are available.  If null,
     *        response can still be obtained via Hub.
     * @param <T> The type of data in the response, if available. Must extend Model.
     * @return A {@link GraphQLOperation} to track progress and provide
     *        a means to cancel the asynchronous operation
     */
    public static <T extends Model> GraphQLOperation<T> query(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            QueryPredicate predicate,
            @NonNull Long lastSync,
            @Nullable ResultListener<GraphQLResponse<Iterable<ModelSyncEntry<T>>>> responseListener
    ) {
        throw new UnsupportedOperationException("Coming soon...");
    }

    /**
     * Uses Amplify API to make a mutation and record versioning information.
     * @param apiName The name of a configured API
     * @param model An instance of the Model with the values to mutate
     * @param predicate Conditions on the current data to determine whether to go through
     *                   with an UPDATE or DELETE operation
     * @param type What type of mutation to perform (e.g. Create, Update, Delete)
     * @param lastSync The time you last synced - all changes since this time are retrieved.
     * @param responseListener Invoked when response data/errors are available.  If null,
     *        response can still be obtained via Hub.
     * @param <T> The type of data in the response, if available. Must extend Model.
     * @return A {@link GraphQLOperation} to track progress and provide
     *        a means to cancel the asynchronous operation
     */
    public static <T extends Model> GraphQLOperation<T> mutate(
            @NonNull String apiName,
            @NonNull T model,
            QueryPredicate predicate,
            @NonNull MutationType type,
            @NonNull Long lastSync,
            @Nullable ResultListener<GraphQLResponse<ModelSyncEntry<T>>> responseListener
    ) {
        throw new UnsupportedOperationException("Coming soon...");
    }
}
