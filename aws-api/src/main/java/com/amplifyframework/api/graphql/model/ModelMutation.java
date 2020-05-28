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

package com.amplifyframework.api.graphql.model;

import androidx.annotation.NonNull;

import com.amplifyframework.api.aws.AppSyncGraphQLRequestFactory;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;

/**
 * Helper class that provides methods to create {@link GraphQLRequest} from {@link Model}.
 */
public final class ModelMutation {
    private ModelMutation() {}

    /**
     * Creates a {@link GraphQLRequest} that represents a create mutation for a given {@code model} instance.
     * @param model the model instance populated with values.
     * @param <M> the model concrete type.
     * @return a valid {@code GraphQLRequest} instance.
     * @see MutationType#CREATE
     */
    public static <M extends Model> GraphQLRequest<M> create(@NonNull M model) {
        return AppSyncGraphQLRequestFactory.buildMutation(model, QueryPredicates.all(), MutationType.CREATE);
    }

    /**
     * Creates a {@link GraphQLRequest} that represents an update mutation for a given {@code model} instance.
     * @param model the model instance populated with values.
     * @param predicate a predicate passed as the condition to apply the mutation.
     * @param <M> the model concrete type.
     * @return a valid {@code GraphQLRequest} instance.
     * @see MutationType#UPDATE
     */
    public static <M extends Model> GraphQLRequest<M> update(
            @NonNull M model,
            @NonNull QueryPredicate predicate
    ) {
        return AppSyncGraphQLRequestFactory.buildMutation(model, predicate, MutationType.UPDATE);
    }

    /**
     * Creates a {@link GraphQLRequest} that represents an update mutation for a given {@code model} instance.
     * @param model the model instance populated with values.
     * @param <M> the model concrete type.
     * @return a valid {@code GraphQLRequest} instance.
     * @see MutationType#UPDATE
     * @see #update(Model, QueryPredicate)
     */
    public static <M extends Model> GraphQLRequest<M> update(@NonNull M model) {
        return AppSyncGraphQLRequestFactory.buildMutation(model, QueryPredicates.all(), MutationType.UPDATE);
    }

    /**
     * Creates a {@link GraphQLRequest} that represents a delete mutation for a given {@code model} instance.
     * @param model the model instance populated with values.
     * @param predicate a predicate passed as the condition to apply the mutation.
     * @param <M> the model concrete type.
     * @return a valid {@code GraphQLRequest} instance.
     * @see MutationType#DELETE
     */
    public static <M extends Model> GraphQLRequest<M> delete(
            @NonNull M model,
            @NonNull QueryPredicate predicate
    ) {
        return AppSyncGraphQLRequestFactory.buildMutation(model, predicate, MutationType.DELETE);
    }

    /**
     * Creates a {@link GraphQLRequest} that represents a delete mutation for a given {@code model} instance.
     * @param model the model instance populated with values.
     * @param <M> the model concrete type.
     * @return a valid {@code GraphQLRequest} instance.
     * @see MutationType#DELETE
     * @see #delete(Model, QueryPredicate)
     */
    public static <M extends Model> GraphQLRequest<M> delete(@NonNull M model) {
        return AppSyncGraphQLRequestFactory.buildMutation(model, QueryPredicates.all(), MutationType.DELETE);
    }
}
