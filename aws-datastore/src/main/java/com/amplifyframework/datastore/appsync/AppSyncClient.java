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
import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.AuthModeStrategyType;
import com.amplifyframework.api.graphql.GraphQLBehavior;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.async.NoOpCancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.logging.Logger;

/**
 * An implementation of the {@link AppSync} client interface.
 *
 * This implementation adds business rules on top of the generic GraphQL behaviors in the
 * {@link ApiCategoryBehavior}.
 *
 * AppSync requests are formed as raw GraphQL document strings, using the
 * {@link AppSyncRequestFactory}. The requests are not generic GraphQL, and contain
 * AppSync-specific details like protocol-specific operation names (create, update, delete),
 * assumptions about the structure of data types (unique IDs, versioning information), etc.
 */
public final class AppSyncClient implements AppSync {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private final GraphQLBehavior api;
    private final AuthModeStrategyType authModeStrategyType;

    /**
     * Constructs a new AppSyncClient.
     * @param api The API Category, configured with a DataStore API
     */
    private AppSyncClient(GraphQLBehavior api,
                          AuthModeStrategyType strategyType) {
        this.api = api;
        this.authModeStrategyType = strategyType;
    }

    /**
     * Obtain an instance of the AppSyncAPI, which uses the Amplify API category
     * as its backing implementation for GraphQL behaviors.
     * @param api GraphQL api behavior through which this app sync client will talk
     * @return An App Sync API instance
     */
    @NonNull
    public static AppSyncClient via(@NonNull GraphQLBehavior api) {
        return new AppSyncClient(api, AuthModeStrategyType.DEFAULT);
    }

    /**
     * Obtain an instance of the AppSyncAPI, which uses the Amplify API category
     * as its backing implementation for GraphQL behaviors.
     * @param api GraphQL api behavior through which this app sync client will talk
     * @param strategyType Authorization strategy that should be used when creating
     *                     GraphQL requests for AppSync.
     * @return An App Sync API instance
     */
    public static AppSyncClient via(@NonNull GraphQLBehavior api,
                                    @NonNull AuthModeStrategyType strategyType) {
        return new AppSyncClient(api, strategyType);
    }

