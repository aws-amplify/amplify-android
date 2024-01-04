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

package com.amplifyframework.datastore.storage;

import android.content.Context;
import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.testutils.Await;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;

/**
 * A synchronization wrapper on top of a {@link LocalStorageAdapter} instance, which presents
 * the storage adapter's functionality via synchronous methods, without callbacks.
 * If any of the synchronous operations timeout, they will throw {@link RuntimeException}.
 * If the operation can return an {@link DataStoreException} via an error callback in async form,
 * then this adapter will throw that exception on the calling thread directly, to interrupt the
 * flow of execution.
 */
public final class SynchronousStorageAdapter {
    private static final long DEFAULT_OPERATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(3);

    private final long operationTimeoutMs;
    private final LocalStorageAdapter asyncDelegate;

    private SynchronousStorageAdapter(LocalStorageAdapter asyncDelegate, long operationTimeoutMs) {
        this.asyncDelegate = asyncDelegate;
        this.operationTimeoutMs = operationTimeoutMs;
    }

    /**
     * Creates a new instance which will proxy calls to the provided {@link LocalStorageAdapter}.
     * The synchronous operations exposed by the returned adapter will timeout after a default,
     * "reasonable" delay.
     * @param asyncDelegate Adapter to which calls will be delegated
     * @return A SynchronousStorageAdapter configured to proxy towards the provided async storage adapter
     */
    public static SynchronousStorageAdapter delegatingTo(@NonNull LocalStorageAdapter asyncDelegate) {
        Objects.requireNonNull(asyncDelegate);
        return new SynchronousStorageAdapter(asyncDelegate, DEFAULT_OPERATION_TIMEOUT_MS);
    }

    /**
     * Creates a new instance which will proxy calls to the provided {@link LocalStorageAdapter}.
     * The synchronous operations exposed by the returned adapter will timeout after the provided
     * amount of time, in milliseconds.
     * @param asyncDelegate Adapter to which calls will be delegated
     * @param operationTimeoutMs Amount of time after which an operation will time out
     * @return A SynchronousStorageAdapter configured to proxy towards the provided async storage adapter
     */
    public static SynchronousStorageAdapter create(
        @NonNull LocalStorageAdapter asyncDelegate, long operationTimeoutMs) {
        Objects.requireNonNull(asyncDelegate);
        return new SynchronousStorageAdapter(asyncDelegate, operationTimeoutMs);
    }

    /**
     * Initialize the storage adapter.
     * @param context App Context
     * @param dataStoreConfiguration DataStore configuration
     * @throws DataStoreException On any failure to initialize storage adapter
     */
    public void initialize(
            @NonNull Context context,
            @NonNull DataStoreConfiguration dataStoreConfiguration) throws DataStoreException {
        Await.result(
            operationTimeoutMs,
            (Consumer<List<ModelSchema>> onResult, Consumer<DataStoreException> onError) ->
                asyncDelegate.initialize(
                    context,
                    onResult,
                    onError,
                    dataStoreConfiguration
                )
        );
    }

    /**
     * Save a model into the storage adapter.
     * @param model Model to save
     * @param <T> Type of model being saved
     * @throws DataStoreException On any failure to save model into storage adapter
     */
    public <T extends Model> void save(@NonNull T model) throws DataStoreException {
        save(model, QueryPredicates.all());
    }

    /**
     * Save a model.
     * @param model Model to save
     * @param predicate An existing instance of the model in the storage adapter must meet these criteria
     *                  in order for the save to succeed.
     * @param <T> Type of model being saved
     * @throws DataStoreException On any failure to save the model
     */
    public <T extends Model> void save(@NonNull T model, @NonNull QueryPredicate predicate)
        throws DataStoreException {
        Await.result(
            operationTimeoutMs,
            (Consumer<StorageItemChange<T>> onResult, Consumer<DataStoreException> onError) ->
                asyncDelegate.save(
                    model,
                    StorageItemChange.Initiator.DATA_STORE_API,
                    predicate,
                    onResult,
                    onError
                )
        );
    }

    /**
     * Save some models.
     * @param models Models to save
     * @param <T> Type of models
     * @throws DataStoreException On failure to save any of the models
     */
    @SuppressWarnings("varargs")
    @SafeVarargs
    public final <T extends Model> void save(@NonNull T... models) throws DataStoreException {
        for (T model : models) {
            save(model);
        }
    }

