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

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
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
import com.amplifyframework.util.Time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;

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
    private final SchemaRegistry schemaRegistry;
    private final SyncTimeRegistry syncTimeRegistry;
    private final AppSync appSync;
    private final Merger merger;
    private final DataStoreConfigurationProvider dataStoreConfigurationProvider;
    private final QueryPredicateProvider queryPredicateProvider;
    private final RetryHandler requestRetry;

    /**
     * The `isSyncRetryEnabled` value is being passed down all the way from the `AWSDataStorePlugin` or the
     * `DataStoreConfiguration` where it's named `doSyncRetry`.
     */
    private final boolean isSyncRetryEnabled;

    private SyncProcessor(Builder builder) {
        this.modelProvider = builder.modelProvider;
        this.schemaRegistry = builder.schemaRegistry;
        this.syncTimeRegistry = builder.syncTimeRegistry;
        this.appSync = builder.appSync;
        this.merger = builder.merger;
        this.dataStoreConfigurationProvider = builder.dataStoreConfigurationProvider;
        this.queryPredicateProvider = builder.queryPredicateProvider;
        this.requestRetry = builder.requestRetry;
        this.isSyncRetryEnabled = builder.isSyncRetryEnabled;

        if (!this.isSyncRetryEnabled) {
            LOG.warn("Disabling sync retries will be deprecated in a future version.");
        }
    }

    /**
     * Returns a step builder to begin construction of a new {@link SyncProcessor} instance.
     * @return The first step in a sequence of steps to build an instance of the sync processor
     */
    public static ModelProviderStep builder() {
        return new Builder();
    }

    /**
     * The task of hydrating the DataStore either succeeds (with no return value),
     * or it fails, with an explanation.
     * @return An Rx {@link Completable} which can be used to perform the operation.
     */
    Completable hydrate() {
        final List<Completable> hydrationTasks = new ArrayList<>();
        List<ModelSchema> modelSchemas = new ArrayList<>(modelProvider.modelSchemas().values());

        // And sort them all, according to their model's topological order,
        // So that when we save them, the references will exist.
        TopologicalOrdering ordering =
            TopologicalOrdering.forRegisteredModels(schemaRegistry, modelProvider);
        Collections.sort(modelSchemas, ordering::compare);
        ArrayList<String> toBeSyncedModelArray = new ArrayList<>();
        for (ModelSchema schema : modelSchemas) {
            //Check to see if query predicate for this schema is not equal to none. This means customer does
            // not want to sync the data for this model.
            if (!QueryPredicates.none().equals(queryPredicateProvider.getPredicate(schema.getName()))) {
                hydrationTasks.add(createHydrationTask(schema));
                toBeSyncedModelArray.add(schema.getName());
            }
        }

        return Completable.concat(hydrationTasks)
            .doOnSubscribe(ignore -> {
                // This is where we trigger the syncQueriesStarted event since
                // doOnSubscribe means that all upstream hydration tasks
                // have started.
                Amplify.Hub.publish(HubChannel.DATASTORE,
                    HubEvent.create(DataStoreChannelEventName.SYNC_QUERIES_STARTED,
                        new SyncQueriesStartedEvent(toBeSyncedModelArray.toArray(new String[0]))
                    )
                );
            })
            .doOnComplete(() -> {
                // When the Completable completes, then emit syncQueriesReady.
                Amplify.Hub.publish(HubChannel.DATASTORE,
                    HubEvent.create(DataStoreChannelEventName.SYNC_QUERIES_READY));
            });
    }

    private Completable createHydrationTask(ModelSchema schema) {
        ModelSyncMetricsAccumulator metricsAccumulator = new ModelSyncMetricsAccumulator(schema.getName());
        return syncTimeRegistry.lookupLastSyncTime(schema.getName())
            .map(this::filterOutOldSyncTimes)
            // And for each, perform a sync. The network response will contain an Iterable<ModelWithMetadata<T>>
            .flatMap(lastSyncTime -> {
                // Sync all the pages
                return syncModel(schema, lastSyncTime)
                    // Switch to a new thread so that subsequent API fetches will happen in parallel with DB writes.
                    .observeOn(Schedulers.io())
                    // Flatten to a stream of ModelWithMetadata objects
                    .concatMap(Flowable::fromIterable)
                    .concatMapCompletable(item -> merger.merge(item, metricsAccumulator::increment))
                    .toSingle(() -> lastSyncTime.exists() ? SyncType.DELTA : SyncType.BASE);
            })
            .flatMapCompletable(syncType -> {
                Completable syncTimeSaveCompletable = SyncType.DELTA.equals(syncType) ?
                    syncTimeRegistry.saveLastDeltaSyncTime(schema.getName(), SyncTime.now()) :
                    syncTimeRegistry.saveLastBaseSyncTime(schema.getName(), SyncTime.now());
                return syncTimeSaveCompletable.andThen(Completable.fromAction(() ->
                    Amplify.Hub.publish(
                        HubChannel.DATASTORE, metricsAccumulator.toModelSyncedEvent(syncType).toHubEvent()
                    )
                ));
            })
            .doOnError(failureToSync -> {
                LOG.warn("Initial cloud sync failed for " + schema.getName() + ".", failureToSync);
                DataStoreErrorHandler dataStoreErrorHandler =
                    dataStoreConfigurationProvider.getConfiguration().getErrorHandler();
                dataStoreErrorHandler.accept(new DataStoreException(
                    "Initial cloud sync failed for " + schema.getName() + ".", failureToSync,
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
     * @param schema The schema of the model to sync
     * @param syncTime The time of a last successful sync.
     * @param <T> The type of model to sync.
     * @return a stream of all ModelWithMetadata&lt;T&gt; objects from all pages for the provided model.
     * @throws DataStoreException if dataStoreConfigurationProvider.getConfiguration() fails
     */
    private <T extends Model> Flowable<List<ModelWithMetadata<T>>> syncModel(ModelSchema schema, SyncTime syncTime)
            throws DataStoreException {
        final Long lastSyncTimeAsLong = syncTime.exists() ? syncTime.toLong() : null;
        final Integer syncPageSize = dataStoreConfigurationProvider.getConfiguration().getSyncPageSize();
        final Integer syncMaxRecords = dataStoreConfigurationProvider.getConfiguration().getSyncMaxRecords();
        AtomicReference<Integer> recordsFetched = new AtomicReference<>(0);
        QueryPredicate predicate = queryPredicateProvider.getPredicate(schema.getName());
        // Create a BehaviorProcessor, and set the default value to a GraphQLRequest that fetches the first page.
        BehaviorProcessor<GraphQLRequest<PaginatedResult<ModelWithMetadata<T>>>> processor =
                BehaviorProcessor.createDefault(
                        appSync.buildSyncRequest(schema, lastSyncTimeAsLong, syncPageSize, predicate));

        return processor.concatMap(request -> {
            if (isSyncRetryEnabled) {
                return syncPageWithRetry(request).toFlowable();
            } else {
                return syncPage(request).toFlowable();
            }
        })
                .doOnNext(paginatedResult -> {
                    if (paginatedResult.hasNextResult()) {
                        processor.onNext(paginatedResult.getRequestForNextResult());
                    } else {
                        processor.onComplete();
                    }
                })
                // If it's a SerializedModel, add the ModelSchema, since it isn't added during deserialization.
                .map(paginatedResult -> Flowable.fromIterable(paginatedResult)
                        .map(modelWithMetadata -> hydrateSchemaIfNeeded(modelWithMetadata, schema))
                        .toList()
                        .blockingGet()
                )
                .takeUntil(items -> recordsFetched.accumulateAndGet(items.size(), Integer::sum) >= syncMaxRecords);
    }

    @SuppressWarnings("unchecked") // Cast to T
    private <T extends Model> ModelWithMetadata<T> hydrateSchemaIfNeeded(ModelWithMetadata<T> original,
                                                                         ModelSchema schema) {
        if (original.getModel() instanceof SerializedModel) {
            SerializedModel originalModel = (SerializedModel) original.getModel();
            SerializedModel newModel = SerializedModel.builder()
                    .modelSchema(schema)
                    .serializedData(SerializedModel.parseSerializedData(
                            originalModel.getSerializedData(),
                            schema.getName(),
                            schemaRegistry
                    ))
                    .build();
            return new ModelWithMetadata<>((T) newModel, original.getSyncMetadata());
        } else {
            return original;
        }
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
                    emitter.onError(new DataStoreException.IrRecoverableException(
                            String.format("A model sync failed: %s", result.getErrors()),
                            "Check your schema."
                    ));
                } else if (!result.hasData()) {
                    emitter.onError(new DataStoreException.IrRecoverableException(
                            "Empty response from AppSync.", "Report to AWS team."
                    ));
                } else {
                    emitter.onSuccess(result.getData());
                }
            }, emitter::onError);
            emitter.setDisposable(AmplifyDisposables.fromCancelable(cancelable));
        });
    }

    private <T extends Model> Single<PaginatedResult<ModelWithMetadata<T>>> syncPageWithRetry(
            GraphQLRequest<PaginatedResult<ModelWithMetadata<T>>> request) {
        // We don't want to treat DataStoreException.GraphQLResponseException as non retryable here because we want to
        // support merging the applicable data if any.
        List<Class<? extends Throwable>> skipException = new ArrayList<>();
        skipException.add(DataStoreException.IrRecoverableException.class);
        skipException.add(ApiException.NonRetryableException.class);
        return requestRetry.retry(syncPage(request), skipException);
    }

    /**
     * Builds instances of {@link SyncProcessor}s.
     */
    public static final class Builder implements ModelProviderStep, SchemaRegistryStep,
            SyncTimeRegistryStep, AppSyncStep, MergerStep, DataStoreConfigurationProviderStep,
            QueryPredicateProviderStep, RetryHandlerStep, SyncRetryStep, BuildStep {
        private ModelProvider modelProvider;
        private SchemaRegistry schemaRegistry;
        private SyncTimeRegistry syncTimeRegistry;
        private AppSync appSync;
        private Merger merger;
        private DataStoreConfigurationProvider dataStoreConfigurationProvider;
        private QueryPredicateProvider queryPredicateProvider;
        private RetryHandler requestRetry;
        private boolean isSyncRetryEnabled;

        @NonNull
        @Override
        public SchemaRegistryStep modelProvider(@NonNull ModelProvider modelProvider) {
            this.modelProvider = Objects.requireNonNull(modelProvider);
            return Builder.this;
        }

        @NonNull
        @Override
        public SyncTimeRegistryStep schemaRegistry(@NonNull SchemaRegistry schemaRegistry) {
            this.schemaRegistry = Objects.requireNonNull(schemaRegistry);
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
        public QueryPredicateProviderStep dataStoreConfigurationProvider(
            DataStoreConfigurationProvider dataStoreConfigurationProvider) {
            this.dataStoreConfigurationProvider = dataStoreConfigurationProvider;
            return Builder.this;
        }

        @NonNull
        @Override
        public RetryHandlerStep queryPredicateProvider(QueryPredicateProvider queryPredicateProvider) {
            this.queryPredicateProvider = Objects.requireNonNull(queryPredicateProvider);
            return Builder.this;
        }

        @NonNull
        @Override
        public BuildStep isSyncRetryEnabled(boolean isSyncRetryEnabled) {
            this.isSyncRetryEnabled = isSyncRetryEnabled;
            return Builder.this;
        }

        @NonNull
        @Override
        public SyncProcessor build() {
            return new SyncProcessor(this);
        }

        @NonNull
        @Override
        public SyncRetryStep retryHandler(RetryHandler requestRetry) {
            this.requestRetry = requestRetry;
            return Builder.this;
        }
    }

    interface ModelProviderStep {
        @NonNull
        SchemaRegistryStep modelProvider(@NonNull ModelProvider modelProvider);
    }

    interface SchemaRegistryStep {
        @NonNull
        SyncTimeRegistryStep schemaRegistry(@NonNull SchemaRegistry schemaRegistry);
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
        QueryPredicateProviderStep dataStoreConfigurationProvider(
                DataStoreConfigurationProvider dataStoreConfiguration);
    }

    interface QueryPredicateProviderStep {
        @NonNull
        RetryHandlerStep queryPredicateProvider(QueryPredicateProvider queryPredicateProvider);
    }

    interface RetryHandlerStep {
        @NonNull
        SyncRetryStep retryHandler(RetryHandler requestRetry);
    }

    interface SyncRetryStep {
        @NonNull
        BuildStep isSyncRetryEnabled(boolean isSyncRetryEnabled);
    }

    interface BuildStep {
        @NonNull
        SyncProcessor build();
    }
}
