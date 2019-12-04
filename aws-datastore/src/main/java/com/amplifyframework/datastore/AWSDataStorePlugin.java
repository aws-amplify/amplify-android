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
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.network.AppSyncApi;
import com.amplifyframework.datastore.network.SyncEngine;
import com.amplifyframework.datastore.storage.GsonStorageItemChangeConverter;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * An AWS implementation of the {@link DataStorePlugin}.
 */
public final class AWSDataStorePlugin implements DataStorePlugin<Void> {
    // Reference to an implementation of the Local Storage Adapter that
    // manages the persistence of data on-device.
    private final SQLiteStorageAdapter sqliteStorageAdapter;

    // A utility to convert between StorageItemChange.Record and StorageItemChange
    private final GsonStorageItemChangeConverter storageItemChangeConverter;

    // A component which synchronizes data state between the
    // local storage adapter, and a remote API
    private final SyncEngine syncEngine;

    // Configuration for the plugin.
    private AWSDataStorePluginConfiguration pluginConfiguration;

    private AWSDataStorePlugin(@NonNull final ModelProvider modelProvider) {
        this.sqliteStorageAdapter = SQLiteStorageAdapter.forModels(modelProvider);
        this.storageItemChangeConverter = new GsonStorageItemChangeConverter();
        this.syncEngine = createSyncEngine(modelProvider, sqliteStorageAdapter);
    }

    private SyncEngine createSyncEngine(ModelProvider modelProvider, LocalStorageAdapter storageAdapter) {
        return new SyncEngine(modelProvider, storageAdapter, new AppSyncApi(Amplify.API));
    }

