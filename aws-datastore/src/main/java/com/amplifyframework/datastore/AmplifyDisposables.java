/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.logging.Logger;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * A utility for building Rx {@link Disposable}s from Amplify entities,
 * e.g. the {@link Cancelable}.
 */
public final class AmplifyDisposables {
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");

    private AmplifyDisposables() {}

    /**
     * Builds an Rx {@link Disposable} around an Amplify {@link Cancelable}.
     * @param cancelable An Amplify Cancelable
     * @return An Rx Disposable
     */
    @NonNull
    public static Disposable fromCancelable(@Nullable Cancelable cancelable) {
        if (cancelable == null) {
            return io.reactivex.rxjava3.disposables.Disposable.empty();
        }
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

    /**
     * This function that creates a {@link Consumer} which wraps the {@link ObservableEmitter#onError(Throwable)}
     * to prevent it from calling observers that have already been disposed.
     *
     * <p>
     * The scenario is that we have multiple event sources (1 AppSync subscription
     * for each model+operation type combination) being consumed by a single downstream
     * oberserver. Once the first subscription emits an error, the downstream subscriber
     * is placed in a disposed state and will not receive any further notifications.
     * In a situation such as loss of connectivity, it's innevitable that multiple subscriptions will fail.
     * With that said, after the first failure, the other events sources (AppSync subscriptions)
     * will attempt to invoke the downstream onError handler which then results in an
     * {@link io.reactivex.rxjava3.exceptions.UndeliverableException} being thrown.
     * </p>
     *
     * <p>
     * This method takes a reference of the observable that represents the AppSync subscription,
     * wraps it and returns a {@link Consumer} that is used as the onError parameter. The returned
     * {@link Consumer} function will delegate the onError call to the downstream observers if it's
     * still available, otherwise it logs a warning.
     * </p>
     *
     * @param realEmitter The emitter which will have it's onError function proxied by the return
     *                    value of this function.
     * @param <T> The type of model handled by the emitter.
     * @param <E> The type of exception for the onError consumer
     * @return A {@link Consumer} that proxies the {@link ObservableEmitter#onError(Throwable)} call
     * to the {@code realEmitter} if it's not disposed or logs a warning.
     * @see <a href="https://github.com/aws-amplify/amplify-android/issues/541">GitHub issue #541</a>
     *
     */
    @NonNull
    public static <T extends Model, E extends AmplifyException> Consumer<E> onErrorConsumerWrapperFor(
        @NonNull ObservableEmitter<GraphQLResponse<ModelWithMetadata<T>>> realEmitter) {
        return dataStoreException -> {
            if (realEmitter.isDisposed()) {
                LOG.warn("Attempted to invoke an emitter that is already disposed", dataStoreException);
            } else {
                realEmitter.onError(dataStoreException);
            }
        };
    }
}