    @NonNull
    @Override
    public <T extends Model> GraphQLRequest<PaginatedResult<ModelWithMetadata<T>>> buildSyncRequest(
            @NonNull ModelSchema modelSchema,
            @Nullable Long lastSync,
            @Nullable Integer syncPageSize,
            @NonNull QueryPredicate queryPredicate) throws DataStoreException {
        return AppSyncRequestFactory.buildSyncRequest(modelSchema,
                                                      lastSync,
                                                      syncPageSize,
                                                      queryPredicate,
                                                      authModeStrategyType);
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable sync(
            @NonNull GraphQLRequest<PaginatedResult<ModelWithMetadata<T>>> request,
            @NonNull Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<T>>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure
    ) {
        final Consumer<ApiException> failureConsumer =
            failure -> onFailure.accept(new DataStoreException(
                        "Failure performing sync query to AppSync.",
                        failure, AmplifyException.TODO_RECOVERY_SUGGESTION));

        final Cancelable cancelable = api.query(request, onResponse::accept, failureConsumer);
        if (cancelable != null) {
            return cancelable;
        }
        return new NoOpCancelable();
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable create(
            @NonNull T model,
            @NonNull ModelSchema modelSchema,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure) {
        try {
            final GraphQLRequest<ModelWithMetadata<T>> request =
                    AppSyncRequestFactory.buildCreationRequest(modelSchema, model, authModeStrategyType);
            return mutation(request, onResponse, onFailure);
        } catch (AmplifyException amplifyException) {
            onFailure.accept(new DataStoreException(
                "Error encountered while creating model schema",
                amplifyException, "See attached exception for more details"
            ));
        }

        return new NoOpCancelable();
    }

    /**
     * Uses Amplify API to make a mutation which will only apply if the version sent matches the server version.
     *
     * @param model      An instance of the Model with the values to mutate
     * @param modelSchema The schema of the object being deleted
     * @param version    The version of the model we have
     * @param onResponse Invoked when response data is available.
     * @param onFailure  Invoked on failure to obtain response data
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    @Override
    public <T extends Model> Cancelable update(
            @NonNull T model,
            @NonNull ModelSchema modelSchema,
            @NonNull Integer version,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure) {
        return update(model, modelSchema, version, QueryPredicates.all(), onResponse, onFailure);
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable update(
            @NonNull T model,
            @NonNull ModelSchema modelSchema,
            @NonNull Integer version,
            @NonNull QueryPredicate predicate,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure) {
        try {
            final GraphQLRequest<ModelWithMetadata<T>> request =
                AppSyncRequestFactory.buildUpdateRequest(modelSchema,
                                                         model,
                                                         version,
                                                         predicate,
                                                         authModeStrategyType);
            return mutation(request, onResponse, onFailure);
        } catch (AmplifyException amplifyException) {
            onFailure.accept(new DataStoreException(
                "Error encountered while creating model schema",
                amplifyException, "See attached exception for more details"
            ));
        }

        return new NoOpCancelable();
    }

    /**
     * Uses Amplify API to make a mutation which will only apply if the version sent matches the server version.
     *
     * @param model       An instance of the Model to be deleted
     * @param modelSchema The schema of the object being deleted
     * @param version     The version of the model we have
     * @param onResponse  Invoked when response data is available.
     * @param onFailure   Invoked on failure to obtain response data
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    @Override
    public <T extends Model> Cancelable delete(
            @NonNull T model,
            @NonNull ModelSchema modelSchema,
            @NonNull Integer version,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure) {
        return delete(model, modelSchema, version, QueryPredicates.all(), onResponse, onFailure);
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable delete(
            @NonNull T model,
            @NonNull ModelSchema modelSchema,
            @NonNull Integer version,
            @NonNull QueryPredicate predicate,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure) {
        try {
            final GraphQLRequest<ModelWithMetadata<T>> request =
                    AppSyncRequestFactory.buildDeletionRequest(modelSchema,
                                                               model,
                                                               version,
                                                               predicate,
                                                               authModeStrategyType);
            return mutation(request, onResponse, onFailure);
        } catch (DataStoreException dataStoreException) {
            onFailure.accept(dataStoreException);
        }

        return new NoOpCancelable();
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable onCreate(
            @NonNull ModelSchema modelSchema,
            @NonNull Consumer<String> onSubscriptionStarted,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onNextResponse,
            @NonNull Consumer<DataStoreException> onSubscriptionFailure,
            @NonNull Action onSubscriptionCompleted) {
        return subscription(
            SubscriptionType.ON_CREATE,
            modelSchema,
            onSubscriptionStarted,
            onNextResponse,
            onSubscriptionFailure,
            onSubscriptionCompleted
        );
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable onUpdate(
            @NonNull ModelSchema modelSchema,
            @NonNull Consumer<String> onSubscriptionStarted,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onNextResponse,
            @NonNull Consumer<DataStoreException> onSubscriptionFailure,
            @NonNull Action onSubscriptionCompleted) {
        return subscription(
            SubscriptionType.ON_UPDATE,
            modelSchema,
            onSubscriptionStarted,
            onNextResponse,
            onSubscriptionFailure,
            onSubscriptionCompleted
        );
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable onDelete(
            @NonNull ModelSchema modelSchema,
            @NonNull Consumer<String> onSubscriptionStarted,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onNextResponse,
            @NonNull Consumer<DataStoreException> onSubscriptionFailure,
            @NonNull Action onSubscriptionCompleted) {
        return subscription(
            SubscriptionType.ON_DELETE,
            modelSchema,
            onSubscriptionStarted,
            onNextResponse,
            onSubscriptionFailure,
            onSubscriptionCompleted
        );
    }

    private <T extends Model> Cancelable subscription(
            SubscriptionType subscriptionType,
            ModelSchema modelSchema,
            Consumer<String> onSubscriptionStarted,
            Consumer<GraphQLResponse<ModelWithMetadata<T>>> onNextResponse,
            Consumer<DataStoreException> onSubscriptionFailure,
            Action onSubscriptionCompleted) {
        final GraphQLRequest<ModelWithMetadata<T>> request;
        try {
            request = AppSyncRequestFactory.buildSubscriptionRequest(modelSchema,
                                                                     subscriptionType,
                                                                     authModeStrategyType);
        } catch (DataStoreException requestGenerationException) {
            onSubscriptionFailure.accept(requestGenerationException);
            return new NoOpCancelable();
        }

        final Consumer<GraphQLResponse<ModelWithMetadata<T>>> responseConsumer = response -> {
            if (response.hasErrors()) {
                onSubscriptionFailure.accept(new DataStoreException.GraphQLResponseException(
                    "Subscription error for " + modelSchema.getName() + ": " + response.getErrors(),
                    response.getErrors()
                ));
            } else {
                onNextResponse.accept(response);
            }
        };
        final Consumer<ApiException> failureConsumer = failure ->
            onSubscriptionFailure.accept(new DataStoreException(
                "Error during subscription.", failure, "Evaluate details."
            ));

        final Cancelable cancelable = api.subscribe(
            request,
            onSubscriptionStarted,
            responseConsumer,
            failureConsumer,
            onSubscriptionCompleted
        );
        if (cancelable != null) {
            return cancelable;
        }
        return new NoOpCancelable();
    }

    private <T extends Model> Cancelable mutation(
            final GraphQLRequest<ModelWithMetadata<T>> request,
            final Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            final Consumer<DataStoreException> onFailure) {

        final Consumer<GraphQLResponse<ModelWithMetadata<T>>> responseConsumer = response -> {
            if (response.hasErrors()) {
                onResponse.accept(new GraphQLResponse<>(null, response.getErrors()));
            } else {
                onResponse.accept(response);
            }
        };
        final Consumer<ApiException> failureConsumer =
            failure -> onFailure.accept(new DataStoreException(
                "Failure during mutation.", failure, "Check details."
            ));
        final Cancelable cancelable = api.mutate(request, responseConsumer, failureConsumer);
        if (cancelable != null) {
            return cancelable;
        }
        return new NoOpCancelable();
    }
}
