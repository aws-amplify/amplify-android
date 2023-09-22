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

import com.amplifyframework.api.aws.AppSyncGraphQLRequestFactory
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelIdentifier
import com.amplifyframework.core.model.ModelPath
import com.amplifyframework.core.model.PropertyContainerPath
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.core.model.query.predicate.QueryPredicates

/**
 * Helper class that provides methods to create [GraphQLRequest] for queries
 * from [Model] and [QueryPredicate].
 */
object ModelQuery {

    /**
     * Creates a [GraphQLRequest] that represents a query that expects a single value as a result.
     * The request will be created with the correct correct document based on the model schema and
     * variables based on given `modelId`.
     * @param modelType the model class.
     * @param modelId the model identifier.
     * @param <M> the concrete model type.
     * @return a valid [GraphQLRequest] instance.
     </M> */
    @JvmStatic
    operator fun <M : Model> get(
        modelType: Class<M>,
        modelId: String,
    ): GraphQLRequest<M> {
        return AppSyncGraphQLRequestFactory.buildQuery(modelType, modelId)
    }

    /**
     * Creates a [GraphQLRequest] that represents a query that expects a single value as a result.
     * The request will be created with the correct correct document based on the model schema and
     * variables based on given `modelId`.
     * @param modelType the model class.
     * @param modelId the model identifier.
     * @param includes lambda returning list of relationships that should be included in the selection set
     * @param <M> the concrete model type.
     * @param <P> the concrete model path for the M model type
     * @return a valid [GraphQLRequest] instance.
     </M> */
    @JvmStatic
    operator fun <M : Model, P : ModelPath<M>> get(
        modelType: Class<M>,
        modelId: String,
        includes: ((P) -> List<PropertyContainerPath>)
    ): GraphQLRequest<M> {
        return AppSyncGraphQLRequestFactory.buildQuery(modelType, modelId, includes)
    }

    /**
     * Creates a [GraphQLRequest] that represents a query that expects a single value as a result.
     * The request will be created with the correct document based on the model schema and
     * variables based on given `modelIdentifier`.
     * @param modelType the model class.
     * @param modelIdentifier the model identifier.
     * @param <M> the concrete model type.
     * @return a valid [GraphQLRequest] instance.
     </M> */
    @JvmStatic
    operator fun <M : Model> get(
        modelType: Class<M>,
        modelIdentifier: ModelIdentifier<M>
    ): GraphQLRequest<M> {
        return AppSyncGraphQLRequestFactory.buildQuery(modelType, modelIdentifier)
    }

    /**
     * Creates a [GraphQLRequest] that represents a query that expects a single value as a result.
     * The request will be created with the correct document based on the model schema and
     * variables based on given `modelIdentifier`.
     * @param modelType the model class.
     * @param modelIdentifier the model identifier.
     * @param includes lambda returning list of relationships that should be included in the selection set
     * @param <M> the concrete model type.
     * @param <P> the concrete model path for the M model type
     * @return a valid [GraphQLRequest] instance.
     </M> */
    @JvmStatic
    operator fun <M : Model, P : ModelPath<M>> get(
        modelType: Class<M>,
        modelIdentifier: ModelIdentifier<M>,
        includes: ((P) -> List<PropertyContainerPath>)
    ): GraphQLRequest<M> {
        return AppSyncGraphQLRequestFactory.buildQuery(modelType, modelIdentifier, includes)
    }

    /**
     * Creates a [GraphQLRequest] that represents a query that expects multiple values as a result.
     * The request will be created with the correct document based on the model schema and variables
     * for filtering based on the given predicate.
     * @param modelType the model class.
     * @param predicate the predicate for filtering.
     * @param <M> the concrete model type.
     * @return a valid [GraphQLRequest] instance.
     </M> */
    @JvmStatic
    fun <M : Model> list(
        modelType: Class<M>,
        predicate: QueryPredicate
    ): GraphQLRequest<PaginatedResult<M>> {
        return AppSyncGraphQLRequestFactory.buildQuery(modelType, predicate)
    }

    /**
     * Creates a [GraphQLRequest] that represents a query that expects multiple values as a result.
     * The request will be created with the correct document based on the model schema and variables
     * for filtering based on the given predicate.
     * @param modelType the model class.
     * @param predicate the predicate for filtering.
     * @param includes lambda returning list of relationships that should be included in the selection set
     * @param <M> the concrete model type.
     * @param <P> the concrete model path for the M model type
     * @return a valid [GraphQLRequest] instance.
     </M> */
    @JvmStatic
    fun <M : Model, P : ModelPath<M>> list(
        modelType: Class<M>,
        predicate: QueryPredicate,
        includes: ((P) -> List<PropertyContainerPath>)
    ): GraphQLRequest<PaginatedResult<M>> {
        return AppSyncGraphQLRequestFactory.buildQuery(modelType, predicate, includes)
    }

