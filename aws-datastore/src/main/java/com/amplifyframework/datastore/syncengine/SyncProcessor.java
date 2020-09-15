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

package com.amplifyframework.datastore.syncengine;

import androidx.annotation.NonNull;

import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.datastore.AmplifyDisposables;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.DataStoreConfigurationProvider;
import com.amplifyframework.datastore.DataStoreErrorHandler;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.events.SyncQueriesStartedEvent;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.ForEach;
import com.amplifyframework.util.Time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.processors.BehaviorProcessor;

/**
 * "Hydrates" the local DataStore, using model metadata receive from the
 * {@link AppSync#sync(GraphQLRequest, Consumer, Consumer)}.
 * Hydration refers to populating the local storage with values from a remote system.
 *
 * For all items returned by the sync, merge them back into local storage through
 * the {@link Merger}.
 */
final class SyncProcessor {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final ModelProvider modelProvider;
    private final ModelSchemaRegistry modelSchemaRegistry;
    private final SyncTimeRegistry syncTimeRegistry;
    private final AppSync appSync;
    private final Merger merger;
    private final DataStoreConfigurationProvider dataStoreConfigurationProvider;
    private final String[] modelNames;

    private SyncProcessor(
            ModelProvider modelProvider,
            ModelSchemaRegistry modelSchemaRegistry,
            SyncTimeRegistry syncTimeRegistry,
            AppSync appSync,
            Merger merger,
            DataStoreConfigurationProvider dataStoreConfigurationProvider) {
        this.modelProvider = Objects.requireNonNull(modelProvider);
        this.modelSchemaRegistry = Objects.requireNonNull(modelSchemaRegistry);
        this.syncTimeRegistry = Objects.requireNonNull(syncTimeRegistry);
        this.appSync = Objects.requireNonNull(appSync);
        this.merger = Objects.requireNonNull(merger);
        this.dataStoreConfigurationProvider = dataStoreConfigurationProvider;
        this.modelNames = ForEach.inCollection(modelProvider.models(), Class::getSimpleName).toArray(new String[0]);
    }

    /**
     * Gets a builder of {@link SyncProcessor}.
     * @return A {@link SyncProcessor.Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The task of hydrating the DataStore either succeeds (with no return value),
     * or it fails, with an explanation.
     * @return An Rx {@link Completable} which can be used to perform the operation.
     */
    Completable hydrate() {
        ModelClassComparator modelClassComparator =
                new ModelClassComparator(modelProvider, modelSchemaRegistry);

        final List<Completable> hydrationTasks = new ArrayList<>();
        List<Class<? extends Model>> modelClsList =
            new ArrayList<Class<? extends Model>>(modelProvider.models());

        // And sort them all, according to their model's topological order,
        // So that when we save them, the references will exist.
        Collections.sort(modelClsList, modelClassComparator::compare);
        for (Class<? extends Model> clazz : modelClsList) {
            hydrationTasks.add(createHydrationTask(clazz));
        }

        return Completable.concat(hydrationTasks)
            .doOnSubscribe(ignore -> {
                // This is where we trigger the syncQueriesStarted event since
                // doOnSubscribe means that all upstream hydration tasks
                // have started.
                Amplify.Hub.publish(HubChannel.DATASTORE,
                    HubEvent.create(DataStoreChannelEventName.SYNC_QUERIES_STARTED,
                        new SyncQueriesStartedEvent(modelNames)
                    )
                );
            })
            .doOnComplete(() -> {
                // When the Completable completes, then emit syncQueriesReady.
                Amplify.Hub.publish(HubChannel.DATASTORE,
                    HubEvent.create(DataStoreChannelEventName.SYNC_QUERIES_READY));
            });
    }

    private Completable createHydrationTask(Class<? extends Model> modelClass) {
        ModelSyncMetricsAccumulator metricsAccumulator = new ModelSyncMetricsAccumulator(modelClass);
        return syncTimeRegistry.lookupLastSyncTime(modelClass)
            .map(this::filterOutOldSyncTimes)
            // And for each, perform a sync. The network response will contain an Iterable<ModelWithMetadata<T>>
            .flatMap(lastSyncTime -> {
                // Sync all the pages
                return syncModel(modelClass, lastSyncTime)
                    // For each ModelWithMetadata, merge it into the local store.
                    .flatMapCompletable(modelWithMetadata ->
                        merger.merge(modelWithMetadata, metricsAccumulator::increment)
                    )
                    .toSingle(() -> lastSyncTime.exists() ? SyncType.DELTA : SyncType.BASE);
            })
            .flatMapCompletable(syncType -> {
                Completable syncTimeSaveCompletable = SyncType.DELTA.equals(syncType) ?
                    syncTimeRegistry.saveLastDeltaSyncTime(modelClass, SyncTime.now()) :
                    syncTimeRegistry.saveLastBaseSyncTime(modelClass, SyncTime.now());
                return syncTimeSaveCompletable.andThen(Completable.fromAction(() -> {
                    Amplify.Hub.publish(HubChannel.DATASTORE,
                                        metricsAccumulator.toModelSyncedEvent(syncType).toHubEvent());
                }));
            })
            .doOnError(failureToSync -> {
                LOG.warn("Initial cloud sync failed.", failureToSync);
                DataStoreErrorHandler dataStoreErrorHandler =
                    dataStoreConfigurationProvider.getConfiguration().getDataStoreErrorHandler();
                dataStoreErrorHandler.accept(new DataStoreException(
                    "Initial cloud sync failed.", failureToSync,
                    "Check your internet connection."
                ));
            })
            .doOnComplete(() ->
                LOG.info("Successfully sync'd down model state from cloud.")
            );
    }

