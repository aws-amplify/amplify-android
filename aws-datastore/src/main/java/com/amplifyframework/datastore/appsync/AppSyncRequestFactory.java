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

package com.amplifyframework.datastore.appsync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.aws.AppSyncGraphQLRequest;
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
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.util.Casing;
import com.amplifyframework.util.TypeMaker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A factory to generate requests against an AppSync endpoint.
 *
 * This is an implementation detail of the {@link AppSyncClient}.
 *
 * AppSync requests are raw GraphQL document strings, which contain AppSync-specific details,
 * such as AppSync mutation names (create, update, delete, and associated subscription names),
 * and AppSync-specific field names (`_version`, `_deleted`, etc.)
 */
final class AppSyncRequestFactory {

    private AppSyncRequestFactory() {}

    /**
     * Builds the query document for base and delta sync.
     * If you provide lastSyncTime, it builds a delta sync, where the delta is computed
     * against the provided time. Otherwise, if you provide a null lastSyncTime, a
     * request doc is generated for a base sync.
     * @param modelSchema Schema Class for which we want to sync.
     * @param lastSync The last time synced. If not provided, do a base query.
     *                 If provided, do a delta query.
     * @param <T> The type of objects we are syncing
     * @return A string which contains a GraphQL query doc for an base/delta sync
     * @throws DataStoreException On Failure to inspect
     */
    @NonNull
    static <T, M extends Model> AppSyncGraphQLRequest<T> buildSyncRequest(
            @NonNull final ModelSchema modelSchema,
            @Nullable final Long lastSync,
            @Nullable final Integer limit)
            throws DataStoreException {

        try {
            AppSyncGraphQLRequest.Builder builder = AppSyncGraphQLRequest.builder()
                    .modelClass(modelSchema.getModelClass())
                    .modelSchema(modelSchema)
                    .operation(QueryType.SYNC)
                    .requestOptions(new DataStoreGraphQLRequestOptions())
                    .responseType(
                            TypeMaker.getParameterizedType(
                                    PaginatedResult.class,
                                    ModelWithMetadata.class,
                                    modelSchema.getModelClass()));
            if (lastSync != null) {
                builder.variable("lastSync", "AWSTimestamp", lastSync);
            }
            if (limit != null) {
                builder.variable("limit", "Int", limit);
            }

            return builder.build();
        } catch (AmplifyException amplifyException) {
            throw new DataStoreException("Failed to get fields for model.",
                    amplifyException, "Validate your model file.");
        }
    }

    static <T, M extends Model> AppSyncGraphQLRequest<T> buildSubscriptionRequest(
            ModelSchema modelSchema,
            SubscriptionType subscriptionType) throws DataStoreException {
        try {
            return AppSyncGraphQLRequest.builder()
                    .modelClass(modelSchema.getModelClass())
                    .modelSchema(modelSchema)
                    .operation(subscriptionType)
                    .requestOptions(new DataStoreGraphQLRequestOptions())
                    .responseType(TypeMaker.getParameterizedType(ModelWithMetadata.class, modelSchema.getModelClass()))
                    .build();
        } catch (AmplifyException amplifyException) {
            throw new DataStoreException("Failed to get fields for model.",
                    amplifyException, "Validate your model file.");
        }
    }

    static <M extends Model> AppSyncGraphQLRequest<ModelWithMetadata<M>> buildDeletionRequest(
            ModelSchema schema, String objectId, Integer version, QueryPredicate predicate) throws DataStoreException {
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("id", objectId);
        inputMap.put("_version", version);
        return buildMutation(schema, inputMap, predicate, MutationType.DELETE);
    }

