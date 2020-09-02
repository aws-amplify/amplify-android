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
import com.amplifyframework.api.graphql.GraphQLBehavior;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.async.NoOpCancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreException;

import java.util.Objects;

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
    private final GraphQLBehavior api;
    private final ApiNameProvider apiNameProvider;

    /**
     * Constructs a new AppSyncClient.
     * @param api The API Category, configured with a DataStore API
     * @param apiNameProvider Provides the name of the API to target.
     */
    private AppSyncClient(GraphQLBehavior api, ApiNameProvider apiNameProvider) {
        this.api = api;
        this.apiNameProvider = apiNameProvider;
    }

    /**
     * Obtain an instance of the AppSyncAPI, which uses the Amplify API category
     * as its backing implementation for GraphQL behaviors.
     * Specifies an API token to pass into the API category while targeting an AppSync endpoint.
     * @param api GraphQL api behavior through which this app sync client will talk
     * @param apiNameProvider Provides the name of the API that will be targeted.
     * @return An App Sync API instance
     */
    @NonNull
    public static AppSyncClient via(@NonNull GraphQLBehavior api, @NonNull ApiNameProvider apiNameProvider) {
        Objects.requireNonNull(api);
        Objects.requireNonNull(apiNameProvider);
        return new AppSyncClient(api, apiNameProvider);
    }

    /**
     * Obtain an instance of the AppSyncAPI, which uses the Amplify API category
     * as its backing implementation for GraphQL behaviors.
     * @param api GraphQL api behavior through which this app sync client will talk
     * @return An App Sync API instance
     */
    @NonNull
    public static AppSyncClient via(@NonNull GraphQLBehavior api) {
        Objects.requireNonNull(api);
        return new AppSyncClient(api, new NullApiNameProvider());
    }

    @NonNull
    @Override
    public <T extends Model> GraphQLRequest<PaginatedResult<ModelWithMetadata<T>>> buildSyncRequest(
            @NonNull Class<T> modelClass,
            @Nullable Long lastSync,
            @Nullable Integer syncPageSize) throws DataStoreException {
        return AppSyncRequestFactory.buildSyncRequest(modelClass, lastSync, syncPageSize);
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable sync(
            @NonNull GraphQLRequest<PaginatedResult<ModelWithMetadata<T>>> request,
            @NonNull Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<T>>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure
    ) {
        final Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<T>>>> responseConsumer = apiQueryResponse -> {
            if (apiQueryResponse.hasErrors()) {
                onFailure.accept(new DataStoreException(
                    "Failure performing sync query to AppSync: " + apiQueryResponse.getErrors().toString(),
                    AmplifyException.TODO_RECOVERY_SUGGESTION
                ));
            } else {
                onResponse.accept(apiQueryResponse);
            }
        };
        final Consumer<ApiException> failureConsumer =
            failure -> onFailure.accept(new DataStoreException(
                "Failure performing sync query to AppSync.",
                failure, AmplifyException.TODO_RECOVERY_SUGGESTION
            ));

        final String apiName = apiNameProvider.getApiName();
        final Cancelable cancelable = (apiName != null) ?
            api.query(apiName, request, responseConsumer, failureConsumer) :
            api.query(request, responseConsumer, failureConsumer);
        if (cancelable != null) {
            return cancelable;
        }
        return new NoOpCancelable();
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable create(
            @NonNull T model,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure) {
        try {
            final GraphQLRequest<ModelWithMetadata<T>> request = AppSyncRequestFactory.buildCreationRequest(model);
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
     * @param version    The version of the model we have
     * @param onResponse Invoked when response data is available.
     * @param onFailure  Invoked on failure to obtain response data
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    @Override
    public <T extends Model> Cancelable update(
            @NonNull T model,
            @NonNull Integer version,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure) {
        return update(model, version, QueryPredicates.all(), onResponse, onFailure);
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable update(
            @NonNull T model,
            @NonNull Integer version,
            @NonNull QueryPredicate predicate,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure) {
        try {
            final GraphQLRequest<ModelWithMetadata<T>> request =
                    AppSyncRequestFactory.buildUpdateRequest(model, version, predicate);
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
     * @param clazz      The class of the object being deleted
     * @param objectId   ID id of the object to delete
     * @param version    The version of the model we have
     * @param onResponse Invoked when response data is available.
     * @param onFailure  Invoked on failure to obtain response data
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    @Override
    public <T extends Model> Cancelable delete(
            @NonNull Class<T> clazz,
            @NonNull String objectId,
            @NonNull Integer version,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure) {
        return delete(clazz, objectId, version, QueryPredicates.all(), onResponse, onFailure);
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable delete(
            @NonNull Class<T> clazz,
            @NonNull String objectId,
            @NonNull Integer version,
            @NonNull QueryPredicate predicate,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure) {
        try {
            final GraphQLRequest<ModelWithMetadata<T>> request =
                    AppSyncRequestFactory.buildDeletionRequest(clazz, objectId, version, predicate);
            return mutation(request, onResponse, onFailure);
        } catch (DataStoreException dataStoreException) {
            onFailure.accept(dataStoreException);
        }

        return new NoOpCancelable();
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable onCreate(
            @NonNull Class<T> modelClass,
            @NonNull Consumer<String> onSubscriptionStarted,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onNextResponse,
            @NonNull Consumer<DataStoreException> onSubscriptionFailure,
            @NonNull Action onSubscriptionCompleted) {
        return subscription(
            SubscriptionType.ON_CREATE,
            modelClass,
            onSubscriptionStarted,
            onNextResponse,
            onSubscriptionFailure,
            onSubscriptionCompleted
        );
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable onUpdate(
            @NonNull Class<T> modelClass,
            @NonNull Consumer<String> onSubscriptionStarted,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onNextResponse,
            @NonNull Consumer<DataStoreException> onSubscriptionFailure,
            @NonNull Action onSubscriptionCompleted) {
        return subscription(
            SubscriptionType.ON_UPDATE,
            modelClass,
            onSubscriptionStarted,
            onNextResponse,
            onSubscriptionFailure,
            onSubscriptionCompleted
        );
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable onDelete(
            @NonNull Class<T> modelClass,
            @NonNull Consumer<String> onSubscriptionStarted,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onNextResponse,
            @NonNull Consumer<DataStoreException> onSubscriptionFailure,
            @NonNull Action onSubscriptionCompleted) {
        return subscription(
            SubscriptionType.ON_DELETE,
            modelClass,
            onSubscriptionStarted,
            onNextResponse,
            onSubscriptionFailure,
            onSubscriptionCompleted
        );
    }

    private <T extends Model> Cancelable subscription(
            SubscriptionType subscriptionType,
            Class<T> clazz,
            Consumer<String> onSubscriptionStarted,
            Consumer<GraphQLResponse<ModelWithMetadata<T>>> onNextResponse,
            Consumer<DataStoreException> onSubscriptionFailure,
            Action onSubscriptionCompleted) {
        final GraphQLRequest<ModelWithMetadata<T>> request;
        try {
            request = AppSyncRequestFactory.buildSubscriptionRequest(clazz, subscriptionType);
        } catch (DataStoreException requestGenerationException) {
            onSubscriptionFailure.accept(requestGenerationException);
            return new NoOpCancelable();
        }

        final Consumer<GraphQLResponse<ModelWithMetadata<T>>> responseConsumer = response -> {
            if (response.hasErrors()) {
                onSubscriptionFailure.accept(new DataStoreException(
                    "Bad subscription data for " + clazz.getSimpleName() + ": " + response.getErrors(),
                    AmplifyException.TODO_RECOVERY_SUGGESTION
                ));
            } else {
                onNextResponse.accept(response);
            }
        };
        final Consumer<ApiException> failureConsumer = failure ->
            onSubscriptionFailure.accept(new DataStoreException(
                "Error during subscription.", failure, "Evaluate details."
            ));

        final String apiName = apiNameProvider.getApiName();
        final Cancelable cancelable = (apiName != null) ?
            api.subscribe(
                apiName,
                request,
                onSubscriptionStarted,
                responseConsumer,
                failureConsumer,
                onSubscriptionCompleted
            ) :
            api.subscribe(
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
        final String apiName = apiNameProvider.getApiName();
        final Cancelable cancelable = (apiName != null) ?
            api.mutate(apiName, request, responseConsumer, failureConsumer) :
            api.mutate(request, responseConsumer, failureConsumer);
        if (cancelable != null) {
            return cancelable;
        }
        return new NoOpCancelable();
    }

    /**
     * Provides the name of the API instance that the AppSyncClient will talk to.
     * The API name is a token used by the API category to select one of its configured
     * APIs.
     */
    @FunctionalInterface
    public interface ApiNameProvider {
        /**
         * Gets the name of the API to use when interacting with AppSync via API category.
         * @return Name of API to talk to
         */
        @Nullable
        String getApiName();
    }

    /**
     * An implementation of {@link ApiNameProvider} which always returns null for the API name;
     * that is to say "no specific API is selected; use default behavior".
     */
    private static final class NullApiNameProvider implements ApiNameProvider {
        @Nullable
        @Override
        public String getApiName() {
            return null;
        }
    }
}
