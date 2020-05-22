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

package com.amplifyframework.core.model.graphql;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.api.aws.AppSyncGraphQLRequestFactory;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

/**
 * TODO write docs.
 */
public final class ModelQuery {

    private ModelQuery() {}

    /**
     * TODO write docs.
     * @param modelType todo.
     * @param modelId todo.
     * @param <M> todo.
     * @return todo.
     */
    public static <M extends Model> GraphQLRequest<M> get(
            @NonNull Class<M> modelType,
            @NonNull String modelId
    ) {
        return AppSyncGraphQLRequestFactory.buildQuery(modelType, modelId);
    }

    /**
     * TODO write docs.
     * @param modelType todo.
     * @param predicate todo.
     * @param <M> todo.
     * @return todo.
     */
    public static <M extends Model> GraphQLRequest<Iterable<M>> list(
            @NonNull Class<M> modelType,
            @Nullable QueryPredicate predicate
    ) {
        return AppSyncGraphQLRequestFactory.buildQuery(modelType, predicate);
    }

    /**
     * TODO write docs.
     * @param modelType todo.
     * @param <M> todo.
     * @return todo.
     */
    public static <M extends Model> GraphQLRequest<Iterable<M>> list(@NonNull Class<M> modelType) {
        return list(modelType, (QueryPredicate) null);
    }

    /**
     * TODO write docs.
     * @param modelType todo.
     * @param predicate todo.
     * @param pagination todo.
     * @param <M> todo.
     * @return todo.
     */
    public static <M extends Model> GraphQLRequest<PaginatedResult<M>> list(
            @NonNull Class<M> modelType,
            @NonNull QueryPredicate predicate,
            @NonNull ModelPagination pagination
    ) {
        return AppSyncGraphQLRequestFactory.buildPaginatedResultQuery(
                modelType, predicate, pagination.getLimit(), pagination.getNextToken());
    }

    /**
     * TODO write docs.
     * @param modelType todo.
     * @param pagination todo.
     * @param <M> todo.
     * @return todo.
     */
    public static <M extends Model> GraphQLRequest<PaginatedResult<M>> list(
            @NonNull Class<M> modelType,
            @NonNull ModelPagination pagination
    ) {
        return AppSyncGraphQLRequestFactory.buildPaginatedResultQuery(
                modelType, null, pagination.getLimit(), pagination.getNextToken());
    }

}