    /**
     * Creates a [GraphQLRequest] that represents a query that expects multiple values as a result.
     * The request will be created with the correct document based on the model schema.
     * @param modelType the model class.
     * @param <M> the concrete model type.
     * @return a valid [GraphQLRequest] instance.
     * @see .list
     </M> */
    @JvmStatic
    fun <M : Model> list(modelType: Class<M>): GraphQLRequest<PaginatedResult<M>> {
        return list(modelType, QueryPredicates.all())
    }

    /**
     * Creates a [GraphQLRequest] that represents a query that expects multiple values as a result.
     * The request will be created with the correct document based on the model schema.
     * @param modelType the model class.
     * @param includes lambda returning list of relationships that should be included in the selection set
     * @param <M> the concrete model type.
     * @param <P> the concrete model path for the M model type
     * @return a valid [GraphQLRequest] instance.
     * @see .list
     </M> */
    @JvmStatic
    fun <M : Model, P : ModelPath<M>> list(
        modelType: Class<M>,
        includes: ((P) -> List<PropertyContainerPath>)
    ): GraphQLRequest<PaginatedResult<M>> {
        return list(modelType, QueryPredicates.all(), includes)
    }

    /**
     * Creates a [GraphQLRequest] that represents a query that expects multiple values as a result
     * within a certain range (i.e. paginated).
     *
     * The request will be created with the correct document based on the model schema and variables
     * for filtering based on the given predicate and pagination.
     *
     * @param modelType the model class.
     * @param predicate the predicate for filtering.
     * @param pagination the pagination settings.
     * @param <M> the concrete model type.
     * @return a valid [GraphQLRequest] instance.
     * @see ModelPagination.firstPage
     </M> */
    @JvmStatic
    fun <M : Model> list(
        modelType: Class<M>,
        predicate: QueryPredicate,
        pagination: ModelPagination
    ): GraphQLRequest<PaginatedResult<M>> {
        return AppSyncGraphQLRequestFactory.buildPaginatedResultQuery(
            modelType, predicate, pagination.limit
        )
    }

    /**
     * Creates a [GraphQLRequest] that represents a query that expects multiple values as a result
     * within a certain range (i.e. paginated).
     *
     * The request will be created with the correct document based on the model schema and variables
     * for filtering based on the given predicate and pagination.
     *
     * @param modelType the model class.
     * @param predicate the predicate for filtering.
     * @param pagination the pagination settings.
     * @param includes lambda returning list of relationships that should be included in the selection set
     * @param <M> the concrete model type.
     * @param <P> the concrete model path for the M model type
     * @return a valid [GraphQLRequest] instance.
     * @see ModelPagination.firstPage
     </M> */
    @JvmStatic
    fun <M : Model, P : ModelPath<M>> list(
        modelType: Class<M>,
        predicate: QueryPredicate,
        pagination: ModelPagination,
        includes: ((P) -> List<PropertyContainerPath>)
    ): GraphQLRequest<PaginatedResult<M>> {
        return AppSyncGraphQLRequestFactory.buildPaginatedResultQuery(
            modelType, predicate, pagination.limit, includes
        )
    }

    /**
     * Creates a [GraphQLRequest] that represents a query that expects multiple values as a result
     * within a certain range (i.e. paginated).
     *
     * The request will be created with the correct document based on the model schema and variables
     * for pagination based on the given [ModelPagination].
     *
     * @param modelType the model class.
     * @param pagination the pagination settings.
     * @param <M> the concrete model type.
     * @return a valid [GraphQLRequest] instance.
     * @see ModelPagination.firstPage
     </M> */
    @JvmStatic
    fun <M : Model> list(
        modelType: Class<M>,
        pagination: ModelPagination
    ): GraphQLRequest<PaginatedResult<M>> {
        return AppSyncGraphQLRequestFactory.buildPaginatedResultQuery(
            modelType, QueryPredicates.all(), pagination.limit
        )
    }

    /**
     * Creates a [GraphQLRequest] that represents a query that expects multiple values as a result
     * within a certain range (i.e. paginated).
     *
     * The request will be created with the correct document based on the model schema and variables
     * for pagination based on the given [ModelPagination].
     *
     * @param modelType the model class.
     * @param pagination the pagination settings.
     * @param includes lambda returning list of relationships that should be included in the selection set
     * @param <M> the concrete model type.
     * @param <P> the concrete model path for the M model type
     * @return a valid [GraphQLRequest] instance.
     * @see ModelPagination.firstPage
     </M> */
    @JvmStatic
    fun <M : Model, P : ModelPath<M>> list(
        modelType: Class<M>,
        pagination: ModelPagination,
        includes: ((P) -> List<PropertyContainerPath>)
    ): GraphQLRequest<PaginatedResult<M>> {
        return AppSyncGraphQLRequestFactory.buildPaginatedResultQuery(
            modelType, QueryPredicates.all(), pagination.limit, includes
        )
    }
}