    /**
     * Query the storage adapter for models of a given class.
     * @param modelClass Class of models being queried
     * @param <T> Type of models being queried
     * @return The list of models in the storage adapter that are of the requested class
     * @throws DataStoreException On any failure to query storage adapter
     */
    public <T extends Model> List<T> query(@NonNull Class<T> modelClass) throws DataStoreException {
        return query(modelClass, Where.matchesAll());
    }

    /**
     * Query for all models that are of the types provided in the given model provider.
     * For example, if the model provider returns A.class, B.class, then this query
     * methods would return all instances of model types A and B.
     * @param modelProvider Provides of model classes
     * @return All instances of the provided models that exist in the store
     * @throws DataStoreException On failure to query the store
     */
    public List<? extends Model> query(@NonNull ModelProvider modelProvider) throws DataStoreException {
        final List<Model> models = new ArrayList<>();
        for (Class<? extends Model> modelClass : modelProvider.models()) {
            models.addAll(query(modelClass));
        }
        return models;
    }

    /**
     * Query the storage adapter for models of a given class, and considering some additional criteria
     * that each model must meet.
     * @param modelClass Class of models being queried
     * @param options Query options that can include predicate and pagination
     * @param <T> Type of model being queried
     * @return The list of models which are of the requested class and meet the requested criteria
     * @throws DataStoreException On any failure to query the storage adapter
     */
    public <T extends Model> List<T> query(@NonNull Class<T> modelClass, @NonNull QueryOptions options)
        throws DataStoreException {
        Iterator<T> resultIterator = Await.result(
            operationTimeoutMs,
            (Consumer<Iterator<T>> onResult, Consumer<DataStoreException> onError) ->
                asyncDelegate.query(modelClass, options, onResult, onError)
        );
        final List<T> results = new ArrayList<>();
        while (resultIterator.hasNext()) {
            results.add(resultIterator.next());
        }
        return results;
    }

    /**
     * Delete a model, unconditionally. Expect success.
     * @param model A model to be deleted
     * @param <T> Type of model being deleted
     * @throws DataStoreException On any failure to delete model
     */
    public <T extends Model> void delete(@NonNull T model) throws DataStoreException {
        delete(model, QueryPredicates.all());
    }

    /**
     * Delete a model, and expect success.
     * @param model Model to delete
     * @param predicate Conditions that must be met before model is candidate for deletion
     * @param <T> Type of model being deleted
     * @throws DataStoreException On any failure to delete the model
     */
    public <T extends Model> void delete(@NonNull T model, @NonNull QueryPredicate predicate)
        throws DataStoreException {
        Await.result(
            operationTimeoutMs,
            (Consumer<StorageItemChange<T>> onResult, Consumer<DataStoreException> onError) ->
                asyncDelegate.delete(
                    model,
                    StorageItemChange.Initiator.DATA_STORE_API,
                    predicate,
                    onResult,
                    onError
                )
        );
    }

    /**
     * Delete a model, but /expect/ the operation to fail, due to some exception being thrown.
     * Return the raised {@link DataStoreException} in a synchronous way,
     * instead of letting it crash the calling thread.
     * @param model Try to delete this
     * @param <T> Type of thing being deleted
     * @return The exception that was thrown while attempting to delete
     */
    @SuppressWarnings("unused")
    public <T extends Model> DataStoreException deleteExpectingError(@NonNull T model) {
        return Await.error(
            operationTimeoutMs,
            (Consumer<StorageItemChange<T>> onResult, Consumer<DataStoreException> onError) ->
                asyncDelegate.delete(
                    model,
                    StorageItemChange.Initiator.DATA_STORE_API,
                    QueryPredicates.all(),
                    onResult,
                    onError
                )
        );
    }

    /**
     * Observe changes in the adapter.
     * @return An observable stream of storage item changes.
     */
    public Observable<StorageItemChange<? extends Model>> observe() {
        return Observable.create(emitter ->
            asyncDelegate.observe(emitter::onNext, emitter::onError, emitter::onComplete)
        );
    }

    /**
     * Pass terminate to delegate.
     * @throws DataStoreException if termination fails
     */
    public void terminate() throws DataStoreException {
        asyncDelegate.terminate();
    }
}