    /**
     * Return the instance for the model provider.
     * @param modelProvider Provider of models to be usable by plugin
     * @return the plugin instance for the model provider.
     */
    @NonNull
    @SuppressWarnings("WeakerAccess")
    public static synchronized AWSDataStorePlugin forModels(@NonNull final ModelProvider modelProvider) {
        return new AWSDataStorePlugin(modelProvider);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public String getPluginKey() {
        return "awsDataStorePlugin";
    }

    /**
     * {@inheritDoc}
     */
    @SuppressLint("CheckResult")
    @Override
    public void configure(
            @Nullable JSONObject pluginConfigurationJson,
            @NonNull Context context) throws DataStoreException {
        try {
            this.pluginConfiguration =
                AWSDataStorePluginConfiguration.fromJson(pluginConfigurationJson);
        } catch (DataStoreException badConfigException) {
            throw new DataStoreException(
                "There was an issue configuring the plugin from the amplifyconfiguration.json",
                    badConfigException,
                    "Check the attached exception for more details and " +
                    "be sure you are only calling Amplify.configure once"
            );
        }

        //noinspection ResultOfMethodCallIgnored
        initializeStorageAdapter(context)
            .doOnSuccess(modelSchemas -> startModelSynchronization(pluginConfiguration.getSyncMode()))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .blockingGet();
    }

    private void startModelSynchronization(AWSDataStorePluginConfiguration.SyncMode syncMode) {
        if (AWSDataStorePluginConfiguration.SyncMode.SYNC_WITH_API.equals(syncMode)) {
            syncEngine.start();
        }
    }

    /**
     * Initializes the storage adapter, and gets the result as a {@link Single}.
     * @param context An Android Context
     * @return A single which will initialize the storage adapter when subscribed.
     *         Single completes successfully by emitting the list of model schema
     *         that will be managed by the storage adapter. Single completed with
     *         error by emitting an error via {@link SingleEmitter#onError(Throwable)}.
     */
    @WorkerThread
    private Single<List<ModelSchema>> initializeStorageAdapter(Context context) {
        return Single.defer(() -> Single.create(emitter ->
            sqliteStorageAdapter.initialize(context, new ResultListener<List<ModelSchema>>() {
                @Override
                public void onResult(List<ModelSchema> modelSchema) {
                    emitter.onSuccess(modelSchema);
                }

                @Override
                public void onError(Throwable error) {
                    emitter.onError(error);
                }
            })
        ));
    }

    /**
     * Terminate use of the plugin.
     */
    synchronized void terminate() throws DataStoreException {
        syncEngine.stop();
        sqliteStorageAdapter.terminate();
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public Void getEscapeHatch() {
        return null;
    }

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
            @NonNull ResultListener<DataStoreItemChange<T>> saveItemListener) {
        sqliteStorageAdapter.save(item, StorageItemChange.Initiator.DATA_STORE_API,
            new ResultConversionListener<>(saveItemListener, this::toDataStoreItemChange));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void delete(
            @NonNull T item,
            @NonNull ResultListener<DataStoreItemChange<T>> deleteItemListener) {
        sqliteStorageAdapter.delete(item, StorageItemChange.Initiator.DATA_STORE_API,
            new ResultConversionListener<>(deleteItemListener, this::toDataStoreItemChange));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void query(
            @NonNull Class<T> itemClass,
            @NonNull ResultListener<Iterator<T>> queryResultsListener) {
        sqliteStorageAdapter.query(itemClass, queryResultsListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void query(
            @NonNull Class<T> itemClass,
            @Nullable QueryPredicate predicate,
            @NonNull ResultListener<Iterator<T>> queryResultsListener) {
        sqliteStorageAdapter.query(itemClass, predicate, queryResultsListener);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Observable<DataStoreItemChange<? extends Model>> observe() {
        return sqliteStorageAdapter.observe()
            .map(this::toDataStoreItemChange);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public <T extends Model> Observable<DataStoreItemChange<T>> observe(@NonNull Class<T> itemClass) {
        return sqliteStorageAdapter.observe()
            .filter(record -> record.getItemClass().equals(itemClass.getName()))
            .map(this::toDataStoreItemChange);
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
        return Observable.error(new DataStoreException("Not implemented yet, buster!", "Check back later!"));
    }

    /**
     * Converts an {@link StorageItemChange.Record}, as recevied by the {@link LocalStorageAdapter}'s
     * {@link LocalStorageAdapter#save(Model, StorageItemChange.Initiator, ResultListener)} and
     * {@link LocalStorageAdapter#delete(Model, StorageItemChange.Initiator, ResultListener)} methods'
     * callbacks, into an {@link DataStoreItemChange}, which can be returned via the public DataStore API.
     * @param record A record of change in the storage layer
     * @param <T> Type of data that was changed
     * @return A {@link DataStoreItemChange} representing the storage change record
     */
    private <T extends Model> DataStoreItemChange<T> toDataStoreItemChange(final StorageItemChange.Record record)
        throws DataStoreException {
        return toDataStoreItemChange(record.toStorageItemChange(storageItemChangeConverter));
    }

    /**
     * Converts an {@link StorageItemChange} into an {@link DataStoreItemChange}.
     * @param storageItemChange A storage item change
     * @param <T> Type of data that was changed in the storage layer
     * @return A data store item change representing the change in storage layer
     */
    private static <T extends Model> DataStoreItemChange<T> toDataStoreItemChange(
            final StorageItemChange<T> storageItemChange) throws DataStoreException {

        final DataStoreItemChange.Initiator dataStoreItemChangeInitiator;
        switch (storageItemChange.initiator()) {
            case SYNC_ENGINE:
                dataStoreItemChangeInitiator = DataStoreItemChange.Initiator.REMOTE;
                break;
            case DATA_STORE_API:
                dataStoreItemChangeInitiator = DataStoreItemChange.Initiator.LOCAL;
                break;
            default:
                throw new DataStoreException(
                        "Unknown initiator of storage change: " + storageItemChange.initiator(),
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                );
        }

        final DataStoreItemChange.Type dataStoreItemChangeType;
        switch (storageItemChange.type()) {
            case DELETE:
                dataStoreItemChangeType = DataStoreItemChange.Type.SAVE;
                break;
            case SAVE:
                dataStoreItemChangeType = DataStoreItemChange.Type.DELETE;
                break;
            default:
                throw new DataStoreException(
                        "Unknown type of storage change: " + storageItemChange.type(),
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                );
        }

        return DataStoreItemChange.<T>builder()
            .initiator(dataStoreItemChangeInitiator)
            .item(storageItemChange.item())
            .itemClass(storageItemChange.itemClass())
            .type(dataStoreItemChangeType)
            .uuid(storageItemChange.changeId().toString())
            .build();
    }

    /**
     * A listener of the {@link LocalStorageAdapter} APIs, which responds by
     * converting received {@link StorageItemChange.Record} into {@link DataStoreItemChange}s,
     * and emitting those on the provided data store item change listener.
     * using the provided {@link ConversionStrategy}.
     * @param <T> Type of data in the emitted DataStoreItemChange
     */
    static final class ResultConversionListener<T extends Model> implements ResultListener<StorageItemChange.Record> {
        private final ResultListener<DataStoreItemChange<T>> dataStoreListener;
        private final ConversionStrategy conversionStrategy;

        /**
         * Constructs a new ResultConversionListener.
         * @param dataStoreListener listener object of the public DataStore API
         * @param conversionStrategy A strategy to convert between storage adapter and data store api types
         */
        ResultConversionListener(
                ResultListener<DataStoreItemChange<T>> dataStoreListener,
                ConversionStrategy conversionStrategy) {
            this.dataStoreListener = dataStoreListener;
            this.conversionStrategy = conversionStrategy;
        }

        @Override
        public void onResult(StorageItemChange.Record result) {
            try {
                DataStoreItemChange<T> converted = conversionStrategy.convert(result);
                dataStoreListener.onResult(converted);
            } catch (DataStoreException exception) {
                onError(exception);
            }
        }

        @Override
        public void onError(Throwable error) {
            dataStoreListener.onError(new DataStoreException(
                    "Oof, something went wrong.",
                    error,
                    "Check the attached error for details."
            ));
        }

        /**
         * A strategy to convert an object from the {@link LocalStorageAdapter} into
         * a {@link DataStoreItemChange} as exposed by the publid {@link DataStoreCategoryBehavior}.
         */
        interface ConversionStrategy {
            /**
             * Converts a record of change in the {@link LocalStorageAdapter}
             * (a {@link StorageItemChange.Record}) into a {@link DataStoreItemChange}.
             * @param record Record to convert
             * @param <T> Type of data contained inside the emitted {@link DataStoreItemChange}
             * @return An {@link DataStoreItemChange}, usable by the DataStore APIs
             */
            <T extends Model> DataStoreItemChange<T> convert(StorageItemChange.Record record) throws DataStoreException;
        }
    }
}
