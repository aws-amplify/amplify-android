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
import com.amplifyframework.api.GraphQlBehavior;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.InitializationStatus;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.appsync.AppSyncClient;
import com.amplifyframework.datastore.storage.GsonStorageItemChangeConverter;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter;
import com.amplifyframework.datastore.syncengine.Orchestrator;
import com.amplifyframework.hub.HubChannel;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import io.reactivex.Completable;

/**
 * An AWS implementation of the {@link DataStorePlugin}.
 */
public final class AWSDataStorePlugin extends DataStorePlugin<Void> {
    // Reference to an implementation of the Local Storage Adapter that
    // manages the persistence of data on-device.
    private final LocalStorageAdapter sqliteStorageAdapter;

    // A utility to convert between StorageItemChange.Record and StorageItemChange
    private final GsonStorageItemChangeConverter storageItemChangeConverter;

    // A component which synchronizes data state between the
    // local storage adapter, and a remote API
    private final Orchestrator orchestrator;

    // Keeps track of whether of not the category is initialized yet
    private final CountDownLatch categoryInitializationsPending;

    // Configuration for the plugin.
    private AWSDataStorePluginConfiguration pluginConfiguration;

    private AWSDataStorePlugin(
            @NonNull ModelSchemaRegistry modelSchemaRegistry,
            @NonNull ModelProvider modelProvider,
            @NonNull GraphQlBehavior api) {
        this.sqliteStorageAdapter = SQLiteStorageAdapter.forModels(modelSchemaRegistry, modelProvider);
        this.storageItemChangeConverter = new GsonStorageItemChangeConverter();
        this.categoryInitializationsPending = new CountDownLatch(1);
        this.orchestrator = new Orchestrator(
            modelProvider,
            modelSchemaRegistry,
            sqliteStorageAdapter,
            AppSyncClient.via(api),
            () -> pluginConfiguration.getBaseSyncIntervalMs()
        );
    }

    /**
     * Creates an {@link AWSDataStorePlugin} which can warehouse the model types provided by
     * the supplied {@link ModelProvider}. If remote synchronization is enabled, it will be
     * performed through {@link Amplify#API}.
     * @param modelProvider Provider of models to be usable by plugin
     * @return An {@link AWSDataStorePlugin} which warehouses the provided models
     */
    @NonNull
    @SuppressWarnings("WeakerAccess")
    public static AWSDataStorePlugin forModels(@NonNull ModelProvider modelProvider) {
        Objects.requireNonNull(modelProvider);
        return create(modelProvider, Amplify.API);
    }

    /**
     * Creates an {@link AWSDataStorePlugin} which can warehouse the model types provided by the
     * supplied {@link ModelProvider}. If remote synchronization is enabled, it will be performed
     * through the provided {@link GraphQlBehavior}.
     * @param modelProvider Provides the set of models to be warehouse-able by this system
     * @param api Interface to a remote system where models will be synchronized
     * @return An {@link AWSDataStorePlugin} which warehouses the provided model types
     */
    @NonNull
    public static AWSDataStorePlugin create(@NonNull ModelProvider modelProvider, @NonNull GraphQlBehavior api) {
        Objects.requireNonNull(modelProvider);
        Objects.requireNonNull(api);
        ModelSchemaRegistry modelSchemaRegistry = ModelSchemaRegistry.instance();
        return new AWSDataStorePlugin(modelSchemaRegistry, modelProvider, api);
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
            @Nullable JSONObject pluginConfiguration,
            @NonNull Context context
    ) throws DataStoreException {
        try {
            this.pluginConfiguration =
                AWSDataStorePluginConfiguration.fromJson(pluginConfiguration);
        } catch (DataStoreException badConfigException) {
            throw new DataStoreException(
                "There was an issue configuring the plugin from the amplifyconfiguration.json",
                badConfigException,
                "Check the attached exception for more details and " +
                    "be sure you are only calling Amplify.configure once"
            );
        }

        HubChannel hubChannel = HubChannel.forCategoryType(getCategoryType());
        Amplify.Hub.subscribe(hubChannel,
            event -> InitializationStatus.SUCCEEDED.toString().equals(event.getName()),
            event -> categoryInitializationsPending.countDown()
        );
    }

    @WorkerThread
    @Override
    public void initialize(@NonNull Context context) {
        initializeStorageAdapter(context)
            .andThen(startModelSynchronization(pluginConfiguration.getSyncMode()))
            .blockingAwait();
    }

    private Completable startModelSynchronization(AWSDataStorePluginConfiguration.SyncMode syncMode) {
        if (!AWSDataStorePluginConfiguration.SyncMode.SYNC_WITH_API.equals(syncMode)) {
            return Completable.complete();
        } else {
            return orchestrator.start();
        }
    }

