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
package com.amplifyframework.api.aws

import com.amplifyframework.AmplifyException
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.MutationType
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.api.graphql.QueryType
import com.amplifyframework.api.graphql.SubscriptionType
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelIdentifier
import com.amplifyframework.core.model.ModelPath
import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.core.model.PropertyContainerPath
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import com.amplifyframework.util.Casing
import com.amplifyframework.util.TypeMaker
import java.lang.reflect.Type

/**
 * Converts provided model or class type into a request container with automatically generated GraphQL documents that
 * follow AppSync specifications.
 */
object AppSyncGraphQLRequestFactory {
    private const val DEFAULT_QUERY_LIMIT = 1000

    /**
     * Creates a [GraphQLRequest] that represents a query that expects a single value as a result. The request
     * will be created with the correct document based on the model schema and variables based on given
     * `objectId`.
     * @param modelClass the model class.
     * @param objectId the model identifier.
     * @param <R> the response type.
     * @param <T> the concrete model type.
     * @param <P> the model's ModelPath type.
     * @return a valid [GraphQLRequest] instance.
     * @throws IllegalStateException when the model schema does not contain the expected information.
    </T></R> */
    @JvmOverloads
    @JvmStatic
    fun <R, T : Model, P : ModelPath<T>> buildQuery(
        modelClass: Class<T>,
        objectId: String,
        includes: ((P) -> List<PropertyContainerPath>)? = null
    ): GraphQLRequest<R> {
        val variable: GraphQLRequestVariable = try {
            val modelSchema = ModelSchema.fromModelClass(modelClass)
            val primaryKeyName = modelSchema.primaryKeyName
            // Find target field to pull type info
            val targetField = requireNotNull(modelSchema.fields[primaryKeyName])
            val requiredSuffix = if (targetField.isRequired) "!" else ""
            val targetTypeString = "${targetField.targetType}$requiredSuffix"
            GraphQLRequestVariable(primaryKeyName, objectId, targetTypeString)
        } catch (exception: Exception) {
            // If we fail to pull primary key name and type, fallback to default id/ID!
            GraphQLRequestVariable("id", objectId, "ID!")
        }
        return buildQuery(modelClass, includes, variable)
    }

    /**
     * Creates a [GraphQLRequest] that represents a query that expects a single value as a result. The request
     * will be created with the correct document based on the model schema and variables based on given
     * `modelIdentifier`.
     * @param modelClass the model class.
     * @param modelIdentifier the model identifier.
     * @param <R> the response type.
     * @param <T> the concrete model type.
     * @return a valid [GraphQLRequest] instance.
     * @throws IllegalStateException when the model schema does not contain the expected information.
    </T></R> */
    @JvmStatic
    @JvmOverloads
    fun <R, T : Model, P : ModelPath<T>> buildQuery(
        modelClass: Class<T>,
        modelIdentifier: ModelIdentifier<T>,
        includes: ((P) -> List<PropertyContainerPath>)? = null
    ): GraphQLRequest<R> {
        try {
            val modelSchema = ModelSchema.fromModelClass(modelClass)
            val primaryIndexFields = modelSchema.primaryIndexFields
            val sortedKeys = modelIdentifier.sortedKeys()
            val variables = primaryIndexFields.mapIndexed { i, key ->
                // Find target field to pull type info
                val targetField = requireNotNull(modelSchema.fields[key])
                val requiredSuffix = if (targetField.isRequired) "!" else ""
                val targetTypeString = "${targetField.targetType}$requiredSuffix"

                // If index 0, value is primary key, else get next unused sort key
                val value = if (i == 0) {
                    modelIdentifier.key().toString()
                } else {
                    sortedKeys[i-1]
                }

                GraphQLRequestVariable(key, value, targetTypeString)
            }
            return buildQuery(modelClass, includes, *variables.toTypedArray())
        } catch (exception: AmplifyException) {
            throw IllegalStateException(
                "Could not generate a schema for the specified class",
                exception
            )
        }
    }

    /**
     * Creates a [GraphQLRequest] that represents a query that expects a single value as a result. The request
     * will be created with the correct document based on the model schema and variables.
     * @param modelClass the model class.
     * @param variables the variables.
     * @param <R> the response type.
     * @param <T> the concrete model type.
     * @return a valid [GraphQLRequest] instance.
     * @throws IllegalStateException when the model schema does not contain the expected information.
    </T></R> */
    private fun <R, T : Model, P : ModelPath<T>> buildQuery(
        modelClass: Class<T>,
        includes: ((P) -> List<PropertyContainerPath>)?,
        vararg variables: GraphQLRequestVariable
    ): GraphQLRequest<R> {
        return try {
            val builder = AppSyncGraphQLRequest.builder()
                .modelClass(modelClass)
                .operation(QueryType.GET)
                .requestOptions(ApiGraphQLRequestOptions())
                .responseType(modelClass)
            for ((key, value, type) in variables) {
                builder.variable(key, type, value)
            }
            includes?.invoke(ModelPath.getRootPath(modelClass))?.let { associations ->
                val selectionSet = SelectionSet.builder()
                    .modelClass(modelClass)
                    .operation(QueryType.GET)
                    .requestOptions(ApiGraphQLRequestOptions())
                    .includeAssociations(associations)
                    .build()

                builder.selectionSet(selectionSet)
            }
            builder.build()
        } catch (exception: AmplifyException) {
            throw IllegalStateException(
                "Could not generate a schema for the specified class",
                exception
            )
        }
    }

