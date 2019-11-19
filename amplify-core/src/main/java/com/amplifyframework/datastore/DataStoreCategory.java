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

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

import java.util.Iterator;
import java.util.List;

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
    @Override
    public CategoryType getCategoryType() {
        return CategoryType.DATASTORE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUp(@NonNull Context context,
                      @NonNull ModelProvider modelProvider,
                      @NonNull ResultListener<List<ModelSchema>> listener) {
        getSelectedPlugin().setUp(context, modelProvider, listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void save(@NonNull T object,
                                       ResultListener<MutationEvent<T>> resultListener) {
        getSelectedPlugin().save(object, resultListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void delete(@NonNull T object,
                                         ResultListener<MutationEvent<T>> resultListener) {
        getSelectedPlugin().delete(object, resultListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void query(@NonNull Class<T> objectType,
                                        ResultListener<Iterator<T>> resultListener) {
        getSelectedPlugin().query(objectType, resultListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<MutationEvent<? extends Model>> observe() {
        return getSelectedPlugin().observe();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> Observable<MutationEvent<T>> observe(Class<T> modelClass) {
        return getSelectedPlugin().observe(modelClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> Observable<MutationEvent<T>> observe(
            Class<T> modelClass,
            String uniqueId) {
        return getSelectedPlugin().observe(modelClass, uniqueId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> Observable<MutationEvent<T>> observe(
            Class<T> modelClass,
            QueryPredicate queryPredicate) {
        return getSelectedPlugin().observe(modelClass, queryPredicate);
    }
}
