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
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.plugin.PluginException;
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

import io.reactivex.Observable;

/**
 * An AWS implementation of the {@link DataStorePlugin}.
 */
public final class AWSDataStorePlugin implements DataStorePlugin<Void> {

    // Singleton instance
    private static AWSDataStorePlugin singleton;

    // Reference to an implementation of the Local Storage Adapter that
    // manages the persistence of data on-device.
    private final SQLiteStorageAdapter sqLiteStorageAdapter;

    private AWSDataStorePlugin() {
        sqLiteStorageAdapter = SQLiteStorageAdapter.defaultInstance();
    }

    /**
     * Return the singleton instance if it exists, otherwise create, assign
     * and return.
     * @return the singleton instance.
     */
    public static synchronized AWSDataStorePlugin singleton() {
        if (singleton == null) {
            singleton = new AWSDataStorePlugin();
        }
        return singleton;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPluginKey() {
        return "AWSDataStorePlugin";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(@NonNull JSONObject pluginConfiguration,
                          Context context) throws PluginException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Void getEscapeHatch() {
        return null;
    }

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
    public void initialize(
            @NonNull Context context,
            @NonNull ModelProvider modelProvider,
            @NonNull ResultListener<List<ModelSchema>> listener) {
        sqLiteStorageAdapter.initialize(context, modelProvider, listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void save(@NonNull T object,
                                       ResultListener<MutationEvent<T>> resultListener) {
        sqLiteStorageAdapter.save(object, resultListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void delete(@NonNull T object,
                                         ResultListener<MutationEvent<T>> resultListener) {
        sqLiteStorageAdapter.delete(object, resultListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void query(@NonNull Class<T> objectType,
                                        ResultListener<Iterator<T>> resultListener) {
        sqLiteStorageAdapter.query(objectType, resultListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<MutationEvent<? extends Model>> observe() {
        return sqLiteStorageAdapter.observe();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> Observable<MutationEvent<T>> observe(Class<T> modelClass) {
        return Observable.error(new DataStoreException("Not implemented yet, buster!"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> Observable<MutationEvent<T>> observe(
            Class<T> modelClass,
            String uniqueId) {
        return Observable.error(new DataStoreException("Not implemented yet, buster!"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> Observable<MutationEvent<T>> observe(
            Class<T> modelClass,
            QueryPredicate queryPredicate) {
        return Observable.error(new DataStoreException("Not implemented yet, buster!"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void terminate() {
        sqLiteStorageAdapter.terminate();
    }
}
