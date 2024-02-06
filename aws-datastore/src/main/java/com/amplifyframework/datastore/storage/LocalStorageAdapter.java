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

package com.amplifyframework.datastore.storage;

import android.content.Context;
import androidx.annotation.NonNull;

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.ObserveQueryOptions;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreQuerySnapshot;

import java.util.Iterator;
import java.util.List;

/**
 * A LocalStorageAdapter provides a simple set of interactions to
 * save, delete, query, and observe changes to object models. An instance of an
 * object model is called an "item" in the storage.
 *
 * An implementation of a LocalStorageAdapter is intended to provide a durable
 * local repository implementation, where item does not leave the device on which
 * this software is running (item is "local" to the localhost).
 *
 * Plausible implementations of the LocalStorageAdapter might use SQLite, SharedPreferences,
 * Room, Realm, Flat-file, in-memory, etc., etc.
 */
public interface LocalStorageAdapter {

    /**
     * Initialize the storage engine s.t. it will be able to host models
     * of the provided types. A {@link ModelSchema} will be generated for each
     * {@link Model} provided by the {@link ModelProvider}.
     *
     * This method must be called before any other method on the LocalStorageAdapter
     * may be used. Only models that have been provided at initialization time are "in-play"
     * for use with the LocalStorageAdapter. It is a user error to try to save/query/delete
     * any model type that has not been initialized by this call.
     *
     * @param context An Android Context
     * @param onSuccess A callback to be invoked upon completion of the initialization
     * @param onError A callback to be invoked upon initialization error
     * @param dataStoreConfiguration Datastore configuration
     */
    void initialize(
            @NonNull Context context,
            @NonNull Consumer<List<ModelSchema>> onSuccess,
            @NonNull Consumer<DataStoreException> onError,
            @NonNull DataStoreConfiguration dataStoreConfiguration
    );

    /**
     * Save an item into local storage only if the data being overwritten meets the
     * specific conditions. A {@link Consumer} will be invoked when the
     * save operation is completed, to notify the caller of success or failure.
     * @param <T> The type of the item being stored
     * @param item the item to save into the repository
     * @param initiator An identification of the actor who initiated this save
     * @param predicate Predicate condition for conditional write
     * @param onSuccess A callback that will be invoked if the save succeeds
     * @param onError A callback that will be invoked if the save fails with an error
     */
    <T extends Model> void save(
            @NonNull T item,
            @NonNull StorageItemChange.Initiator initiator,
            @NonNull QueryPredicate predicate,
            @NonNull Consumer<StorageItemChange<T>> onSuccess,
            @NonNull Consumer<DataStoreException> onError
    );

    /**
     * Query the storage for items of a given type with specific conditions.
     * @param itemClass Items that have this class will be solicited
     * @param options options, such as predicates, pagination to apply to query
     * @param onSuccess A callback that will be notified if the query succeeds
     * @param onError A callback that will be notified if the query fails with an error
     * @param <T> Type type of the items that are being queried
     */
    <T extends Model> void query(
            @NonNull Class<T> itemClass,
            @NonNull QueryOptions options,
            @NonNull Consumer<Iterator<T>> onSuccess,
            @NonNull Consumer<DataStoreException> onError
    );

    /**
     * Query the storage for items of a given type with specific conditions.
     * @param modelName name of the Model to query
     * @param options options, such as predicates, pagination to apply to query
     * @param onSuccess A callback that will be notified if the query succeeds
     * @param onError A callback that will be notified if the query fails with an error
     */
    void query(
            @NonNull String modelName,
            @NonNull QueryOptions options,
            @NonNull Consumer<Iterator<? extends Model>> onSuccess,
            @NonNull Consumer<DataStoreException> onError
    );