    /**
     * Creates a [GraphQLRequest] that represents a query that expects multiple values as a result. The request
     * will be created with the correct document based on the model schema and variables for filtering based on the
     * given predicate.
     * @param modelClass the model class.
     * @param predicate the model predicate.
     * @param <R> the response type.
     * @param <T> the concrete model type.
     * @return a valid [GraphQLRequest] instance.
     * @throws IllegalStateException when the model schema does not contain the expected information.
    </T></R> */
    @JvmStatic
    fun <R, T : Model> buildQuery(
        modelClass: Class<T>,
        predicate: QueryPredicate
    ): GraphQLRequest<R> {
        val dataType = TypeMaker.getParameterizedType(PaginatedResult::class.java, modelClass)
        return buildQuery(modelClass, predicate, DEFAULT_QUERY_LIMIT, dataType)
    }

    /**
     * Creates a [GraphQLRequest] that represents a query that expects multiple values as a result within a
     * certain range (i.e. paginated).
     *
     *
     * The request will be created with the correct document based on the model schema and variables for filtering based
     * on the given predicate and pagination.
     * @param modelClass the model class.
     * @param predicate the predicate for filtering.
     * @param limit the page size/limit.
     * @param <R> the response type.
     * @param <T> the concrete model type.
     * @return a valid [GraphQLRequest] instance.
    </T></R> */
    @JvmStatic
    fun <R, T : Model> buildPaginatedResultQuery(
        modelClass: Class<T>,
        predicate: QueryPredicate,
        limit: Int
    ): GraphQLRequest<R> {
        val responseType = TypeMaker.getParameterizedType(PaginatedResult::class.java, modelClass)
        return buildQuery(modelClass, predicate, limit, responseType)
    }

    @JvmStatic
    fun <R, T : Model> buildQuery(
        modelClass: Class<T>,
        predicate: QueryPredicate,
        limit: Int,
        responseType: Type
    ): GraphQLRequest<R> {
        return try {
            val modelName = ModelSchema.fromModelClass(
                modelClass
            ).name
            val builder = AppSyncGraphQLRequest.builder()
                .modelClass(modelClass)
                .operation(QueryType.LIST)
                .requestOptions(ApiGraphQLRequestOptions())
                .responseType(responseType)
            if (QueryPredicates.all() != predicate) {
                val filterType = "Model" + Casing.capitalizeFirst(modelName) + "FilterInput"
                builder.variable(
                    "filter",
                    filterType,
                    GraphQLRequestHelper.parsePredicate(predicate)
                )
            }
            builder.variable("limit", "Int", limit)
            builder.build()
        } catch (exception: AmplifyException) {
            throw IllegalStateException(
                "Could not generate a schema for the specified class",
                exception
            )
        }
    }

    /**
     * Creates a [GraphQLRequest] that represents a mutation of a given type.
     * @param model the model instance.
     * @param predicate the model predicate.
     * @param type the mutation type.
     * @param <R> the response type.
     * @param <T> the concrete model type.
     * @return a valid [GraphQLRequest] instance.
     * @throws IllegalStateException when the model schema does not contain the expected information.
    </T></R> */
    @JvmStatic
    fun <R, T : Model> buildMutation(
        model: T,
        predicate: QueryPredicate,
        type: MutationType
    ): GraphQLRequest<R> {
        return try {
            val modelClass: Class<out Model> = model.javaClass
            val schema = ModelSchema.fromModelClass(modelClass)
            val graphQlTypeName = schema.name
            val builder = AppSyncGraphQLRequest.builder()
                .operation(type)
                .modelClass(modelClass)
                .requestOptions(ApiGraphQLRequestOptions())
                .responseType(modelClass)
            val inputType = Casing.capitalize(type.toString()) +
                    Casing.capitalizeFirst(graphQlTypeName) +
                    "Input!" // CreateTodoInput
            if (MutationType.DELETE == type) {
                builder.variable(
                    "input",
                    inputType,
                    GraphQLRequestHelper.getDeleteMutationInputMap(schema, model)
                )
            } else {
                builder.variable(
                    "input",
                    inputType,
                    GraphQLRequestHelper.getMapOfFieldNameAndValues(schema, model, type)
                )
            }
            if (QueryPredicates.all() != predicate) {
                val conditionType = "Model" +
                        Casing.capitalizeFirst(graphQlTypeName) +
                        "ConditionInput"
                builder.variable(
                    "condition", conditionType, GraphQLRequestHelper.parsePredicate(predicate)
                )
            }
            builder.build()
        } catch (exception: AmplifyException) {
            throw IllegalStateException(
                "Could not generate a schema for the specified class",
                exception
            )
        }
    }

    /**
     * Creates a [GraphQLRequest] that represents a subscription of a given type.
     * @param modelClass the model type.
     * @param subscriptionType the subscription type.
     * @param <R> the response type.
     * @param <T> the concrete model type.
     * @return a valid [GraphQLRequest] instance.
     * @throws IllegalStateException when the model schema does not contain the expected information.
    </T></R> */
    @JvmStatic
    fun <R, T : Model> buildSubscription(
        modelClass: Class<T>,
        subscriptionType: SubscriptionType
    ): GraphQLRequest<R> {
        return try {
            AppSyncGraphQLRequest.builder()
                .modelClass(modelClass)
                .operation(subscriptionType)
                .requestOptions(ApiGraphQLRequestOptions())
                .responseType(modelClass)
                .build()
        } catch (exception: AmplifyException) {
            throw IllegalStateException(
                "Failed to build GraphQLRequest",
                exception
            )
        }
    }
}