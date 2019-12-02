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

public class AppSyncApiModelSync implements ApiModelSync {
    @Override
    public <T extends Model> GraphQLOperation<T> query(
            @NonNull String apiName,
            @NonNull Class<T> modelClass,
            QueryPredicate predicate,
            @NonNull Long lastSync,
            @Nullable ResultListener<GraphQLResponse<Iterable<ModelWithMetadata<T>>>> responseListener
    ) {
        return null;
    }

    @Override
    public <T extends Model> GraphQLOperation<T> mutate(
            @NonNull String apiName,
            @NonNull T model,
            QueryPredicate predicate,
            @NonNull MutationType type,
            @NonNull Long lastSync,
            @Nullable ResultListener<GraphQLResponse<ModelWithMetadata<T>>> responseListener
    ) {
        return null;
    }
}
