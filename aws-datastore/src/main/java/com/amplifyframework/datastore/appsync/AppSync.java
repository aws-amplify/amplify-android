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

import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;

/**
 * Client interface for an AppSync service endpoint.
 *
 * Specifically, an AppSync client expects that the AppSync endpoint:
 *
 *   1. May be queried via base/delta sync queries, to understand the current states of
 *      the data warehoused on the AppSync endpoint. The logic to perform a base or a
 *      delta query is managed by business rules around the last sync time;
 *
 *   2. Supports create, update, delete mutations, to modify the state of any data
 *      that is warehoused at the endpoint. These operations consider a unique ID for
 *      each model instance, as well as a monotonically increasing version for every
 *      model instance warehoused at the endpoint;
 *
 *   3. Can host subscriptions, over which a client may receive notifications when any
 *      of the above AppSync mutations have been performed on a particular model(s);
 */
public interface AppSync {
    /**
     * Builds a sync query {@link GraphQLRequest} that can be passed to the
     * {@link AppSync#sync(GraphQLRequest, Consumer, Consumer)} method.
     * @param <T> The type of data in the response. Must extend Model.
     * @param modelClass The class of the Model we are querying on.
     * @param lastSync The time you last synced - all changes since this time are retrieved.
     * @param syncPageSize limit for number of records to return per page.
     * @return A {@link GraphQLRequest} for making a sync query
     * @throws DataStoreException on error building GraphQLRequest due to inability to obtain model schema.
     */
    @NonNull
    <T extends Model> GraphQLRequest<PaginatedResult<ModelWithMetadata<T>>> buildSyncRequest(
            @NonNull Class<T> modelClass,
            @Nullable Long lastSync,
            @Nullable Integer syncPageSize
    ) throws DataStoreException;

    /**
     * Uses Amplify API category to get a list of changes which have happened since a last sync time.
     * @param <T> The type of data in the response. Must extend Model.
     * @param request The {@link GraphQLRequest} for requesting a sync query.
     * @param onResponse Invoked when response data is available.
     * @param onFailure Invoked on failure to obtain response data.
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    <T extends Model> Cancelable sync(
            @NonNull GraphQLRequest<PaginatedResult<ModelWithMetadata<T>>> request,
            @NonNull Consumer<GraphQLResponse<PaginatedResult<ModelWithMetadata<T>>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure
    );

    /**
     * Uses Amplify API to make a mutation which will only apply if the version sent matches the server version.
     * @param model An instance of the Model with the values to mutate
     * @param onResponse Invoked when response data is available.
     * @param onFailure Invoked on failure to obtain response data
     * @param <T> The type of data in the response. Must extend Model.
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    <T extends Model> Cancelable create(
            @NonNull T model,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure
    );

    /**
     * Uses Amplify API to make a mutation which will only apply if the version sent matches the server version.
     * @param model An instance of the Model with the values to mutate
     * @param version The version of the model we have
     * @param onResponse Invoked when response data is available.
     * @param onFailure Invoked on failure to obtain response data
     * @param <T> The type of data in the response. Must extend Model.
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    <T extends Model> Cancelable update(
            @NonNull T model,
            @NonNull Integer version,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure
    );

    /**
     * Uses Amplify API to make a mutation which will only apply if the version sent matches the server version.
     * @param model An instance of the Model with the values to mutate
     * @param version The version of the model we have
     * @param predicate Condition to use for the update.
     * @param onResponse Invoked when response data is available.
     * @param onFailure Invoked on failure to obtain response data
     * @param <T> The type of data in the response. Must extend Model.
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    <T extends Model> Cancelable update(
            @NonNull T model,
            @NonNull Integer version,
            @NonNull QueryPredicate predicate,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure
    );

    /**
     * Uses Amplify API to make a mutation which will only apply if the version sent matches the server version.
     * @param clazz The class of the object being deleted
     * @param objectId ID id of the object to delete
     * @param version The version of the model we have
     * @param onResponse Invoked when response data is available.
     * @param onFailure Invoked on failure to obtain response data
     * @param <T> The type of data in the response. Must extend Model.
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    <T extends Model> Cancelable delete(
            @NonNull Class<T> clazz,
            @NonNull String objectId,
            @NonNull Integer version,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure
    );

    /**
     * Uses Amplify API to make a mutation which will only apply if the version sent matches the server version.
     * @param clazz The class of the object being deleted
     * @param objectId ID id of the object to delete
     * @param version The version of the model we have
     * @param predicate Condition to use for the delete operation.
     * @param onResponse Invoked when response data is available.
     * @param onFailure Invoked on failure to obtain response data
     * @param <T> The type of data in the response. Must extend Model.
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    <T extends Model> Cancelable delete(
            @NonNull Class<T> clazz,
            @NonNull String objectId,
            @NonNull Integer version,
            @NonNull QueryPredicate predicate,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
            @NonNull Consumer<DataStoreException> onFailure
    );

    /**
     * Get notified when a create event happens on a given class.
     * @param modelClass The class of the Model we are listening on
     * @param onSubscriptionStarted
     *        Called when subscription over network has been established
     * @param onNextResponse
     *        A callback to receive notifications when new items are
     *        available via the subscription stream
     * @param onSubscriptionFailure
     *        Called when the subscription terminates with a failure
     * @param onSubscriptionCompleted
     *        Called when the subscription terminates gracefully
     * @param <T> The type of data in the response. Must extend Model.
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    <T extends Model> Cancelable onCreate(
            @NonNull Class<T> modelClass,
            @NonNull Consumer<String> onSubscriptionStarted,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onNextResponse,
            @NonNull Consumer<DataStoreException> onSubscriptionFailure,
            @NonNull Action onSubscriptionCompleted
    );

    /**
     * Get notified when an update event happens on a given class.
     * @param modelClass The class of the Model we are listening on
     * @param onSubscriptionStarted
     *        Called when subscription over network has been established
     * @param onNextResponse
     *        A callback to receive notifications when new items are
     *        available via the subscription stream
     * @param onSubscriptionFailure
     *        Called when the subscription terminates with a failure
     * @param onSubscriptionCompleted
     *        Called when the subscription terminates gracefully
     * @param <T> The type of data in the response. Must extend Model.
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    <T extends Model> Cancelable onUpdate(
            @NonNull Class<T> modelClass,
            @NonNull Consumer<String> onSubscriptionStarted,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onNextResponse,
            @NonNull Consumer<DataStoreException> onSubscriptionFailure,
            @NonNull Action onSubscriptionCompleted
    );

    /**
     * Get notified when a delete event happens on a given class.
     * @param modelClass The class of the Model we are listening on
     * @param onSubscriptionStarted
     *        Called when subscription over network has been established
     * @param onNextResponse
     *        A callback to receive notifications when new items are
     *        available via the subscription stream
     * @param onSubscriptionFailure
     *        Called when the subscription terminates with a failure
     * @param onSubscriptionCompleted
     *        Called when the subscription terminates gracefully
     * @param <T> The type of data in the response. Must extend Model.
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    <T extends Model> Cancelable onDelete(
            @NonNull Class<T> modelClass,
            @NonNull Consumer<String> onSubscriptionStarted,
            @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onNextResponse,
            @NonNull Consumer<DataStoreException> onSubscriptionFailure,
            @NonNull Action onSubscriptionCompleted
    );
}
