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
import androidx.annotation.Nullable;

import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

import io.reactivex.Completable;
import io.reactivex.Observable;

/**
 * DataStore simplifies local storage of your application data on the
 * device for offline access and automatically synchronizes data with
 * the cloud.The category is implemented by zero or more {@link DataStorePlugin}.
 * The operations made available by the category are defined in the
 * {@link DataStoreCategoryBehavior}.
 */
public class DataStoreCategory
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
    @NonNull
    @Override
    public <T extends Model> Completable save(@NonNull T object) {
        return getSelectedPlugin().save(object);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public <T extends Model> Completable delete(@NonNull T object) {
        return getSelectedPlugin().delete(object);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public <T extends Model> Observable<T> query(@NonNull Class<T> itemClass) {
        return getSelectedPlugin().query(itemClass);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public <T extends Model> Observable<T> query(@NonNull Class<T> itemClass, @Nullable QueryPredicate predicate) {
        return getSelectedPlugin().query(itemClass, predicate);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Observable<DataStoreItemChange<? extends Model>> observe() {
        return getSelectedPlugin().observe();
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public <T extends Model> Observable<DataStoreItemChange<T>> observe(@NonNull Class<T> itemClass) {
        return getSelectedPlugin().observe(itemClass);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public <T extends Model> Observable<DataStoreItemChange<T>> observe(
            @NonNull Class<T> itemClass,
            @NonNull String uniqueId) {
        return getSelectedPlugin().observe(itemClass, uniqueId);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public <T extends Model> Observable<DataStoreItemChange<T>> observe(
            @NonNull Class<T> itemClass,
            @NonNull QueryPredicate selectionCriteria) {
        return getSelectedPlugin().observe(itemClass, selectionCriteria);
    }
}
