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

package com.amplifyframework.datastore.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.StreamListener;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;

/**
 * Convenience class to call API in a way that supports versioning and retrieving sync metadata.
 */
@SuppressWarnings("unused") // Hold my beer...
public interface AppSyncEndpoint {
    /**
     * Uses Amplify API category to get a list of changes which have happened since a last sync time.
     * @param <T> The type of data in the response. Must extend Model.
     * @param modelClass The class of the Model we are querying on
     * @param lastSync The time you last synced - all changes since this time are retrieved.
     * @param responseListener Invoked when response data/errors are available.
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    <T extends Model> Cancelable sync(
            @NonNull Class<T> modelClass,
            @Nullable Long lastSync,
            @NonNull ResultListener<GraphQLResponse<Iterable<ModelWithMetadata<T>>>> responseListener);

    /**
     * Uses Amplify API to make a mutation which will only apply if the version sent matches the server version.
     * @param model An instance of the Model with the values to mutate
     * @param responseListener Invoked when response data/errors are available.
     * @param <T> The type of data in the response. Must extend Model.
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    <T extends Model> Cancelable create(
            @NonNull T model,
            @NonNull ResultListener<GraphQLResponse<ModelWithMetadata<T>>> responseListener);

    /**
     * Uses Amplify API to make a mutation which will only apply if the version sent matches the server version.
     * @param model An instance of the Model with the values to mutate
     * @param version The version of the model we have
     * @param responseListener Invoked when response data/errors are available.
     * @param <T> The type of data in the response. Must extend Model.
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    <T extends Model> Cancelable update(
            @NonNull T model,
            @NonNull Integer version,
            @NonNull ResultListener<GraphQLResponse<ModelWithMetadata<T>>> responseListener);

    /**
     * Uses Amplify API to make a mutation which will only apply if the version sent matches the server version.
     * @param clazz The class of the object being deleted
     * @param objectId ID id of the object to delete
     * @param version The version of the model we have
     * @param responseListener Invoked when response data/errors are available.
     * @param <T> The type of data in the response. Must extend Model.
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    <T extends Model> Cancelable delete(
            @NonNull Class<T> clazz,
            @NonNull String objectId,
            @NonNull Integer version,
            @NonNull ResultListener<GraphQLResponse<ModelWithMetadata<T>>> responseListener);

    /**
     * Get notified when a create event happens on a given class.
     * @param modelClass The class of the Model we are listening on
     * @param subscriptionListener  A listener to receive notifications when new items are
     *                              available via the subscription stream
     * @param <T> The type of data in the response. Must extend Model.
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    <T extends Model> Cancelable onCreate(
            @NonNull Class<T> modelClass,
            @NonNull StreamListener<GraphQLResponse<ModelWithMetadata<T>>> subscriptionListener);

    /**
     * Get notified when an update event happens on a given class.
     * @param modelClass The class of the Model we are listening on
     * @param subscriptionListener  A listener to receive notifications when new items are
     *                              available via the subscription stream
     * @param <T> The type of data in the response. Must extend Model.
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    <T extends Model> Cancelable onUpdate(
            @NonNull Class<T> modelClass,
            @NonNull StreamListener<GraphQLResponse<ModelWithMetadata<T>>> subscriptionListener);

    /**
     * Get notified when a delete event happens on a given class.
     * @param modelClass The class of the Model we are listening on
     * @param subscriptionListener  A listener to receive notifications when new items are
     *                              available via the subscription stream
     * @param <T> The type of data in the response. Must extend Model.
     * @return A {@link Cancelable} to provide a means to cancel the asynchronous operation
     */
    @NonNull
    <T extends Model> Cancelable onDelete(
            @NonNull Class<T> modelClass,
            @NonNull StreamListener<GraphQLResponse<ModelWithMetadata<T>>> subscriptionListener);
}