    /**
     * Initializes the storage adapter, and gets the result as a {@link Completable}.
     * @param context An Android Context
     * @return A Completable which will initialize the storage adapter when subscribed.
     */
    @WorkerThread
    private Completable initializeStorageAdapter(Context context) {
        return Completable.defer(() -> Completable.create(emitter ->
            sqliteStorageAdapter.initialize(context, schemaList -> emitter.onComplete(), emitter::onError)
        ));
    }

    /**
     * Terminate use of the plugin.
     * @throws AmplifyException On failure to terminate use of the plugin
     */
    synchronized void terminate() throws AmplifyException {
        orchestrator.stop();
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
        afterInitialization(() -> sqliteStorageAdapter.save(
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
        ));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void delete(
            @NonNull T item,
            @NonNull Consumer<DataStoreItemChange<T>> onItemDeleted,
            @NonNull Consumer<DataStoreException> onFailureToDelete) {
        delete(item, null, onItemDeleted, onFailureToDelete);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void delete(
            @NonNull T item,
            @Nullable QueryPredicate predicate,
            @NonNull Consumer<DataStoreItemChange<T>> onItemDeleted,
            @NonNull Consumer<DataStoreException> onFailureToDelete) {
        afterInitialization(() -> sqliteStorageAdapter.delete(
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
        ));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void query(
            @NonNull Class<T> itemClass,
            @NonNull Consumer<Iterator<T>> onQueryResults,
            @NonNull Consumer<DataStoreException> onQueryFailure) {
        afterInitialization(() -> sqliteStorageAdapter.query(itemClass, onQueryResults, onQueryFailure));
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
        afterInitialization(() ->
            sqliteStorageAdapter.query(itemClass, predicate, onQueryResults, onQueryFailure));
    }

    @Override
    public void observe(
            @NonNull Consumer<Cancelable> onObservationStarted,
            @NonNull Consumer<DataStoreItemChange<? extends Model>> onDataStoreItemChange,
            @NonNull Consumer<DataStoreException> onObservationFailure,
            @NonNull Action onObservationCompleted) {
        afterInitialization(() -> onObservationStarted.accept(sqliteStorageAdapter.observe(
            storageItemChangeRecord -> {
                try {
                    onDataStoreItemChange.accept(toDataStoreItemChange(storageItemChangeRecord));
                } catch (DataStoreException dataStoreException) {
                    onObservationFailure.accept(dataStoreException);
                }
            },
            onObservationFailure,
            onObservationCompleted
        )));
    }

    @Override
    public <T extends Model> void observe(
            @NonNull Class<T> itemClass,
            @NonNull Consumer<Cancelable> onObservationStarted,
            @NonNull Consumer<DataStoreItemChange<T>> onDataStoreItemChange,
            @NonNull Consumer<DataStoreException> onObservationFailure,
            @NonNull Action onObservationCompleted) {
        afterInitialization(() -> onObservationStarted.accept(sqliteStorageAdapter.observe(
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
        )));
    }

    @Override
    public <T extends Model> void observe(
            @NonNull Class<T> itemClass,
            @NonNull String uniqueId,
            @NonNull Consumer<Cancelable> onObservationStarted,
            @NonNull Consumer<DataStoreItemChange<T>> onDataStoreItemChange,
            @NonNull Consumer<DataStoreException> onObservationFailure,
            @NonNull Action onObservationCompleted) {
        afterInitialization(() -> onObservationStarted.accept(sqliteStorageAdapter.observe(
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
        )));
    }

    @Override
    public <T extends Model> void observe(
            @NonNull Class<T> itemClass,
            @NonNull QueryPredicate selectionCriteria,
            @NonNull Consumer<Cancelable> onObservationStarted,
            @NonNull Consumer<DataStoreItemChange<T>> onDataStoreItemChange,
            @NonNull Consumer<DataStoreException> onObservationFailure,
            @NonNull Action onObservationCompleted) {
        onObservationFailure.accept(new DataStoreException("Not implemented yet, buster!", "Check back later!"));
    }

    private void afterInitialization(@NonNull final Runnable runnable) {
        Completable.fromAction(categoryInitializationsPending::await)
            .andThen(Completable.fromRunnable(runnable))
            .blockingAwait();
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
                dataStoreItemChangeType = DataStoreItemChange.Type.DELETE;
                break;
            case UPDATE:
                dataStoreItemChangeType = DataStoreItemChange.Type.UPDATE;
                break;
            case CREATE:
                dataStoreItemChangeType = DataStoreItemChange.Type.CREATE;
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
