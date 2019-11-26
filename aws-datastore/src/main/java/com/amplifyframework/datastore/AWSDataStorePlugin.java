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

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;

import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.plugin.PluginException;
import com.amplifyframework.datastore.network.SyncEngine;
import com.amplifyframework.datastore.storage.GsonStorageItemChangeConverter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * An AWS implementation of the {@link DataStorePlugin}.
 */
public final class AWSDataStorePlugin implements DataStorePlugin<Void> {
    // Singleton instance
    private static AWSDataStorePlugin singleton;

    // Reference to an implementation of the Local Storage Adapter that
    // manages the persistence of data on-device.
    private final SQLiteStorageAdapter sqliteStorageAdapter;

    // A utility to convert between StorageItemChange.Record and StorageItemChange
    private final GsonStorageItemChangeConverter storageItemChangeConverter;

    // Configuration for the plugin.
    private AWSDataStorePluginConfiguration pluginConfiguration;

    // A component which synchronizes data state between the
    // local storage adapter, and a remote API
    private SyncEngine syncEngine;

    private AWSDataStorePlugin(@NonNull final ModelProvider modelProvider) {
        this.sqliteStorageAdapter = SQLiteStorageAdapter.forModels(modelProvider);
        this.storageItemChangeConverter = new GsonStorageItemChangeConverter();
        this.syncEngine = null;
    }