    /**
     * If a sync time is older than (now) - (the base sync interval), regard the provided sync time
     * as "too old", and return {@link SyncTime#never()}, instead. In all other cases,
     * just return the provided value.
     * @param lastSyncTime The time of a last successful sync.
     * @return The input, or {@link SyncTime#never()}, if the last sync time is "too old."
     */
    private SyncTime filterOutOldSyncTimes(SyncTime lastSyncTime) throws DataStoreException {
        if (!lastSyncTime.exists()) {
            return SyncTime.never();
        }

        // "If (now - last sync time) is within the base sync interval"
        if (Time.now() - lastSyncTime.toLong() <=
            dataStoreConfigurationProvider.getConfiguration().getSyncIntervalMs()) {
            // Pass through the last sync time, so that it can be used to compute delta sync.
            return lastSyncTime;
        }

        // In all other situations - such as when the last sync time was too long ago,
        // pretend a sync hasn't happened yet.
        return SyncTime.never();
    }

    /**
     * Sync models for a given model class.
     * This involves three steps:
     *  1. Lookup the last time the model class was synced;
     *  2. Make a request to the AppSync endpoint. If the last sync time is within a recent window
     *     of time, then request a *delta* sync. If the last sync time is outside a recent window of time,
     *     perform a *base* sync. A base sync is preformed by passing null.
     *  3. Continue fetching paged results until !hasNextResult() or we have synced the max records.
     *
     * @param modelClass The model class to sync
     * @param syncTime The time of a last successful sync.
     * @param <T> The type of model to sync.
     * @return a stream of all ModelWithMetadata&lt;T&gt; objects from all pages for the provided model.
     * @throws DataStoreException if dataStoreConfigurationProvider.getConfiguration() fails
     */
    private <T extends Model> Flowable<ModelWithMetadata<T>> syncModel(Class<T> modelClass, SyncTime syncTime)
            throws DataStoreException {
        final Long lastSyncTimeAsLong = syncTime.exists() ? syncTime.toLong() : null;
        final Integer syncPageSize = dataStoreConfigurationProvider.getConfiguration().getSyncPageSize();

        // Create a BehaviorProcessor, and set the default value to a GraphQLRequest that fetches the first page.
        BehaviorProcessor<GraphQLRequest<PaginatedResult<ModelWithMetadata<T>>>> processor =
                BehaviorProcessor.createDefault(appSync.buildSyncRequest(modelClass, lastSyncTimeAsLong, syncPageSize));

        return processor.concatMap(request -> syncPage(request).toFlowable())
                .doOnNext(paginatedResult -> {
                    if (paginatedResult.hasNextResult()) {
                        processor.onNext(paginatedResult.getRequestForNextResult());
                    } else {
                        processor.onComplete();
                    }
                })
                // Flatten the PaginatedResult objects into a stream of ModelWithMetadata objects.
                .concatMapIterable(PaginatedResult::getItems)
                // Stop after fetching the maximum configured records to sync.
                .take(dataStoreConfigurationProvider.getConfiguration().getSyncMaxRecords());
    }

    /**
     * Fetches one page for a sync.
     * @param request GraphQLRequest object for the sync, obtained from {@link AppSync#buildSyncRequest}, or from
     *                response.getData().getRequestForNextResult() for subsequent requests.
     * @param <T> The type of model to sync.
     */
    private <T extends Model> Single<PaginatedResult<ModelWithMetadata<T>>> syncPage(
            GraphQLRequest<PaginatedResult<ModelWithMetadata<T>>> request) {
        return Single.create(emitter -> {
            Cancelable cancelable = appSync.sync(request, result -> {
                if (result.hasErrors()) {
                    emitter.onError(new DataStoreException(
                            String.format("A model sync failed: %s", result.getErrors()),
                            "Check your schema."
                    ));
                } else if (!result.hasData()) {
                    emitter.onError(new DataStoreException(
                            "Empty response from AppSync.", "Report to AWS team."
                    ));
                } else {
                    emitter.onSuccess(result.getData());
                }
            }, emitter::onError);
            emitter.setDisposable(AmplifyDisposables.fromCancelable(cancelable));
        });
    }

