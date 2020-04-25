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

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.util.Time;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * "Hydrates" the local DataStore, using model metadata receive from the
 * {@link AppSync#sync(Class, Long, Consumer, Consumer)}.
 * Hydration refers to populating the local storage with values from a remote system.
 *
 * For all items returned by the sync, merge them back into local storage through
 * the {@link Merger}.
 */
final class SyncProcessor {
    private final ModelProvider modelProvider;
    private final ModelSchemaRegistry modelSchemaRegistry;
    private final SyncTimeRegistry syncTimeRegistry;
    private final AppSync appSync;
    private final Merger merger;
    private final long syncIntervalMs;
    private final long syncIntervalInMinutes;

    private SyncProcessor(
            ModelProvider modelProvider,
            ModelSchemaRegistry modelSchemaRegistry,
            SyncTimeRegistry syncTimeRegistry,
            AppSync appSync,
            Merger merger,
            long syncIntervalInMinutes) {
        this.modelProvider = Objects.requireNonNull(modelProvider);
        this.modelSchemaRegistry = Objects.requireNonNull(modelSchemaRegistry);
        this.syncTimeRegistry = Objects.requireNonNull(syncTimeRegistry);
        this.appSync = Objects.requireNonNull(appSync);
        this.merger = Objects.requireNonNull(merger);
        this.syncIntervalInMinutes = syncIntervalInMinutes;
        this.syncIntervalMs = TimeUnit.MINUTES.toMillis(syncIntervalInMinutes);
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
        ModelWithMetadataComparator modelWithMetadataComparator =
            new ModelWithMetadataComparator(modelProvider, modelSchemaRegistry);

        // Observe the remote model states,
        // Get an observable stream of the set of model classes.
        return Observable.fromIterable(modelProvider.models())
            // Heavy network traffic, we require this to be done on IO scheduler.
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            // For each model class, find the last time it was sync'd.
            .flatMapCompletable(modelClass ->
                syncTimeRegistry.lookupLastSyncTime(modelClass)
                    .map(this::filterOutOldSyncTimes)
                    // And for each, perform a sync. The network response will contain an Iterable<ModelWithMetadata<T>>
                    .flatMapCompletable(lastSyncTime -> syncModel(modelClass, lastSyncTime)
                        // Okay, but we want to flatten the Iterable elements back into an Observable stream.
                        .flatMapObservable(Observable::fromIterable)
                        // And sort them all, according to their model's topological order,
                        // So that when we save them, the references will exist.
                        .sorted(modelWithMetadataComparator::compare)
                        // For each ModelWithMetadata, merge it into the local store.
                        .flatMapCompletable(merger::merge)
                    )
                    .andThen(syncTimeRegistry.saveLastSyncTime(modelClass, SyncTime.now()))
            );
    }

    /**
     * If a sync time is older than (now) - (the base sync interval), regard the provided sync time
     * as "too old", and return {@link SyncTime#never()}, instead. In all other cases,
     * just return the provided value.
     * @param lastSyncTime The time of a last successful sync.
     * @return The input, or {@link SyncTime#never()}, if the last sync time is "too old."
     */
    private SyncTime filterOutOldSyncTimes(SyncTime lastSyncTime) {
        if (!lastSyncTime.exists()) {
            return SyncTime.never();
        }

        // "If (now - last sync time) is within the base sync interval"
        if (Time.now() - lastSyncTime.toLong() <= syncIntervalMs) {
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
     *  3. Update the
     * @param modelClass The model class to sync
     * @param <T> The type of model to sync
     * @return An {@link Single} which emits sync content, on success, {@link DataStoreException} on failure
     */
    private <T extends Model> Single<Iterable<ModelWithMetadata<T>>> syncModel(
            Class<T> modelClass, SyncTime syncTime) {
        final Long lastSyncTimeAsLong = syncTime.exists() ? syncTime.toLong() : null;
        return Single.create(emitter -> {
            final Cancelable cancelable =
                appSync.sync(modelClass, lastSyncTimeAsLong, metadataEmitter(emitter), emitter::onError);
            emitter.setDisposable(asDisposable(cancelable));
        });
    }

    /**
     * A utility method to convert a cancelable to a Disposable.
     * TODO: Move this out to a more generic location?
     * @param cancelable An Amplify Cancelable
     * @return An RxJava2 Disposable that disposed by invoking the cancellation.
     */
    private static Disposable asDisposable(@NonNull Cancelable cancelable) {
        Objects.requireNonNull(cancelable);
        return new Disposable() {
            private final AtomicReference<Boolean> isCanceled = new AtomicReference<>(false);
            @Override
            public void dispose() {
                synchronized (isCanceled) {
                    if (!isCanceled.get()) {
                        cancelable.cancel();
                        isCanceled.set(true);
                    }
                }
            }

            @Override
            public boolean isDisposed() {
                synchronized (isCanceled) {
                    return isCanceled.get();
                }
            }
        };
    }

    private static <T extends Model> Consumer<GraphQLResponse<Iterable<ModelWithMetadata<T>>>> metadataEmitter(
        SingleEmitter<Iterable<ModelWithMetadata<T>>> singleEmitter) {
        return resultFromEndpoint -> {
            if (resultFromEndpoint.hasErrors()) {
                singleEmitter.onError(new DataStoreException(
                    String.format("A model sync failed: %s", resultFromEndpoint.getErrors()),
                    "Check your schema."
                ));
            } else if (!resultFromEndpoint.hasData()) {
                singleEmitter.onError(new DataStoreException(
                    "Empty response from AppSync.", "Report to AWS team."
                ));
            } else {
                final Set<ModelWithMetadata<T>> emittedValue = new HashSet<>();
                for (ModelWithMetadata<T> modelWithMetadata : resultFromEndpoint.getData()) {
                    emittedValue.add(modelWithMetadata);
                }
                singleEmitter.onSuccess(emittedValue);
            }
        };
    }

    /**
     * Builds instances of {@link SyncProcessor}s.
     */
    public static final class Builder implements ModelProviderStep, ModelSchemaRegistryStep,
            SyncTimeRegistryStep, AppSyncStep, MergerStep, SyncIntervalStep, BuildStep {
        private ModelProvider modelProvider;
        private ModelSchemaRegistry modelSchemaRegistry;
        private SyncTimeRegistry syncTimeRegistry;
        private AppSync appSync;
        private Merger merger;
        private long syncIntervalInMinutes;

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
        public SyncIntervalStep merger(@NonNull Merger merger) {
            this.merger = Objects.requireNonNull(merger);
            return Builder.this;
        }

        @NonNull
        @Override
        public BuildStep syncIntervalInMinutes(long syncIntervalInMinutes) {
            this.syncIntervalInMinutes = syncIntervalInMinutes;
            return Builder.this;
        }

        @SuppressLint("SyntheticAccessor")
        @NonNull
        @Override
        public SyncProcessor build() {
            return new SyncProcessor(
                modelProvider,
                modelSchemaRegistry,
                syncTimeRegistry,
                appSync,
                merger,
                syncIntervalInMinutes
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
        SyncIntervalStep merger(@NonNull Merger merger);
    }

    interface SyncIntervalStep {
        @NonNull
        BuildStep syncIntervalInMinutes(long syncIntervalInMinutes);
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
    private static final class ModelWithMetadataComparator {
        private final ModelSchemaRegistry modelSchemaRegistry;
        private final TopologicalOrdering topologicalOrdering;

        ModelWithMetadataComparator(ModelProvider modelProvider, ModelSchemaRegistry modelSchemaRegistry) {
            this.modelSchemaRegistry = modelSchemaRegistry;
            this.topologicalOrdering =
                TopologicalOrdering.forRegisteredModels(modelSchemaRegistry, modelProvider);
        }

        private <M extends ModelWithMetadata<? extends Model>> int compare(M left, M right) {
            return topologicalOrdering.compare(schemaFor(left), schemaFor(right));
        }

        /**
         * Gets the model schema for a model.
         * @param modelWithMetadata A model with metadata about it
         * @param <M> Type for ModelWithMetadata containing arbitrary model instances
         * @return Model Schema for model
         */
        @NonNull
        private <M extends ModelWithMetadata<? extends Model>> ModelSchema schemaFor(M modelWithMetadata) {
            return modelSchemaRegistry.getModelSchemaForModelInstance(modelWithMetadata.getModel());
        }
    }
}
