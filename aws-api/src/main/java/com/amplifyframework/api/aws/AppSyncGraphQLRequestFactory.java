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
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.BeginsWithQueryOperator;
import com.amplifyframework.core.model.query.predicate.BetweenQueryOperator;
import com.amplifyframework.core.model.query.predicate.ContainsQueryOperator;
import com.amplifyframework.core.model.query.predicate.EqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.GreaterOrEqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.GreaterThanQueryOperator;
import com.amplifyframework.core.model.query.predicate.LessOrEqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.LessThanQueryOperator;
import com.amplifyframework.core.model.query.predicate.NotEqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicateGroup;
import com.amplifyframework.core.model.query.predicate.QueryPredicateOperation;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.util.Casing;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converts provided model or class type into a request container
 * with automatically generated GraphQL documents that follow
 * AppSync specifications.
 */
public final class AppSyncGraphQLRequestFactory {
    private static final int DEFAULT_QUERY_LIMIT = 1000;

    // This class should not be instantiated
    private AppSyncGraphQLRequestFactory() { }

    /**
     * Creates a {@link GraphQLRequest} that represents a query that expects a single value as a result.
     * The request will be created with the correct document based on the model schema and
     * variables based on given {@code objectId}.
     *
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
        try {
            return AppSyncGraphQLRequest.builder()
                    .modelClass(modelClass)
                    .operation(QueryType.GET)
                    .requestOptions(new ApiGraphQLRequestOptions())
                    .responseType(modelClass)
                    .variable("id", "ID!", objectId)
                    .build();
        } catch (AmplifyException exception) {
            throw new IllegalStateException(
                    "Could not generate a schema for the specified class",
                    exception
            );
        }
    }

    /**
     * Creates a {@link GraphQLRequest} that represents a query that expects multiple values as a result.
     * The request will be created with the correct document based on the model schema and variables
     * for filtering based on the the given predicate.
     *
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
     * Creates a {@link GraphQLRequest} that represents a query that expects multiple values as a result
     * within a certain range (i.e. paginated).
     *
     * The request will be created with the correct document based on the model schema and variables
     * for filtering based on the the given predicate and pagination.
     *
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
                builder.variable("filter", filterType, parsePredicate(predicate));
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
     *
     * @param model the model instance.
     * @param predicate the model predicate.
     * @param type the mutation type.
     * @param <R> the response type.
     * @param <T> the concrete model type.
     * @return a valid {@link GraphQLRequest} instance.
     * @throws IllegalStateException when the model schema does not contain the expected information.
     */
    @SuppressWarnings("unchecked")
    public static <R, T extends Model> GraphQLRequest<R> buildMutation(
            T model,
            QueryPredicate predicate,
            MutationType type
    ) {
        try {
            // model is of type T so this is a safe cast - hence the warning suppression
            Class<T> modelClass = (Class<T>) model.getClass();
            ModelSchema schema = ModelSchema.fromModelClass(modelClass);
            String graphQlTypeName = schema.getName();

            AppSyncGraphQLRequest.Builder builder = AppSyncGraphQLRequest.builder()
                    .operation(type)
                    .modelClass(modelClass)
                    .requestOptions(new ApiGraphQLRequestOptions())
                    .responseType(modelClass);

            String inputType = new StringBuilder()
                .append(Casing.capitalize(type.toString()))
                .append(Casing.capitalizeFirst(graphQlTypeName))
                .append("Input!")
                .toString(); // CreateTodoInput

            if (MutationType.DELETE.equals(type)) {
                builder.variable("input", inputType, Collections.singletonMap("id", model.getId()));
            } else {
                builder.variable("input", inputType, schema.getMapOfFieldNameAndValues(model));
            }

            if (!QueryPredicates.all().equals(predicate)) {
                String conditionType = new StringBuilder()
                    .append("Model")
                    .append(Casing.capitalizeFirst(graphQlTypeName))
                    .append("ConditionInput")
                    .toString();
                builder.variable("condition", conditionType, parsePredicate(predicate));
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
     *
     * @param modelClass the model type.
     * @param subscriptionType the subscription type.
     * @param <R> the response type.
     * @param <T> the concrete model type.
     * @return a valid {@link GraphQLRequest} instance.
     * @throws IllegalStateException when the model schema does not contain the expected information.
     */
    @SuppressWarnings("SameParameterValue")
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

    private static Map<String, Object> parsePredicate(QueryPredicate queryPredicate) {
        if (queryPredicate instanceof QueryPredicateOperation) {
            QueryPredicateOperation<?> qpo = (QueryPredicateOperation) queryPredicate;
            QueryOperator<?> op = qpo.operator();
            return Collections.singletonMap(
                    qpo.field(),
                    Collections.singletonMap(appSyncOpType(op.type()), appSyncOpValue(op))
            );
        } else if (queryPredicate instanceof QueryPredicateGroup) {
            QueryPredicateGroup qpg = (QueryPredicateGroup) queryPredicate;

            if (QueryPredicateGroup.Type.NOT.equals(qpg.type())) {
                try {
                    return Collections.singletonMap("not", parsePredicate(qpg.predicates().get(0)));
                } catch (IndexOutOfBoundsException exception) {
                    throw new IllegalStateException(
                        "Predicate group of type NOT must include a value to negate.",
                        exception
                    );
                }
            } else {
                List<Map<String, Object>> predicates = new ArrayList<>();

                for (QueryPredicate predicate : qpg.predicates()) {
                    predicates.add(parsePredicate(predicate));
                }

                return Collections.singletonMap(qpg.type().toString().toLowerCase(Locale.getDefault()), predicates);
            }
        } else {
            throw new IllegalStateException(
                "Invalid predicate type, supported values: QueryPredicateOperation, QueryPredicateGroup."
            );
        }
    }

    private static String appSyncOpType(QueryOperator.Type type) {
        switch (type) {
            case NOT_EQUAL:
                return "ne";
            case EQUAL:
                return "eq";
            case LESS_OR_EQUAL:
                return "le";
            case LESS_THAN:
                return "lt";
            case GREATER_OR_EQUAL:
                return "ge";
            case GREATER_THAN:
                return "gt";
            case CONTAINS:
                return "contains";
            case BETWEEN:
                return "between";
            case BEGINS_WITH:
                return "beginsWith";
            default:
                throw new IllegalStateException(
                    "Tried to parse an unsupported QueryOperator type. Check if a new QueryOperator.Type enum " +
                    "has been created which is not supported in the AppSyncGraphQLRequestFactory."
                );
        }
    }

    private static Object appSyncOpValue(QueryOperator<?> qOp) {
        switch (qOp.type()) {
            case NOT_EQUAL:
                return ((NotEqualQueryOperator) qOp).value();
            case EQUAL:
                return ((EqualQueryOperator) qOp).value();
            case LESS_OR_EQUAL:
                return ((LessOrEqualQueryOperator) qOp).value();
            case LESS_THAN:
                return ((LessThanQueryOperator) qOp).value();
            case GREATER_OR_EQUAL:
                return ((GreaterOrEqualQueryOperator) qOp).value();
            case GREATER_THAN:
                return ((GreaterThanQueryOperator) qOp).value();
            case CONTAINS:
                return ((ContainsQueryOperator) qOp).value();
            case BETWEEN:
                BetweenQueryOperator<?> betweenOp = (BetweenQueryOperator) qOp;
                return Arrays.asList(betweenOp.start(), betweenOp.end());
            case BEGINS_WITH:
                return ((BeginsWithQueryOperator) qOp).value();
            default:
                throw new IllegalStateException(
                    "Tried to parse an unsupported QueryOperator type. Check if a new QueryOperator.Type enum " +
                    "has been created which is not implemented yet."
                );
        }
    }
}
