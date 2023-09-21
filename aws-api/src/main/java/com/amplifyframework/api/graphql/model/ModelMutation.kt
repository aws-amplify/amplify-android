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
package com.amplifyframework.api.graphql.model

import com.amplifyframework.api.aws.AppSyncGraphQLRequestFactory.buildMutation
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.MutationType
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelPath
import com.amplifyframework.core.model.PropertyContainerPath
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.core.model.query.predicate.QueryPredicates

/**
 * Helper class that provides methods to create [GraphQLRequest] from [Model].
 */
object ModelMutation {
    /**
     * Creates a [GraphQLRequest] that represents a create mutation for a given `model` instance.
     * @param model the model instance populated with values.
     * @param <M> the model concrete type.
     * @return a valid `GraphQLRequest` instance.
     * @see MutationType.CREATE
     </M> */
    @JvmStatic
    fun <M : Model> create(model: M): GraphQLRequest<M> {
        return buildMutation(model, QueryPredicates.all(), MutationType.CREATE)
    }

    /**
     * Creates a [GraphQLRequest] that represents a create mutation for a given `model` instance.
     * @param model the model instance populated with values.
     * @param includes lambda returning list of relationships that should be included in the selection set
     * @param <M> the model concrete type.
     * @param <P> the concrete model path for the M model type
     * @return a valid `GraphQLRequest` instance.
     * @see MutationType.CREATE
     </M> */
    @JvmStatic
    fun <M : Model, P : ModelPath<M>> create(
        model: M,
        includes: ((P) -> List<PropertyContainerPath>)
    ): GraphQLRequest<M> {
        return buildMutation(model, QueryPredicates.all(), MutationType.CREATE, includes)
    }

    /**
     * Creates a [GraphQLRequest] that represents an update mutation for a given `model` instance.
     * @param model the model instance populated with values.
     * @param predicate a predicate passed as the condition to apply the mutation.
     * @param <M> the model concrete type.
     * @return a valid `GraphQLRequest` instance.
     * @see MutationType.UPDATE
     </M> */
    @JvmStatic
    fun <M : Model> update(
        model: M,
        predicate: QueryPredicate
    ): GraphQLRequest<M> {
        return buildMutation(model, predicate, MutationType.UPDATE)
    }

    /**
     * Creates a [GraphQLRequest] that represents an update mutation for a given `model` instance.
     * @param model the model instance populated with values.
     * @param predicate a predicate passed as the condition to apply the mutation.
     * @param includes lambda returning list of relationships that should be included in the selection set
     * @param <M> the model concrete type.
     * @param <P> the concrete model path for the M model type
     * @return a valid `GraphQLRequest` instance.
     * @see MutationType.UPDATE
     </M> */
    @JvmStatic
    fun <M : Model, P : ModelPath<M>> update(
        model: M,
        predicate: QueryPredicate,
        includes: ((P) -> List<PropertyContainerPath>)
    ): GraphQLRequest<M> {
        return buildMutation(model, predicate, MutationType.UPDATE, includes)
    }

    /**
     * Creates a [GraphQLRequest] that represents an update mutation for a given `model` instance.
     * @param model the model instance populated with values.
     * @param <M> the model concrete type.
     * @return a valid `GraphQLRequest` instance.
     * @see MutationType.UPDATE
     *
     * @see .update
     </M> */
    @JvmStatic
    fun <M : Model> update(model: M): GraphQLRequest<M> {
        return buildMutation(model, QueryPredicates.all(), MutationType.UPDATE)
    }

    /**
     * Creates a [GraphQLRequest] that represents an update mutation for a given `model` instance.
     * @param model the model instance populated with values.
     * @param includes lambda returning list of relationships that should be included in the selection set
     * @param <M> the model concrete type.
     * @param <P> the concrete model path for the M model type
     * @return a valid `GraphQLRequest` instance.
     * @see MutationType.UPDATE
     </M> */
    @JvmStatic
    fun <M : Model, P : ModelPath<M>> update(
        model: M,
        includes: ((P) -> List<PropertyContainerPath>)
    ): GraphQLRequest<M> {
        return buildMutation(model, QueryPredicates.all(), MutationType.UPDATE, includes)
    }

    /**
     * Creates a [GraphQLRequest] that represents a delete mutation for a given `model` instance.
     * @param model the model instance populated with values.
     * @param predicate a predicate passed as the condition to apply the mutation.
     * @param <M> the model concrete type.
     * @return a valid `GraphQLRequest` instance.
     * @see MutationType.DELETE
     </M> */
    @JvmStatic
    fun <M : Model> delete(
        model: M,
        predicate: QueryPredicate
    ): GraphQLRequest<M> {
        return buildMutation(model, predicate, MutationType.DELETE)
    }

    /**
     * Creates a [GraphQLRequest] that represents a delete mutation for a given `model` instance.
     * @param model the model instance populated with values.
     * @param predicate a predicate passed as the condition to apply the mutation.
     * @param includes lambda returning list of relationships that should be included in the selection set
     * @param <M> the model concrete type.
     * @param <P> the concrete model path for the M model type
     * @return a valid `GraphQLRequest` instance.
     * @see MutationType.DELETE
     </M> */
    @JvmStatic
    fun <M : Model, P : ModelPath<M>> delete(
        model: M,
        predicate: QueryPredicate,
        includes: ((P) -> List<PropertyContainerPath>)
    ): GraphQLRequest<M> {
        return buildMutation(model, predicate, MutationType.DELETE, includes)
    }

    /**
     * Creates a [GraphQLRequest] that represents a delete mutation for a given `model` instance.
     * @param model the model instance populated with values.
     * @param <M> the model concrete type.
     * @return a valid `GraphQLRequest` instance.
     * @see MutationType.DELETE
     *
     * @see .delete
     </M> */
    @JvmStatic
    fun <M : Model> delete(model: M): GraphQLRequest<M> {
        return buildMutation(model, QueryPredicates.all(), MutationType.DELETE)
    }

    /**
     * Creates a [GraphQLRequest] that represents a delete mutation for a given `model` instance.
     * @param model the model instance populated with values.
     * @param includes lambda returning list of relationships that should be included in the selection set
     * @param <M> the model concrete type.
     * @param <P> the concrete model path for the M model type
     * @return a valid `GraphQLRequest` instance.
     * @see MutationType.DELETE
     </M> */
    @JvmStatic
    fun <M : Model, P : ModelPath<M>> delete(
        model: M,
        includes: ((P) -> List<PropertyContainerPath>)
    ): GraphQLRequest<M> {
        return buildMutation(model, QueryPredicates.all(), MutationType.DELETE, includes)
    }
}
