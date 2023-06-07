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

package com.amplifyframework.api.aws;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.api.graphql.QueryType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelIdentifier;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.util.Casing;
import com.amplifyframework.util.TypeMaker;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

/**
 * Converts provided model or class type into a request container with automatically generated GraphQL documents that
 * follow AppSync specifications.
 */
public final class AppSyncGraphQLRequestFactory {
    private static final int DEFAULT_QUERY_LIMIT = 1000;

    // This class should not be instantiated
    private AppSyncGraphQLRequestFactory() {
    }

    /**
     * Creates a {@link GraphQLRequest} that represents a query that expects a single value as a result. The request
     * will be created with the correct document based on the model schema and variables based on given
     * {@code objectId}.
     * @param modelClass the model class.
     * @param objectId the model identifier.
     * @param <R> the response type.
     * @param <T> the concrete model type.
     * @return a valid {@link GraphQLRequest} instance.
     * @throws IllegalStateException when the model schema does not contain the expected information.
     */
    public static <R, T extends Model> GraphQLRequest<R> buildQuery(
        Class<T> modelClass,
        String objectId
    ) {
        return buildQuery(modelClass, new GraphQLRequestVariable("id", objectId, "ID!"));
    }

    /**
     * Creates a {@link GraphQLRequest} that represents a query that expects a single value as a result. The request
     * will be created with the correct document based on the model schema and variables based on given
     * {@code modelIdentifier}.
     * @param modelClass the model class.
     * @param modelIdentifier the model identifier.
     * @param <R> the response type.
     * @param <T> the concrete model type.
     * @return a valid {@link GraphQLRequest} instance.
     * @throws IllegalStateException when the model schema does not contain the expected information.
     */
    public static <R, T extends Model> GraphQLRequest<R> buildQuery(
            Class<T> modelClass,
            ModelIdentifier<T> modelIdentifier
    ) {
        GraphQLRequestVariable[] variables;
        try {
            ModelSchema modelSchema = ModelSchema.fromModelClass(modelClass);
            List<String> primaryIndexFields = modelSchema.getPrimaryIndexFields();
            List<? extends Serializable> sortedKeys = modelIdentifier.sortedKeys();

            variables = new GraphQLRequestVariable[primaryIndexFields.size()];

            for (int i = 0; i < primaryIndexFields.size(); i++) {

                // Index 0 is primary key, next values are ordered sort keys
                String key = primaryIndexFields.get(i);

                // Find target field to pull type info
                ModelField targetField =
                        Objects.requireNonNull(modelSchema.getFields().get(key));

                // Should create "ID!", "String!", "Float!", etc.
                // Appends "!" if required (should always be the case with CPK requirements).
                String targetTypeString = targetField.getTargetType() +
                        (targetField.isRequired() ? "!" : "");

                // If index 0, value is primary key, else get next unused sort key
                Object value = i == 0 ?
                        modelIdentifier.key().toString() : sortedKeys.get(i - 1);
                variables[i] = new GraphQLRequestVariable(key, value, targetTypeString);
            }
        } catch (AmplifyException exception) {
            throw new IllegalStateException(
                    "Could not generate a schema for the specified class",
                    exception
            );
        }

        return buildQuery(modelClass, variables);
    }

    /**
     * Creates a {@link GraphQLRequest} that represents a query that expects a single value as a result. The request
     * will be created with the correct document based on the model schema and variables.
     * @param modelClass the model class.
     * @param variables the variables.
     * @param <R> the response type.
     * @param <T> the concrete model type.
     * @return a valid {@link GraphQLRequest} instance.
     * @throws IllegalStateException when the model schema does not contain the expected information.
     */
    private static <R, T extends Model> GraphQLRequest<R> buildQuery(
            Class<T> modelClass,
            GraphQLRequestVariable... variables
    ) {
        try {
            AppSyncGraphQLRequest.Builder builder = AppSyncGraphQLRequest.builder()
                    .modelClass(modelClass)
                    .operation(QueryType.GET)
                    .requestOptions(new ApiGraphQLRequestOptions())
                    .responseType(modelClass);

            for (GraphQLRequestVariable v : variables) {
                builder.variable(v.getKey(), v.getType(), v.getValue());
            }
            return builder.build();
        } catch (AmplifyException exception) {
            throw new IllegalStateException(
                    "Could not generate a schema for the specified class",
                    exception
            );
        }
    }

    /**
     * Creates a {@link GraphQLRequest} that represents a query that expects multiple values as a result. The request
     * will be created with the correct document based on the model schema and variables for filtering based on the
     * given predicate.
     * @param modelClass the model class.
     * @param predicate the model predicate.
     * @param <R> the response type.
     * @param <T> the concrete model type.
     * @return a valid {@link GraphQLRequest} instance.
     * @throws IllegalStateException when the model schema does not contain the expected information.
     */
    public static <R, T extends Model> GraphQLRequest<R> buildQuery(
        Class<T> modelClass,
        QueryPredicate predicate
    ) {
        Type dataType = TypeMaker.getParameterizedType(PaginatedResult.class, modelClass);
        return buildQuery(modelClass, predicate, DEFAULT_QUERY_LIMIT, dataType);
    }

