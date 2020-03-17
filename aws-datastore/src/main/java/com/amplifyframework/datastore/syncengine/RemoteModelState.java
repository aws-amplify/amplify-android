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
import androidx.annotation.Nullable;

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
import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * {@link RemoteModelState} is an implementation detail of the {@link SyncProcessor}.
 *
 * For each model described by the {@link ModelProvider}, the {@link RemoteModelState} will
 * perform a base/delta sync via the {@link AppSync} client. Every instance of each type of
 * model is then emitted onto a single {@link Observable} stream.
 *
 * The {@link RemoteModelState#observe()} method is the single intended entry point to this
 * class, and represents a stream of the current state of all models on an AppSync backend.
 */
final class RemoteModelState {
    private final AppSync appSync;
    private final ModelProvider modelProvider;
    private final ModelSchemaRegistry modelSchemaRegistry;

    RemoteModelState(
            AppSync appSync,
            ModelProvider modelProvider,
            ModelSchemaRegistry modelSchemaRegistry) {
        this.appSync = appSync;
        this.modelProvider = modelProvider;
        this.modelSchemaRegistry = modelSchemaRegistry;
    }

    /**
     * Observes the current state of all models in a remote system.
     * @return An observable onto which the state of all managed models
     *         is emitted. This Observable will complete when all
     *         models have been described.
     */
    Observable<ModelWithMetadata<? extends Model>> observe() {
        // Get an observable stream of the set of model classes.
        return Observable.fromIterable(sortedModelClasses())
            // Heavy network traffic, we require this to be done on IO scheduler.
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            // For each, get an Iterable<ModelWithMetadata<T>>, the result of a network sync
            .flatMapSingle(model -> syncModel(model, null))
            // Okay, but we want to flatten the Iterable back onto our Observable stream.
            .flatMap(Observable::fromIterable);
    }

    private <T extends Model> Single<Iterable<ModelWithMetadata<T>>> syncModel(
            final Class<T> modelClazz,
            @SuppressWarnings("SameParameterValue") @Nullable final Long lastSync) {
        return Single.create(emitter -> {
            final Cancelable cancelable =
                appSync.sync(modelClazz, lastSync, metadataEmitter(emitter), emitter::onError);
            emitter.setDisposable(asDisposable(cancelable));
        });
    }

    /**
     * A utility method to convert a cancelable to a Disposable.
     * TODO: Move this out to a more generic location?
     * @param cancelable An Amplify Cancelable
     * @return An RxJava2 Disposable that disposed by invoking the cancelation.
     */
    private Disposable asDisposable(@NonNull Cancelable cancelable) {
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

    @NonNull
    private List<Class<? extends Model>> sortedModelClasses() {
        final List<Class<? extends Model>> modelClasses = new ArrayList<>(modelProvider.models());
        final TopologicalOrdering topologicalOrdering =
            TopologicalOrdering.forRegisteredModels(modelSchemaRegistry, modelProvider);
        Collections.sort(modelClasses, (left, right) ->
            topologicalOrdering.compare(schemaFor(left), schemaFor(right)));
        return Immutable.of(modelClasses);
    }

    /**
     * Gets the model schema for a model class.
     * @param modelClass A model class
     * @param <T> Type of model
     * @return Model Schema for class
     */
    @NonNull
    private <T extends Model> ModelSchema schemaFor(Class<T> modelClass) {
        final String modelClassName = modelClass.getSimpleName();
        return modelSchemaRegistry.getModelSchemaForModelClass(modelClassName);
    }
}
