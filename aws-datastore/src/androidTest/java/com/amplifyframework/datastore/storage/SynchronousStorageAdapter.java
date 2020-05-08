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
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.testutils.Await;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

/**
 * A synchronization wrapper on top of a {@link LocalStorageAdapter} instance, which presents
 * the storage adapter's functionality via synchronous methods, without callbacks.
 * If any of the synchronous operations timeout, they will throw {@link RuntimeException}.
 * If the operation can return an {@link DataStoreException} via an error callback in async form,
 * then this adapter will throw that exception on the calling thread directly, to interrupt the
 * flow of execution.
 */
public final class SynchronousStorageAdapter {
    private static final long DEFAULT_OPERATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(2);

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
     * Initializes the storage adapter.
     * @param context An Android Context
     * @return The list of model schema that are available for use in the adapter
     * @throws DataStoreException On any initialization failure
     */
    @SuppressWarnings("UnusedReturnValue")
    public List<ModelSchema> initialize(@NonNull Context context) throws DataStoreException {
        return Await.result(
            operationTimeoutMs,
            (Consumer<List<ModelSchema>> onResult, Consumer<DataStoreException> onError) ->
                asyncDelegate.initialize(context, onResult, onError)
        );
    }

    /**
     * Terminate use of the storage adapter.
     * @throws DataStoreException On failure to terminate
     */
    public void terminate() throws DataStoreException {
        asyncDelegate.terminate();
    }

    /**
     * Save a model into the storage adapter.
     * @param model Model to save
     * @param <T> Type of model being saved
     * @throws DataStoreException On any failure to save model into storage adapter
     */
    public <T extends Model> void save(@NonNull T model) throws DataStoreException {
        //noinspection ConstantConditions
        save(model, null);
    }

    /**
     * Save a model.
     * @param model Model to save
     * @param predicate An existing instance of the model in the storage adapter must meet these criteria
     *                  in order for the save to succeed. If null, no criteria are considered
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
     * Try to save a model, but /expect/ it not to work.
     * @param model A model to save
     * @param <T> Type of model being saved
     * @return The exception that was raised while attempting to save the model
     */
    public <T extends Model> DataStoreException saveExpectingError(@NonNull T model) {
        //noinspection ConstantConditions
        return saveExpectingError(model, null);
    }

    /**
     * Save a model, but /expect/ it not to work. Return the exception instead of allowing
     * it to crash the calling thread.
     * @param model A model for which to attempt a deletion
     * @param predicate Criteria that must be met in order for the deletion to work
     * @param <T> Type of model being deleted
     * @return The exception that occured while attempting the deletion
     */
    public <T extends Model> DataStoreException saveExpectingError(
            @NonNull T model, @NonNull QueryPredicate predicate) {
        return Await.error(
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
     * Query the storage adapter for models of a given class.
     * @param modelClass Class of models being queried
     * @param <T> Type of models being queried
     * @return The list of models in the storage adapter that are of the requested class
     * @throws DataStoreException On any failure to query storage adapter
     */
    public <T extends Model> List<T> query(@NonNull Class<T> modelClass) throws DataStoreException {
        //noinspection ConstantConditions
        return query(modelClass, null);
    }

    /**
     * Query the storage adapter for models of a given class, and considering some additional criteria
     * that each model must meet.
     * @param modelClass Class of models being queried
     * @param predicate Additional criteria that the models must match
     * @param <T> Type of model being queried
     * @return The list of models which are of the requested class and meet the requested criteria
     * @throws DataStoreException On any failure to query the storage adapter
     */
    public <T extends Model> List<T> query(@NonNull Class<T> modelClass, @NonNull QueryPredicate predicate)
            throws DataStoreException {
        Iterator<T> resultIterator = Await.result(
            operationTimeoutMs,
            (Consumer<Iterator<T>> onResult, Consumer<DataStoreException> onError) ->
                asyncDelegate.query(modelClass, predicate, onResult, onError)
        );
        final List<T> resultSet = new ArrayList<>();
        while (resultIterator.hasNext()) {
            resultSet.add(resultIterator.next());
        }
        return resultSet;
    }

    /**
     * Delete a model, unconditionally. Expect success.
     * @param model A model to be deleted
     * @param <T> Type of model being deleted
     * @throws DataStoreException On any failure to delete model
     */
    public <T extends Model> void delete(@NonNull T model) throws DataStoreException {
        //noinspection ConstantConditions
        delete(model, null);
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
        //noinspection ConstantConditions
        return deleteExpectingError(model, null);
    }

    /**
     * Delete a model, but /expect/ the operation to fail, due to some exception being thrown.
     * Consider a predicate while attempting the deletion.
     * Return the raised {@link DataStoreException} in a synchronous way,
     * instead of letting it crash the calling thread.
     * @param model Try to delete this
     * @param predicate Apply these conditions when deleting
     * @param <T> Type of thing being deleted
     * @return The exception that was thrown while attempting to delete
     */
    public <T extends Model> DataStoreException deleteExpectingError(
            @NonNull T model, @NonNull QueryPredicate predicate) {
        return Await.error(
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
     * Observe changes to the local storage.
     * @return An observable stream of changes to the local storage.
     */
    public Observable<StorageItemChange<? extends Model>> observe() {
        return Observable.create(emitter ->
            asyncDelegate.observe(emitter::onNext, emitter::onError, emitter::onComplete)
        );
    }
}