    /**
     * Deletes an item from storage only if the data being deleted meets the
     * specific conditions. A {@link Consumer} will be invoked when the
     * save operation is completed, to notify the caller of success or failure.
     * @param <T> The type of item being deleted
     * @param item Item to delete
     * @param initiator An identification of the actor who initiated this deletion
     * @param predicate Predicate condition for conditional delete
     * @param onSuccess A callback that will be invoked when deletion succeeds
     * @param onError A callback that will be invoked when deletion fails with an error
     */
    <T extends Model> void delete(
            @NonNull T item,
            @NonNull StorageItemChange.Initiator initiator,
            @NonNull QueryPredicate predicate,
            @NonNull Consumer<StorageItemChange<T>> onSuccess,
            @NonNull Consumer<DataStoreException> onError
    );

    /**
     * Deletes all items of a given type from storage that meet the
     * specific conditions. A {@link Consumer} will be invoked when the
     * save operation is completed, to notify the caller of success or failure.
     * @param <T> The type of item being deleted
     * @param itemClass Item to delete
     * @param initiator An identification of the actor who initiated this deletion
     * @param predicate Predicate condition for conditional delete
     * @param onSuccess A callback that will be invoked when deletion succeeds
     * @param onError A callback that will be invoked when deletion fails with an error
     */
    <T extends Model> void delete(
            @NonNull Class<T> itemClass,
            @NonNull StorageItemChange.Initiator initiator,
            @NonNull QueryPredicate predicate,
            @NonNull Action onSuccess,
            @NonNull Consumer<DataStoreException> onError
    );

    /**
     * Batches operations for a given type in a single transaction.
     *
     * @param <T>        The type of items being saved/deleted
     * @param operations A list of operations that will sequentially execute
     * @param onComplete A callback that will be invoked when the transaction batch succeeds
     * @param onError    A callback that will be invoked when any operations fails with an error
     *                   If an error is received, the entire transaction will be unsuccessful
     */
    <T extends Model> void batchSyncOperations(
            @NonNull List<StorageOperation<T>> operations,
            @NonNull Action onComplete,
            @NonNull Consumer<DataStoreException> onError);

    /**
     * Observe all changes to that occur to any/all objects in the storage.
     * @param onItemChange
     *        Receives a {@link StorageItemChange} notification every time
     *        any object managed by the storage adapter is changed in any way.
     * @param onObservationError
     *        Invoked if the observation terminates do an unrecoverable error
     * @param onObservationComplete
     *        Invoked it the observation terminates gracefully, perhaps due to cancellation
     * @return A Cancelable with which this observation may be terminated
     */
    @NonNull
    Cancelable observe(
            @NonNull Consumer<StorageItemChange<? extends Model>> onItemChange,
            @NonNull Consumer<DataStoreException> onObservationError,
            @NonNull Action onObservationComplete
    );

    /**
     * Query and observe all changes to that occur to any/all objects in the storage.
     * @param itemClass class of the item being observed.
     * @param options query options.
     * @param onObservationStarted invoked on observation start.
     * @param onQuerySnapshot
     *        Receives a {@link StorageItemChange} notification every time
     *        any object managed by the storage adapter is changed in any way.
     * @param onObservationError
     *        Invoked if the observation terminates do an unrecoverable error.
     * @param onObservationComplete
     *        Invoked it the observation terminates gracefully, perhaps due to cancellation.
     *@param <T> The type of item being observed.
     */
    <T extends Model> void observeQuery(
            @NonNull Class<T> itemClass,
            @NonNull ObserveQueryOptions options,
            @NonNull Consumer<Cancelable> onObservationStarted,
            @NonNull Consumer<DataStoreQuerySnapshot<T>> onQuerySnapshot,
            @NonNull Consumer<DataStoreException> onObservationError,
            @NonNull Action onObservationComplete);

    /**
     * Terminate use of the local storage.
     * This should release all resources used by the implementation.
     * @throws DataStoreException if something goes wrong during terminate
     */
    void terminate() throws DataStoreException;

    /**
     * Each implementation of this adapter interface will have its own
     * interpretation of what clear means. The intent is to destroy
     * any DataStore-related artifacts and reset the adapter such that
     * it is usable by the DataStore plugin.
     * @param onComplete Invoked if the call is successful.
     * @param onError Invoked if an exception occurs.
     */
    void clear(@NonNull Action onComplete,
               @NonNull Consumer<DataStoreException> onError);
}