    @SuppressWarnings("unchecked") // cast to (Class<M>)
    static <M extends Model> AppSyncGraphQLRequest<ModelWithMetadata<M>> buildUpdateRequest(
            M model, Integer version, QueryPredicate predicate) throws DataStoreException {
        Class<M> modelClass = (Class<M>) model.getClass();
        Map<String, Object> inputMap = new HashMap<>();
        try {
            ModelSchema schema = ModelSchema.fromModelClass(modelClass);
            inputMap.putAll(schema.getMapOfFieldNameAndValues(model));
            inputMap.put("_version", version);
            return buildMutation(schema, inputMap, predicate, MutationType.UPDATE);

        } catch (AmplifyException amplifyException) {
            throw new DataStoreException("Failed to get fields for model.",
                    amplifyException, "Validate your model file.");
        }
    }

    @SuppressWarnings("unchecked") // cast to (Class<M>)
    static <M extends Model> AppSyncGraphQLRequest<ModelWithMetadata<M>> buildCreationRequest(
            M model, ModelSchema modelSchema)
            throws DataStoreException {
        try {
            Map<String, Object> inputMap = modelSchema.getMapOfFieldNameAndValues(model);
            return buildMutation(modelSchema, inputMap, QueryPredicates.all(), MutationType.CREATE);
        } catch (AmplifyException amplifyException) {
            throw new DataStoreException("Failed to get fields for model.",
                    amplifyException, "Validate your model file.");
        }

    }

    static Map<String, Object> parsePredicate(QueryPredicate queryPredicate) throws DataStoreException {
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
                    throw new DataStoreException(
                        "Predicate group of type NOT must include a value to negate.",
                        exception,
                        "Check if you created a NOT condition in your Predicate with no included value."
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
            throw new DataStoreException(
                "Tried to parse an unsupported QueryPredicate",
                "Try changing to one of the supported values: QueryPredicateOperation, QueryPredicateGroup."
            );
        }
    }

    private static String appSyncOpType(QueryOperator.Type type) throws DataStoreException {
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
                throw new DataStoreException(
                    "Tried to parse an unsupported QueryOperator type",
                    "Check if a new QueryOperator.Type enum has been created which is not supported " +
                        "in the AppSyncGraphQLRequestFactory."
                );
        }
    }

    private static Object appSyncOpValue(QueryOperator<?> qOp) throws DataStoreException {
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
                throw new DataStoreException(
                    "Tried to parse an unsupported QueryOperator type",
                    "Check if a new QueryOperator.Type enum has been created which is not supported " +
                        "in the AppSyncGraphQLRequestFactory."
                );
        }
    }

    /**
     * Builds a mutation.
     * @param schema the model schema for the mutation
     * @param mutationType Type of mutation, e.g. {@link MutationType#CREATE}
     * @param <M> Type of model being mutated
     * @return Mutation doc
     */
    private static <M extends Model> AppSyncGraphQLRequest<ModelWithMetadata<M>> buildMutation(
                                                               ModelSchema schema,
                                                               Map<String, Object> inputMap,
                                                               QueryPredicate predicate,
                                                               MutationType mutationType) throws DataStoreException {
        try {
            String graphQlTypeName = schema.getName();
            AppSyncGraphQLRequest.Builder builder = AppSyncGraphQLRequest.builder()
                    .modelClass(schema.getModelClass())
                    .modelSchema(schema)
                    .operation(mutationType)
                    .requestOptions(new DataStoreGraphQLRequestOptions())
                    .responseType(TypeMaker.getParameterizedType(ModelWithMetadata.class, schema.getModelClass()));

            String inputType = new StringBuilder()
                    .append(Casing.capitalize(mutationType.toString()))
                    .append(Casing.capitalizeFirst(graphQlTypeName))
                    .append("Input!")
                    .toString(); // CreateTodoInput

            builder.variable("input", inputType, inputMap);

            if (!QueryPredicates.all().equals(predicate)) {
                String conditionType = new StringBuilder()
                        .append("Model")
                        .append(Casing.capitalizeFirst(graphQlTypeName))
                        .append("ConditionInput")
                        .toString();
                builder.variable("condition", conditionType, parsePredicate(predicate));
            }
            return builder.build();

        } catch (AmplifyException amplifyException) {
            throw new DataStoreException("Failed to get fields for model.",
                    amplifyException, "Validate your model file.");
        }
    }
}