    /**
     * Return the singleton instance if it exists, otherwise create, assign
     * and return.
     * @param modelProvider Provider of models to be usable by plugin
     * @return the singleton instance.
     */
    @SuppressWarnings("WeakerAccess")
    public static synchronized AWSDataStorePlugin singleton(@NonNull final ModelProvider modelProvider) {
        if (singleton == null) {
            singleton = new AWSDataStorePlugin(modelProvider);
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
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("CheckResult")
    @Override
    public void configure(
            @NonNull JSONObject pluginConfiguration,
            @NonNull Context context) throws PluginException {
        try {
            this.pluginConfiguration = AWSDataStorePluginConfiguration.fromJson(pluginConfiguration);
        } catch (JSONException badConfigException) {
            throw new PluginException.PluginConfigurationException(badConfigException);
        }

        Single.<List<ModelSchema>>create(emitter ->
            sqliteStorageAdapter.initialize(context, new ResultListener<List<ModelSchema>>() {
                @Override
                public void onResult(List<ModelSchema> modelSchema) {
                    emitter.onSuccess(modelSchema);
                }

                @Override
                public void onError(Throwable error) {
                    emitter.onError(error);
                }
            }))
            .doOnSuccess(this::startModelSynchronization)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .blockingGet();
    }

    /**
     * Terminate use of the plugin.
     */
    synchronized void terminate() {
        syncEngine.stop();
        sqliteStorageAdapter.terminate();
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
    public <T extends Model> void save(
            @NonNull T item,
            ResultListener<DataStoreItemChange<T>> saveItemListener) {
        sqliteStorageAdapter.save(
            item,
            StorageItemChange.Initiator.DATA_STORE_API,
            new ResultListener<StorageItemChange.Record>() {
                @Override
                public void onResult(StorageItemChange.Record result) {
                    StorageItemChange<T> storageItemChange =
                        result.toStorageItemChange(storageItemChangeConverter);
                    saveItemListener.onResult(DataStoreItemChange.<T>builder()
                        .uuid(storageItemChange.changeId().toString())
                        .initiator(toDataStoreItemChangeInitiator(storageItemChange.initiator()))
                        .item(storageItemChange.item())
                        .itemClass(storageItemChange.itemClass())
                        .type(toDataStoreItemChangeType(storageItemChange.type()))
                        .build());
                }

                @Override
                public void onError(Throwable error) {
                    saveItemListener.onError(error);
                }
            }
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void delete(@NonNull T item,
                                         ResultListener<DataStoreItemChange<T>> deleteItemListener) {
        sqliteStorageAdapter.delete(
            item,
            StorageItemChange.Initiator.DATA_STORE_API,
            new ResultListener<StorageItemChange.Record>() {
                @Override
                public void onResult(StorageItemChange.Record result) {
                    StorageItemChange<T> storageItemChange =
                        result.toStorageItemChange(storageItemChangeConverter);
                    deleteItemListener.onResult(DataStoreItemChange.<T>builder()
                        .uuid(storageItemChange.changeId().toString())
                        .type(toDataStoreItemChangeType(storageItemChange.type()))
                        .initiator(toDataStoreItemChangeInitiator(storageItemChange.initiator()))
                        .item(storageItemChange.item())
                        .itemClass(storageItemChange.itemClass())
                        .build());
                }

                @Override
                public void onError(Throwable error) {
                    deleteItemListener.onError(error);
                }
            }
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void query(
            @NonNull Class<T> itemClass,
            ResultListener<Iterator<T>> queryResultsListener) {
        sqliteStorageAdapter.query(itemClass, queryResultsListener);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Observable<DataStoreItemChange<? extends Model>> observe() {
        return sqliteStorageAdapter.observe()
            .map(record -> record.toStorageItemChange(storageItemChangeConverter))
            .map(storageItemChange -> DataStoreItemChange.builder()
                .initiator(toDataStoreItemChangeInitiator(storageItemChange.initiator()))
                .item(storageItemChange.item())
                .itemClass(storageItemChange.itemClass())
                .type(toDataStoreItemChangeType(storageItemChange.type()))
                .uuid(storageItemChange.changeId().toString())
                .build());
    }

    private DataStoreItemChange.Initiator toDataStoreItemChangeInitiator(
            StorageItemChange.Initiator storageItemChangeInitiator) {
        switch (storageItemChangeInitiator) {
            case SYNC_ENGINE:
                return DataStoreItemChange.Initiator.REMOTE;
            case DATA_STORE_API:
                return DataStoreItemChange.Initiator.LOCAL;
            default:
                throw new DataStoreException("Unknown initiator of storage change: " + storageItemChangeInitiator);
        }
    }

    private DataStoreItemChange.Type toDataStoreItemChangeType(
            StorageItemChange.Type storageItemChangeType) {
        switch (storageItemChangeType) {
            case DELETE:
                return DataStoreItemChange.Type.SAVE;
            case SAVE:
                return DataStoreItemChange.Type.DELETE;
            default:
                throw new DataStoreException("Unknown type of storage change: " + storageItemChangeType);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T extends Model> Observable<DataStoreItemChange<T>> observe(@NonNull Class<T> itemClass) {
        return sqliteStorageAdapter.observe()
                .map(record -> record.toStorageItemChange(storageItemChangeConverter))
                .filter(storageItemChange -> itemClass.equals(storageItemChange.itemClass()))
                .map(storageItemChange -> DataStoreItemChange.<T>builder()
                        .initiator(toDataStoreItemChangeInitiator(storageItemChange.initiator()))
                        .item((T) storageItemChange.item())
                        .itemClass((Class<T>) storageItemChange.itemClass())
                        .type(toDataStoreItemChangeType(storageItemChange.type()))
                        .uuid(storageItemChange.changeId().toString())
                        .build());
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public <T extends Model> Observable<DataStoreItemChange<T>> observe(
            @NonNull Class<T> itemClass,
            @NonNull String uniqueId) {
        return observe(itemClass)
                .filter(dataStoreItemChange -> uniqueId.equals(dataStoreItemChange.item().getId()));
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public <T extends Model> Observable<DataStoreItemChange<T>> observe(
            @NonNull Class<T> itemClass,
            @NonNull QueryPredicate selectionCriteria) {
        return Observable.error(new DataStoreException("Not implemented yet, buster!"));
    }

    private void startModelSynchronization(
            @SuppressWarnings("unused" /* oh , it will be! */) final List<ModelSchema> schema) {
        if (AWSDataStorePluginConfiguration.SyncMode.SYNC_WITH_API.equals(pluginConfiguration.getSyncMode())) {
            final ApiCategoryBehavior api = Amplify.API;
            final String apiName = pluginConfiguration.getApiName();
            this.syncEngine = new SyncEngine(/* use schema, shortly ... */ api, apiName, sqliteStorageAdapter);
            this.syncEngine.start();
        }
    }
}
