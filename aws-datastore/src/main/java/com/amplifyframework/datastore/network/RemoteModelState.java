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

package com.amplifyframework.datastore.network;

import androidx.annotation.Nullable;

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.datastore.DataStoreException;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@SuppressWarnings("unused") // one sec
final class RemoteModelState {
    private final AppSyncEndpoint endpoint;
    private final ModelProvider modelProvider;

    RemoteModelState(
            AppSyncEndpoint endpoint,
            ModelProvider modelProvider) {
        this.endpoint = endpoint;
        this.modelProvider = modelProvider;
    }

    /**
     * Observes the current state of all models in a remote system.
     * @return An observable onto which the state of all managed models
     *         is emitted. This Observable will complete when all
     *         models have been described.
     */
    Observable<ModelWithMetadata<? extends Model>> observe() {
        // Get an observable stream of the set of model classes.
        return Observable.fromIterable(modelProvider.models())
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
                endpoint.sync(modelClazz, lastSync, metadataEmitter(emitter), emitter::onError);
            emitter.setDisposable(asDisposable(cancelable));
        });
    }

    /**
     * A utility method to convert a cancelable to a Disposable.
     * TODO: Move this out to a more generic location?
     * @param cancelable An Amplify Cancelable
     * @return An RxJava2 Disposable that disposed by invoking the cancelation.
     */
    private Disposable asDisposable(Cancelable cancelable) {
        return new Disposable() {
            private final AtomicReference<Boolean> isCanceled = new AtomicReference<>(false);
            @Override
            public void dispose() {
                synchronized (isCanceled) {
                    cancelable.cancel();
                    isCanceled.set(true);
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
}
