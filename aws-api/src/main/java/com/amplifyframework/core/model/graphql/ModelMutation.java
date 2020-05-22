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

import com.amplifyframework.api.aws.AppSyncGraphQLRequestFactory;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

/**
 * TODO docs.
 */
public final class ModelMutation {

    private ModelMutation() {
    }

    /**
     * TODO docs.
     * @param model todo.
     * @param <M> todo.
     * @return todo.
     */
    public static <M extends Model> GraphQLRequest<M> create(M model) {
        return AppSyncGraphQLRequestFactory.buildMutation(model, null, MutationType.CREATE);
    }

    /**
     * TODO docs.
     * @param model todo.
     * @param predicate todo.
     * @param <M> todo.
     * @return todo.
     */
    public static <M extends Model> GraphQLRequest<M> update(M model, QueryPredicate predicate) {
        return AppSyncGraphQLRequestFactory.buildMutation(model, predicate, MutationType.UPDATE);
    }

    /**
     * TODO docs.
     * @param model todo.
     * @param <M> todo.
     * @return todo.
     */
    public static <M extends Model> GraphQLRequest<M> update(M model) {
        return AppSyncGraphQLRequestFactory.buildMutation(model, null, MutationType.UPDATE);
    }

    /**
     * TODO docs.
     * @param model todo.
     * @param predicate todo.
     * @param <M> todo.
     * @return todo.
     */
    public static <M extends Model> GraphQLRequest<M> delete(M model, QueryPredicate predicate) {
        return AppSyncGraphQLRequestFactory.buildMutation(model, predicate, MutationType.DELETE);
    }

    /**
     * TODO docs.
     * @param model todo.
     * @param <M> todo.
     * @return todo.
     */
    public static <M extends Model> GraphQLRequest<M> delete(M model) {
        return AppSyncGraphQLRequestFactory.buildMutation(model, null, MutationType.DELETE);
    }

}
