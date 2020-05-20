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

package com.amplifyframework.datastore.appsync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.NoOpConsumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.testutils.Await;

import java.util.Objects;

import io.reactivex.Observable;

/**
 * A synchronous wrapper around an AppSync client, useful in test, so you can just
 * wait for the results of network operations.
 */
@SuppressWarnings("unused") // Oh, that it might be, someday, though!
public final class SynchronousAppSync {
    private final AppSync appSync;

    private SynchronousAppSync(AppSync appSync) {
        this.appSync = appSync;
    }

    /**
     * Creates a new SynchronousAppSync instance, that proxies calls into
     * the provided async AppSync client.
     * @param appSync AppSync client
     * @return Synchronous wrapper around app sync client
     */
    public static SynchronousAppSync using(@NonNull AppSync appSync) {
        Objects.requireNonNull(appSync);
        return new SynchronousAppSync(appSync);
    }

    /**
     * Uses Amplify API category to get a list of changes which have happened since a last sync time.
     * @param <T> The type of data in the response. Must extend Model.
     * @param modelClass The class of the Model we are querying on
     * @param lastSync The time you last synced - all changes since this time are retrieved.
     * @return Response data from AppSync.
     * @throws DataStoreException on failure to obtain response data
     */
    @NonNull
    public <T extends Model> GraphQLResponse<Iterable<ModelWithMetadata<T>>> sync(
            @NonNull Class<T> modelClass,
            @Nullable Long lastSync) throws DataStoreException {
        return Await.<GraphQLResponse<Iterable<ModelWithMetadata<T>>>, DataStoreException>result((onResult, onError) ->
            appSync.sync(modelClass, lastSync, onResult, onError)
        );
    }

    /**
     * Uses Amplify API to make a mutation which will only apply if the version sent matches the server version.
     * @param model An instance of the Model with the values to mutate
     * @param <T> The type of data in the response. Must extend Model.
     * @return Response data from AppSync
     * @throws DataStoreException On failure to obtain response data
     */
    @NonNull
    public <T extends Model> GraphQLResponse<ModelWithMetadata<T>> create(@NonNull T model) throws DataStoreException {
        return Await.<GraphQLResponse<ModelWithMetadata<T>>, DataStoreException>result((onResult, onError) ->
            appSync.create(model, onResult, onError)
        );
    }

    /**
     * Uses Amplify API to make a mutation which will only apply if the version sent matches the server version.
     * @param model An instance of the Model with the values to mutate
     * @param version The version of the model we have
     * @param <T> The type of data in the response. Must extend Model.
     * @param predicate The condition to be applied to the update.
     * @return Response data is from AppSync
     * @throws DataStoreException On failure to obtain response data
     */
    @NonNull
    public <T extends Model> GraphQLResponse<ModelWithMetadata<T>> update(
        @NonNull T model, @NonNull Integer version, @Nullable QueryPredicate predicate) throws DataStoreException {
        return Await.<GraphQLResponse<ModelWithMetadata<T>>, DataStoreException>result(((onResult, onError) ->
            appSync.update(model, version, predicate, onResult, onError)
        ));
    }

    /**
     * Uses Amplify API to make a mutation which will only apply if the version sent matches the server version.
     * @param clazz The class of the object being deleted
     * @param objectId ID id of the object to delete
     * @param version The version of the model we have
     * @param version The condition to be applied to the delete.
     * @param <T> The type of data in the response. Must extend Model.
     * @return Response data from AppSync.
     * @throws DataStoreException On failure to obtain response data
     */
    @SuppressWarnings("LineLength")
    @NonNull
    <T extends Model> GraphQLResponse<ModelWithMetadata<T>> delete(@NonNull Class<T> clazz,
                                                                   @NonNull String objectId,
                                                                   @NonNull Integer version,
                                                                   @Nullable QueryPredicate predicate) throws DataStoreException {
        return Await.<GraphQLResponse<ModelWithMetadata<T>>, DataStoreException>result((onResult, onError) ->
            appSync.delete(clazz, objectId, version, predicate, onResult, onError)
        );
    }

    /**
     * Get notified when a create event happens on a given class.
     * @param modelClass The class of the Model we are listening on
     * @param <T> The type of data in the response. Must extend Model.
     * @return An observable that emits creation events, error/completion on termination
     */
    @NonNull
    public <T extends Model> Observable<GraphQLResponse<ModelWithMetadata<T>>> onCreate(@NonNull Class<T> modelClass) {
        return Observable.defer(() -> Observable.create(emitter ->
            appSync.onCreate(modelClass, NoOpConsumer.create(), emitter::onNext, emitter::onError, emitter::onComplete)
        ));
    }

    /**
     * Get notified when an update event happens on a given class.
     * @param modelClass The class of the Model we are listening on
     * @param <T> The type of data in the response. Must extend Model.
     * @return An observable that emits update events, error/completion on termination
     */
    @NonNull
    public <T extends Model> Observable<GraphQLResponse<ModelWithMetadata<T>>> onUpdate(@NonNull Class<T> modelClass) {
        return Observable.defer(() -> Observable.create(emitter ->
            appSync.onUpdate(modelClass, NoOpConsumer.create(), emitter::onNext, emitter::onError, emitter::onComplete)
        ));
    }

    /**
     * Get notified when a delete event happens on a given class.
     * @param modelClass The class of the Model we are listening on
     * @param <T> The type of data in the response. Must extend Model.
     * @return An observable that emits deletion events, error/completion on termination
     */
    @NonNull
    public <T extends Model> Observable<GraphQLResponse<ModelWithMetadata<T>>> onDelete(@NonNull Class<T> modelClass) {
        return Observable.defer(() -> Observable.create(emitter ->
            appSync.onDelete(modelClass, NoOpConsumer.create(), emitter::onNext, emitter::onError, emitter::onComplete)
        ));
    }
}
