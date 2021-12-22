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

package com.amplifyframework.rx;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.ObserveQueryOptions;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreCategoryBehavior;
import com.amplifyframework.datastore.DataStoreItemChange;
import com.amplifyframework.datastore.DataStoreQuerySnapshot;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;

/**
 * An Rx-idiomatic expression of the behaviors in {@link DataStoreCategoryBehavior}.
 */
public interface RxDataStoreCategoryBehavior {

    /**
     * Saves an item into the DataStore.
     * @param <T> The time of item being saved
     * @param item An item to save
     * @return A {@link Completable} which completes on success, emits error on error
     */
    @NonNull
    <T extends Model> Completable save(
            @NonNull T item
    );

    /**
     * Saves an item into the DataStore, considering a predicate.
     * @param <T> The time of item being saved
     * @param item An item to save
     * @param predicate Predicate condition to apply for conditional write
     * @return A {@link Completable} which completes on success, emits error on error
     */
    @NonNull
    <T extends Model> Completable save(
            @NonNull T item,
            @NonNull QueryPredicate predicate
    );

    /**
     * Deletes an item from the DataStore.
     * @param <T> The type of item being deleted
     * @param item An item to delete from the DataStore
     * @return A {@link Completable} which completes on success, emits error on error
     */
    @NonNull
    <T extends Model> Completable delete(
            @NonNull T item
    );

    /**
     * Deletes an item from the DataStore, considering a predicate.
     * @param <T> The type of item being deleted
     * @param item An item to delete from the DataStore
     * @param predicate Predicate condition to apply for conditional write
     * @return A {@link Completable} which completes on success, emits error on error
     */
    @NonNull
    <T extends Model> Completable delete(
            @NonNull T item,
            @NonNull QueryPredicate predicate
    );

    /**
     * Deletes item from the DataStore, filtered by a predicate.
     * @param <T> The type of item being deleted
     * @param itemClass Item type to delete from the DataStore
     * @param predicate Predicate condition to filter items
     * @return A {@link Completable} which completes on success, emits error on error
     */
    @NonNull
    <T extends Model> Completable delete(
            @NonNull Class<T> itemClass,
            @NonNull QueryPredicate predicate
    );

    /**
     * Query the DataStore to find all items of the requested Java class.
     * @param itemClass Items of this class will be targeted by this query
     * @param <T> The type of items being queried
     * @return An observable stream of 0..n query results, if available.
     *         The Observable will then terminate either either a completion or error.
     */
    @NonNull
    <T extends Model> Observable<T> query(
            @NonNull Class<T> itemClass
    );

    /**
     * Query the DataStore to find all items of the requested Java class that fulfills the
     * predicate.
     * @param itemClass Items of this class will be targeted by this query
     * @param predicate Predicate condition to apply to query
     * @param <T> The type of items being queried
     * @return An observable stream of 0..n query results, if available.
     *         The Observable will then terminate either either a completion or error.
     */
    @NonNull
    <T extends Model> Observable<T> query(
            @NonNull Class<T> itemClass,
            @NonNull QueryPredicate predicate
    );

    /**
     * Query the DataStore to find items of the requested Java class, using the provided
     * {@link QueryOptions}. The query options include support for filtering, paging and sorting.
     * @param itemClass Class of items that will be queried
     * @param options Filtering, paging, and sorting options
     * @param <T> The type of items being queried
     * @return An observable stream of 0..n query results, if available.
     *         The Observable will then terminate either either a completion or error.
     */
    @NonNull
    <T extends Model> Observable<T> query(
            @NonNull Class<T> itemClass,
            @NonNull QueryOptions options
    );

    /**
     * Observe all changes to any/all item(s) in the DataStore.
     * @return An observable stream of {@link DataStoreItemChange}s,
     *         one for each and every change that occurs to any/all item(s)
     *         in the DataStore.
     */
    @NonNull
    Observable<DataStoreItemChange<? extends Model>> observe();

    /**
     * Observe changes to a certain type of item(s) in the DataStore.
     * @param itemClass The class of the item(s) to observe
     * @param <T> The type of the item(s) to observe
     * @return An observable stream of {@link DataStoreItemChange}s, that
     *         will emit a new {@link DataStoreItemChange} whenever there there are
     *         changes to any item of the requested item class.
     */
    @NonNull
    <T extends Model> Observable<DataStoreItemChange<T>> observe(
            @NonNull Class<T> itemClass
    );

    /**
     * Observe changes to a specific item, identified by its class and unique ID.
     * @param itemClass The class of the item being observed
     * @param uniqueId The unique ID of the item being observed
     * @param <T> The type of item being observed
     * @return A stream of {@link DataStoreItemChange} events, specific to a single item
     *         which was uniquely identified by the provided class and unique id.
     *         Note that this stream may emit a non-trivial number of events, in case the
     *         selected item is updated many times.
     */
    @NonNull
    <T extends Model> Observable<DataStoreItemChange<T>> observe(
            @NonNull Class<T> itemClass,
            @NonNull String uniqueId
    );

    /**
     * Observe a collection of item(s) that have a specified class type, and that match
     * additional criteria, specified by a fluent chain of selection operators.
     * @param itemClass The class of item(s) to observe
     * @param selectionCriteria
     *        Additional criteria which will be considered when identifying which
     *        items in the DataStore should be observed for changes.
     * @param <T> The type of the item(s) to observe
     * @return An observable stream of {@link DataStoreItemChange}s, emitted for items that are
     *         of the requested class, and that match the provided selection criteria
     */
    @NonNull
    <T extends Model> Observable<DataStoreItemChange<T>> observe(
            @NonNull Class<T> itemClass,
            @NonNull QueryPredicate selectionCriteria
    );

    /***
     * Observe query returns the items from local datastore when you subscribe, then returns
     * mutations happening in the datastore.
     * @param itemClass class of the item being observed.
     * @param options ObserveQueryOptions.
     * @param <T> Type which extends Model.
     * @return An observable stream of {@link DataStoreQuerySnapshot}s, emitted for items that are
     * of the requested class, and that match the provided selection criteria
     */
    @NonNull
    <T extends Model> Observable<DataStoreQuerySnapshot<T>>
            observeQuery(@NonNull Class<T> itemClass, ObserveQueryOptions options);

    /**
     * Starts the DataStore.  This only needs to be called if you wish to start eagerly.  If you don't call it,
     * it will be called automatically prior to executing any other operations (#query, #save, #delete, #observe).
     *
     * @return A {@link Completable} which emits success after DataStore is initialized.  This does not block until
     * subscriptions and sync are complete.  To block until sync and subscriptions are complete, use
     * Amplify.Hub.subscribe to listen for the DataStoreChannelEventName.READY event on HubChannel.DATASTORE.
     */
    Completable start();

    /**
     * Stops the underlying DataStore, resetting the plugin to the initialized state.
     *
     *  @return A {@link Completable} which emits success upon successful stop of DataStore, emits an error, otherwise
     */
    Completable stop();

    /**
     * Resets the underlying DataStore to its pre-initialized state such that no data remains on the local
     * device. Every implementation of this behavior may have its own interpretation of what clear means.
     * This is meant to be a destructive operation that allows for safe disposal of data stored locally.
     * @return A {@link Completable} which emits success upon successful clear of DataStore,
     *         emits an error, otherwise
     */
    Completable clear();
}
