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

package com.amplifyframework.datastore;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

import java.util.Iterator;
import java.util.List;

import io.reactivex.Observable;

/**
 * A DataStore is a high-level abstraction of an object repository.
 *
 * The DataStore can store items which extends {@link Model}, and are dressed with various
 * annotations, e.g. {@link ModelField}, etc. For a full discussion of Amplify model annotations,
 * see the <a href="https://todo.link">Amplify Android Model Annotations Documentation</a>.
 *
 * The DataStore is initialized by providing a collection of Models to be managed.
 * A result of this initialization is that the models are parsed for comprehension of
 * their schema. An implementation of the DataStore may use these schema to prepare
 * durable storage for the managed models.
 *
 * After this, items of the initialized model classes may be saved, deleted,
 * queried, and observed.
 *
 * When your are done using the DataStore, you should invoke {@link #terminate()} to free any
 * resources that may have been in use by the DataStore implementation.
 */
public interface DataStoreCategoryBehavior {

    /**
     * Initialize the DataStore with a collection of models that it will become able to store.
     * This call must be made before using any other method of the {@link DataStoreCategoryBehavior}.
     *
     * A {@link ModelProvider} provides a collection of {@link Model}. The ModelProvider
     * must provide a Model for each type of item you will store into this DataStore.
     *
     * A result of calling this initialization method is a callback that provides a list of
     * {@link ModelSchema}, one for each {@link Model} that was provided by the {@link ModelProvider}.
     *
     * An implementation of the {@link DataStoreCategoryBehavior} may initialize itself by creating
     * resources to house the various types of data requested.
     *
     * @param context Android application context
     * @param modelProvider Provides all types of Models that are to be usable by the DataStore
     * @param initializationResultListener
     *        An optional listener that will invoked when initialization succeeds or fails
     */
    void initialize(
            @NonNull Context context,
            @NonNull ModelProvider modelProvider,
            @Nullable ResultListener<List<ModelSchema>> initializationResultListener);

    /**
     * Saves an item into the DataStore.
     * @param item An item to save
     * @param saveItemListener
     *        An optional listener which will be callback'd when the save succeeds or fails
     * @param <T> The time of item being saved
     */
    <T extends Model> void save(
            @NonNull T item,
            @Nullable ResultListener<DataStoreItemChange<T>> saveItemListener);

    /**
     * Deletes an item from the DataStore.
     * @param item An item to delete from the DataStore
     * @param deleteItemListener
     *        An optional listener which will be invoked when the deletion succeeds or fails
     * @param <T> The type of item being deleted
     */
    <T extends Model> void delete(
            @NonNull T item,
            @Nullable ResultListener<DataStoreItemChange<T>> deleteItemListener);

    /**
     * Query the DataStore to find all items of the requested Java class.
     * @param itemClass Items of this class will be targeted by this query
     * @param queryResultsListener
     *        An optional listener which will be invoked when the query returns
     *        results, or if there is a failure to query
     * @param <T> The type of items being queried
     */
    <T extends Model> void query(
            @NonNull Class<T> itemClass,
            @Nullable ResultListener<Iterator<T>> queryResultsListener);

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
    <T extends Model> Observable<DataStoreItemChange<T>> observe(@NonNull Class<T> itemClass);

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
            @NonNull String uniqueId);

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
            @NonNull QueryPredicate selectionCriteria);

    /**
     * Terminate use of this instance of the DataStore.
     * An implementation may free any used resources at this time.
     */
    void terminate();
}
