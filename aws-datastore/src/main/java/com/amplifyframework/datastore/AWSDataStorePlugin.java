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
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
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
    private final LocalStorageAdapter sqliteStorageAdapter;

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
        return new SyncEngine(modelProvider, storageAdapter, AppSyncApi.instance());
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
            sqliteStorageAdapter.initialize(context, emitter::onSuccess, emitter::onError)
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
            @NonNull Consumer<DataStoreItemChange<T>> onItemSaved,
            @NonNull Consumer<DataStoreException> onFailureToSave) {
        save(item, null, onItemSaved, onFailureToSave);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void save(
            @NonNull T item,
            @Nullable QueryPredicate predicate,
            @NonNull Consumer<DataStoreItemChange<T>> onItemSaved,
            @NonNull Consumer<DataStoreException> onFailureToSave) {
        sqliteStorageAdapter.save(
            item,
            StorageItemChange.Initiator.DATA_STORE_API,
            predicate,
            recordOfSave -> {
                try {
                    onItemSaved.accept(toDataStoreItemChange(recordOfSave));
                } catch (DataStoreException dataStoreException) {
                    onFailureToSave.accept(dataStoreException);
                }
            },
            onFailureToSave
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void delete(
            @NonNull T item,
            @NonNull Consumer<DataStoreItemChange<T>> onItemDeleted,
            @NonNull Consumer<DataStoreException> onFailureToDelete) {
        sqliteStorageAdapter.delete(
            item,
            StorageItemChange.Initiator.DATA_STORE_API,
            recordOfDelete -> {
                try {
                    onItemDeleted.accept(toDataStoreItemChange(recordOfDelete));
                } catch (DataStoreException dataStoreException) {
                    onFailureToDelete.accept(dataStoreException);
                }
            },
            onFailureToDelete
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void query(
            @NonNull Class<T> itemClass,
            @NonNull Consumer<Iterator<T>> onQueryResults,
            @NonNull Consumer<DataStoreException> onQueryFailure) {
        sqliteStorageAdapter.query(itemClass, onQueryResults, onQueryFailure);
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
        sqliteStorageAdapter.query(itemClass, predicate, onQueryResults, onQueryFailure);
    }

    @NonNull
    @Override
    public Cancelable observe(
            @NonNull Consumer<DataStoreItemChange<? extends Model>> onDataStoreItemChange,
            @NonNull Consumer<DataStoreException> onObservationFailure,
            @NonNull Action onObservationCompleted) {
        return sqliteStorageAdapter.observe(
            storageItemChangeRecord -> {
                try {
                    onDataStoreItemChange.accept(toDataStoreItemChange(storageItemChangeRecord));
                } catch (DataStoreException dataStoreException) {
                    onObservationFailure.accept(dataStoreException);
                }
            },
            onObservationFailure,
            onObservationCompleted
        );
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable observe(
            @NonNull Class<T> itemClass,
            @NonNull Consumer<DataStoreItemChange<T>> onDataStoreItemChange,
            @NonNull Consumer<DataStoreException> onObservationFailure,
            @NonNull Action onObservationCompleted) {
        return sqliteStorageAdapter.observe(
            storageItemChangeRecord -> {
                try {
                    if (!storageItemChangeRecord.getItemClass().equals(itemClass.getName())) {
                        return;
                    }
                    onDataStoreItemChange.accept(toDataStoreItemChange(storageItemChangeRecord));
                } catch (DataStoreException dataStoreException) {
                    onObservationFailure.accept(dataStoreException);
                }
            },
            onObservationFailure,
            onObservationCompleted
        );
    }

    @NonNull
    @Override
    public <T extends Model> Cancelable observe(
            @NonNull Class<T> itemClass,
            @NonNull String uniqueId,
            @NonNull Consumer<DataStoreItemChange<T>> onDataStoreItemChange,
            @NonNull Consumer<DataStoreException> onObservationFailure,
            @NonNull Action onObservationCompleted) {
        return sqliteStorageAdapter.observe(
            storageItemChangeRecord -> {
                try {
                    final DataStoreItemChange<T> dataStoreItemChange =
                        toDataStoreItemChange(storageItemChangeRecord);
                    if (!dataStoreItemChange.itemClass().equals(itemClass) ||
                            !uniqueId.equals(dataStoreItemChange.item().getId())) {
                        return;
                    }
                    onDataStoreItemChange.accept(dataStoreItemChange);
                } catch (DataStoreException dataStoreException) {
                    onObservationFailure.accept(dataStoreException);
                }
            },
            onObservationFailure,
            onObservationCompleted
        );
    }

    @SuppressWarnings("checkstyle:WhitespaceAround") // () -> {}
    @NonNull
    @Override
    public <T extends Model> Cancelable observe(
            @NonNull Class<T> itemClass,
            @NonNull QueryPredicate selectionCriteria,
            @NonNull Consumer<DataStoreItemChange<T>> onDataStoreItemChange,
            @NonNull Consumer<DataStoreException> onObservationFailure,
            @NonNull Action onObservationCompleted) {
        onObservationFailure.accept(new DataStoreException("Not implemented yet, buster!", "Check back later!"));
        return () -> {};
    }

    /**
     * Converts an {@link StorageItemChange.Record}, as recevied by the {@link LocalStorageAdapter}'s
     * {@link LocalStorageAdapter#save(Model, StorageItemChange.Initiator, Consumer, Consumer)} and
     * {@link LocalStorageAdapter#delete(Model, StorageItemChange.Initiator, Consumer, Consumer)} methods'
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
}