    /**
     * Builds instances of {@link SyncProcessor}s.
     */
    public static final class Builder implements ModelProviderStep, ModelSchemaRegistryStep,
            SyncTimeRegistryStep, AppSyncStep, MergerStep, DataStoreConfigurationProviderStep, BuildStep {
        private ModelProvider modelProvider;
        private ModelSchemaRegistry modelSchemaRegistry;
        private SyncTimeRegistry syncTimeRegistry;
        private AppSync appSync;
        private Merger merger;
        private DataStoreConfigurationProvider dataStoreConfigurationProvider;

        @NonNull
        @Override
        public ModelSchemaRegistryStep modelProvider(@NonNull ModelProvider modelProvider) {
            this.modelProvider = Objects.requireNonNull(modelProvider);
            return Builder.this;
        }

        @NonNull
        @Override
        public SyncTimeRegistryStep modelSchemaRegistry(@NonNull ModelSchemaRegistry modelSchemaRegistry) {
            this.modelSchemaRegistry = Objects.requireNonNull(modelSchemaRegistry);
            return Builder.this;
        }

        @NonNull
        @Override
        public AppSyncStep syncTimeRegistry(@NonNull SyncTimeRegistry syncTimeRegistry) {
            this.syncTimeRegistry = Objects.requireNonNull(syncTimeRegistry);
            return Builder.this;
        }

        @NonNull
        @Override
        public MergerStep appSync(@NonNull AppSync appSync) {
            this.appSync = Objects.requireNonNull(appSync);
            return Builder.this;
        }

        @NonNull
        @Override
        public DataStoreConfigurationProviderStep merger(@NonNull Merger merger) {
            this.merger = Objects.requireNonNull(merger);
            return Builder.this;
        }

        @NonNull
        @Override
        public BuildStep dataStoreConfigurationProvider(
            DataStoreConfigurationProvider dataStoreConfigurationProvider) {
            this.dataStoreConfigurationProvider = dataStoreConfigurationProvider;
            return Builder.this;
        }

        @NonNull
        @Override
        public SyncProcessor build() {
            return new SyncProcessor(
                modelProvider,
                modelSchemaRegistry,
                syncTimeRegistry,
                appSync,
                merger,
                dataStoreConfigurationProvider
            );
        }
    }

    interface ModelProviderStep {
        @NonNull
        ModelSchemaRegistryStep modelProvider(@NonNull ModelProvider modelProvider);
    }

    interface ModelSchemaRegistryStep {
        @NonNull
        SyncTimeRegistryStep modelSchemaRegistry(@NonNull ModelSchemaRegistry modelSchemaRegistry);
    }

    interface SyncTimeRegistryStep {
        @NonNull
        AppSyncStep syncTimeRegistry(@NonNull SyncTimeRegistry syncTimeRegistry);
    }

    interface AppSyncStep {
        @NonNull
        MergerStep appSync(@NonNull AppSync appSync);
    }

    interface MergerStep {
        @NonNull
        DataStoreConfigurationProviderStep merger(@NonNull Merger merger);
    }

    interface DataStoreConfigurationProviderStep {
        @NonNull
        BuildStep dataStoreConfigurationProvider(DataStoreConfigurationProvider dataStoreConfiguration);
    }

    interface BuildStep {
        @NonNull
        SyncProcessor build();
    }

    /**
     * Compares to {@link ModelWithMetadata}, according to the topological order
     * of the {@link Model} within each. Topological order is determined by the
     * {@link TopologicalOrdering} utility.
     */
    private static final class ModelClassComparator {
        private final ModelSchemaRegistry modelSchemaRegistry;
        private final TopologicalOrdering topologicalOrdering;

        ModelClassComparator(ModelProvider modelProvider, ModelSchemaRegistry modelSchemaRegistry) {
            this.modelSchemaRegistry = modelSchemaRegistry;
            this.topologicalOrdering =
                    TopologicalOrdering.forRegisteredModels(modelSchemaRegistry, modelProvider);
        }

        private <M extends Class<? extends Model>> int compare(M left, M right) {
            return topologicalOrdering.compare(schemaFor(left), schemaFor(right));
        }

        /**
         * Gets the model schema for a model.
         * @param modelCls A model with metadata about it
         * @param <M> Type for ModelWithMetadata containing arbitrary model instances
         * @return Model Schema for model
         */
        @NonNull
        private <M extends Class<? extends Model>> ModelSchema schemaFor(M modelCls) {
            return modelSchemaRegistry.getModelSchemaForModelClass(modelCls.getSimpleName());
        }
    }
}
