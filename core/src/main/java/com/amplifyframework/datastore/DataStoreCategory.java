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

import androidx.annotation.NonNull;

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

import java.util.Iterator;

/**
 * DataStore simplifies local storage of your application data on the
 * device for offline access and automatically synchronizes data with
 * the cloud.The category is implemented by zero or more {@link DataStorePlugin}.
 * The operations made available by the category are defined in the
 * {@link DataStoreCategoryBehavior}.
 */
public final class DataStoreCategory
        extends Category<DataStorePlugin<?>>
        implements DataStoreCategoryBehavior {

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public CategoryType getCategoryType() {
        return CategoryType.DATASTORE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void save(
            @NonNull T item,
            @NonNull Consumer<DataStoreItemChange<T>> onItemSaved,
            @NonNull Consumer<DataStoreException> onFailureToSave) {
        getSelectedPlugin().save(item, onItemSaved, onFailureToSave);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void save(
            @NonNull T item,
            @NonNull QueryPredicate predicate,
            @NonNull Consumer<DataStoreItemChange<T>> onItemSaved,
            @NonNull Consumer<DataStoreException> onFailureToSave) {
        getSelectedPlugin().save(item, predicate, onItemSaved, onFailureToSave);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void delete(
            @NonNull T item,
            @NonNull Consumer<DataStoreItemChange<T>> onItemDeleted,
            @NonNull Consumer<DataStoreException> onFailureToDelete) {
        getSelectedPlugin().delete(item, onItemDeleted, onFailureToDelete);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void delete(
            @NonNull T object,
            @NonNull QueryPredicate predicate,
            @NonNull Consumer<DataStoreItemChange<T>> onItemDeleted,
            @NonNull Consumer<DataStoreException> onFailureToDelete) {
        getSelectedPlugin().delete(object, predicate, onItemDeleted, onFailureToDelete);
    }

    @Override
    public <T extends Model> void delete(
            @NonNull Class<T> objectClass,
            @NonNull QueryPredicate predicate,
            @NonNull Action onItemsDeleted,
            @NonNull Consumer<DataStoreException> onFailureToDelete) {
        getSelectedPlugin().delete(objectClass, predicate, onItemsDeleted, onFailureToDelete);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void query(
            @NonNull Class<T> itemClass,
            @NonNull Consumer<Iterator<T>> onQueryResults,
            @NonNull Consumer<DataStoreException> onQueryFailure) {
        getSelectedPlugin().query(itemClass, onQueryResults, onQueryFailure);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void query(
            @NonNull Class<T> itemClass,
            @NonNull QueryPredicate predicate,
            @NonNull Consumer<Iterator<T>> onQueryResults,
            @NonNull Consumer<DataStoreException> onQueryFailure) {
        getSelectedPlugin().query(itemClass, predicate, onQueryResults, onQueryFailure);
    }

    @Override
    public <T extends Model> void query(
            @NonNull Class<T> itemClass,
            @NonNull QueryOptions options,
            @NonNull Consumer<Iterator<T>> onQueryResults,
            @NonNull Consumer<DataStoreException> onQueryFailure) {
        getSelectedPlugin().query(itemClass, options, onQueryResults, onQueryFailure);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void observe(
            @NonNull Consumer<Cancelable> onObservationStarted,
            @NonNull Consumer<DataStoreItemChange<? extends Model>> onDataStoreItemChange,
            @NonNull Consumer<DataStoreException> onObservationFailure,
            @NonNull Action onObservationCompleted) {
        getSelectedPlugin().observe(
            onObservationStarted, onDataStoreItemChange, onObservationFailure, onObservationCompleted);
    }

    @Override
    public void observeQuery(@NonNull Consumer<Cancelable> onObservationStarted, @NonNull Consumer<DataStoreItemChange<? extends Model>> onDataStoreItemChange, @NonNull Consumer<DataStoreException> onObservationFailure, @NonNull Action onObservationCompleted) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void observe(
            @NonNull Class<T> itemClass,
            @NonNull Consumer<Cancelable> onObservationStarted,
            @NonNull Consumer<DataStoreItemChange<T>> onDataStoreItemChange,
            @NonNull Consumer<DataStoreException> onObservationFailure,
            @NonNull Action onObservationCompleted) {
        getSelectedPlugin().observe(itemClass,
            onObservationStarted, onDataStoreItemChange, onObservationFailure, onObservationCompleted);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void observe(
            @NonNull Class<T> itemClass,
            @NonNull String uniqueId,
            @NonNull Consumer<Cancelable> onObservationStarted,
            @NonNull Consumer<DataStoreItemChange<T>> onDataStoreItemChange,
            @NonNull Consumer<DataStoreException> onObservationFailure,
            @NonNull Action onObservationCompleted) {
        getSelectedPlugin().observe(itemClass, uniqueId,
            onObservationStarted, onDataStoreItemChange, onObservationFailure, onObservationCompleted);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void observe(
            @NonNull Class<T> itemClass,
            @NonNull QueryPredicate selectionCriteria,
            @NonNull Consumer<Cancelable> onObservationStarted,
            @NonNull Consumer<DataStoreItemChange<T>> onDataStoreItemChange,
            @NonNull Consumer<DataStoreException> onObservationFailure,
            @NonNull Action onObservationCompleted) {
        getSelectedPlugin().observe(itemClass, selectionCriteria,
            onObservationStarted, onDataStoreItemChange, onObservationFailure, onObservationCompleted);
    }

    @Override
    public void start(@NonNull Action onComplete, @NonNull Consumer<DataStoreException> onError) {
        getSelectedPlugin().start(onComplete, onError);
    }

    @Override
    public void stop(@NonNull Action onComplete, @NonNull Consumer<DataStoreException> onError) {
        getSelectedPlugin().stop(onComplete, onError);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear(@NonNull Action onComplete, @NonNull Consumer<DataStoreException> onError) {
        getSelectedPlugin().clear(onComplete, onError);
    }
}
