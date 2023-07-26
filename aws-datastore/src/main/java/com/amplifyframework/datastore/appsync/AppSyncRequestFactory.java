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
import com.amplifyframework.api.aws.AuthModeStrategyType;
import com.amplifyframework.api.aws.GraphQLRequestHelper;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.api.graphql.QueryType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicateGroup;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.Casing;
import com.amplifyframework.util.TypeMaker;

import java.util.HashMap;
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
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");

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
    static <T> AppSyncGraphQLRequest<T> buildSyncRequest(
        @NonNull final ModelSchema modelSchema,
        @Nullable final Long lastSync,
        @Nullable final Integer limit,
        @NonNull final QueryPredicate predicate)
        throws DataStoreException {
        return buildSyncRequest(modelSchema, lastSync, limit, predicate, AuthModeStrategyType.DEFAULT);
    }

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
    static <T> AppSyncGraphQLRequest<T> buildSyncRequest(
            @NonNull final ModelSchema modelSchema,
            @Nullable final Long lastSync,
            @Nullable final Integer limit,
            @NonNull final QueryPredicate predicate,
            @NonNull final AuthModeStrategyType strategyType)
            throws DataStoreException {
        try {
            AppSyncGraphQLRequest.Builder builder = AppSyncGraphQLRequest.builder()
                    .modelClass(modelSchema.getModelClass())
                    .modelSchema(modelSchema)
                    .operation(QueryType.SYNC)
                    .requestAuthorizationStrategyType(strategyType)
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
            if (!QueryPredicates.all().equals(predicate)) {
                String filterType = "Model" + Casing.capitalizeFirst(modelSchema.getName()) + "FilterInput";
                QueryPredicate syncPredicate = predicate;
                if (!(syncPredicate instanceof QueryPredicateGroup)) {
                    // When a filter is provided, wrap it with a predicate group of type AND.  By doing this, it enables
                    // AppSync to optimize the request by performing a DynamoDB query instead of a scan.  If the
                    // provided syncPredicate is already a QueryPredicateGroup, this is not needed.  If the provided
                    // group is of type AND, the optimization will occur.  If the top level group is OR or NOT, the
                    // optimization is not possible anyway.
                    syncPredicate = QueryPredicateGroup.andOf(syncPredicate);
                }
                builder.variable(
                        "filter",
                        filterType,
                        GraphQLRequestHelper.parsePredicate(syncPredicate)
                );
            }
            return builder.build();
        } catch (AmplifyException amplifyException) {
            throw new DataStoreException("Failed to get fields for model.",
                    amplifyException, "Validate your model file.");
        }
    }

    static <T> AppSyncGraphQLRequest<T>
        buildSubscriptionRequest(ModelSchema modelSchema,
                                 SubscriptionType subscriptionType,
                                 AuthModeStrategyType strategyType) throws DataStoreException {
        try {
            return AppSyncGraphQLRequest.builder()
                    .modelClass(modelSchema.getModelClass())
                    .modelSchema(modelSchema)
                    .operation(subscriptionType)
                    .requestOptions(new DataStoreGraphQLRequestOptions())
                    .requestAuthorizationStrategyType(strategyType)
                    .responseType(TypeMaker.getParameterizedType(ModelWithMetadata.class, modelSchema.getModelClass()))
                    .build();
        } catch (AmplifyException amplifyException) {
            throw new DataStoreException("Failed to get fields for model.",
                    amplifyException, "Validate your model file.");
        }
    }

    static <M extends Model> AppSyncGraphQLRequest<ModelWithMetadata<M>> buildDeletionRequest(
            ModelSchema schema,
            M model,
            Integer version,
            QueryPredicate predicate,
            AuthModeStrategyType strategyType)
            throws DataStoreException {
        try {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("_version", version);
            inputMap.putAll(GraphQLRequestHelper.getDeleteMutationInputMap(schema, model));
            return buildMutation(schema, inputMap, predicate, MutationType.DELETE, strategyType);
        } catch (AmplifyException amplifyException) {
            throw new DataStoreException("Failed to get fields for model.",
                    amplifyException, "Validate your model file.");
        }
    }

    static <M extends Model> AppSyncGraphQLRequest<ModelWithMetadata<M>> buildUpdateRequest(
            ModelSchema schema,
            M model,
            Integer version,
            QueryPredicate predicate,
            AuthModeStrategyType strategyType) throws DataStoreException {
        try {
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("_version", version);
            inputMap.putAll(GraphQLRequestHelper.getMapOfFieldNameAndValues(schema, model, MutationType.UPDATE));
            return buildMutation(schema, inputMap, predicate, MutationType.UPDATE, strategyType);
        } catch (AmplifyException amplifyException) {
            throw new DataStoreException("Failed to get fields for model.",
                    amplifyException, "Validate your model file.");
        }
    }

    static <M extends Model> AppSyncGraphQLRequest<ModelWithMetadata<M>> buildCreationRequest(
            ModelSchema schema,
            M model,
            AuthModeStrategyType strategyType) throws DataStoreException {
        try {
            Map<String, Object> inputMap =
                    GraphQLRequestHelper.getMapOfFieldNameAndValues(
                            schema,
                            model,
                            MutationType.CREATE
                    );
            return buildMutation(schema, inputMap, QueryPredicates.all(), MutationType.CREATE, strategyType);
        } catch (AmplifyException amplifyException) {
            throw new DataStoreException("Failed to get fields for model.",
                    amplifyException, "Validate your model file.");
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
            MutationType mutationType,
            AuthModeStrategyType strategyType)
            throws DataStoreException {
        try {
            String graphQlTypeName = schema.getName();
            AppSyncGraphQLRequest.Builder builder = AppSyncGraphQLRequest.builder()
                    .modelClass(schema.getModelClass())
                    .modelSchema(schema)
                    .operation(mutationType)
                    .requestAuthorizationStrategyType(strategyType)
                    .requestOptions(new DataStoreGraphQLRequestOptions())
                    .responseType(TypeMaker.getParameterizedType(ModelWithMetadata.class, schema.getModelClass()));

            String inputType =
                    Casing.capitalize(mutationType.toString()) +
                    Casing.capitalizeFirst(graphQlTypeName) +
                    "Input!"; // CreateTodoInput

            builder.variable("input", inputType, inputMap);

            if (!QueryPredicates.all().equals(predicate)) {
                String conditionType =
                        "Model" +
                        Casing.capitalizeFirst(graphQlTypeName) +
                        "ConditionInput";
                builder.variable(
                        "condition",
                        conditionType,
                        GraphQLRequestHelper.parsePredicate(predicate)
                );
            }
            return builder.build();

        } catch (AmplifyException amplifyException) {
            throw new DataStoreException("Failed to get fields for model.",
                    amplifyException, "Validate your model file.");
        }
    }
}