    /**
     * Creates a {@link GraphQLRequest} that represents a query that expects multiple values as a result within a
     * certain range (i.e. paginated).
     * <p>
     * The request will be created with the correct document based on the model schema and variables for filtering based
     * on the given predicate and pagination.
     * @param modelClass the model class.
     * @param predicate the predicate for filtering.
     * @param limit the page size/limit.
     * @param <R> the response type.
     * @param <T> the concrete model type.
     * @return a valid {@link GraphQLRequest} instance.
     */
    public static <R, T extends Model> GraphQLRequest<R> buildPaginatedResultQuery(
        Class<T> modelClass,
        QueryPredicate predicate,
        int limit
    ) {
        Type responseType = TypeMaker.getParameterizedType(PaginatedResult.class, modelClass);
        return buildQuery(modelClass, predicate, limit, responseType);
    }

    static <R, T extends Model> GraphQLRequest<R> buildQuery(
        Class<T> modelClass,
        QueryPredicate predicate,
        int limit,
        Type responseType
    ) {
        try {
            String modelName = ModelSchema.fromModelClass(modelClass).getName();
            AppSyncGraphQLRequest.Builder builder = AppSyncGraphQLRequest.builder()
                                                        .modelClass(modelClass)
                                                        .operation(QueryType.LIST)
                                                        .requestOptions(new ApiGraphQLRequestOptions())
                                                        .responseType(responseType);

            if (!QueryPredicates.all().equals(predicate)) {
                String filterType = "Model" + Casing.capitalizeFirst(modelName) + "FilterInput";
                builder.variable(
                        "filter",
                        filterType,
                        GraphQLRequestHelper.parsePredicate(predicate)
                );
            }

            builder.variable("limit", "Int", limit);
            return builder.build();
        } catch (AmplifyException exception) {
            throw new IllegalStateException(
                "Could not generate a schema for the specified class",
                exception
            );
        }
    }

    /**
     * Creates a {@link GraphQLRequest} that represents a mutation of a given type.
     * @param model the model instance.
     * @param predicate the model predicate.
     * @param type the mutation type.
     * @param <R> the response type.
     * @param <T> the concrete model type.
     * @return a valid {@link GraphQLRequest} instance.
     * @throws IllegalStateException when the model schema does not contain the expected information.
     */
    public static <R, T extends Model> GraphQLRequest<R> buildMutation(
        T model,
        QueryPredicate predicate,
        MutationType type
    ) {
        try {
            Class<? extends Model> modelClass = model.getClass();
            ModelSchema schema = ModelSchema.fromModelClass(modelClass);
            String graphQlTypeName = schema.getName();

            AppSyncGraphQLRequest.Builder builder = AppSyncGraphQLRequest.builder()
                                                        .operation(type)
                                                        .modelClass(modelClass)
                                                        .requestOptions(new ApiGraphQLRequestOptions())
                                                        .responseType(modelClass);

            String inputType =
                Casing.capitalize(type.toString()) +
                    Casing.capitalizeFirst(graphQlTypeName) +
                    "Input!"; // CreateTodoInput

            if (MutationType.DELETE.equals(type)) {
                builder.variable(
                        "input",
                        inputType,
                        GraphQLRequestHelper.getDeleteMutationInputMap(schema, model)
                );
            } else {
                builder.variable(
                        "input",
                        inputType,
                        GraphQLRequestHelper.getMapOfFieldNameAndValues(schema, model)
                );
            }

            if (!QueryPredicates.all().equals(predicate)) {
                String conditionType =
                    "Model" +
                        Casing.capitalizeFirst(graphQlTypeName) +
                        "ConditionInput";
                builder.variable(
                        "condition", conditionType, GraphQLRequestHelper.parsePredicate(predicate));
            }

            return builder.build();
        } catch (AmplifyException exception) {
            throw new IllegalStateException(
                "Could not generate a schema for the specified class",
                exception
            );
        }
    }

    /**
     * Creates a {@link GraphQLRequest} that represents a subscription of a given type.
     * @param modelClass the model type.
     * @param subscriptionType the subscription type.
     * @param <R> the response type.
     * @param <T> the concrete model type.
     * @return a valid {@link GraphQLRequest} instance.
     * @throws IllegalStateException when the model schema does not contain the expected information.
     */
    public static <R, T extends Model> GraphQLRequest<R> buildSubscription(
        Class<T> modelClass,
        SubscriptionType subscriptionType
    ) {
        try {
            return AppSyncGraphQLRequest.builder()
                       .modelClass(modelClass)
                       .operation(subscriptionType)
                       .requestOptions(new ApiGraphQLRequestOptions())
                       .responseType(modelClass)
                       .build();
        } catch (AmplifyException exception) {
            throw new IllegalStateException(
                "Failed to build GraphQLRequest",
                exception
            );
        }
    }
}
