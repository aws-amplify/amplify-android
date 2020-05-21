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
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.async.NoOpCancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.ReplaySubject;

/**
 * Observes mutations occurring on a remote {@link AppSync} system. The mutations arrive
 * over a long-lived subscription, as {@link SubscriptionEvent}s.
 * For every type of model provided by a {@link ModelProvider}, the SubscriptionProcessor
 * marries mutated models back into the local DataStore, through the {@link Merger}.
 */
final class SubscriptionProcessor {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private static final long SUBSCRIPTION_START_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);

    private final AppSync appSync;
    private final ModelProvider modelProvider;
    private final Merger merger;
    private final CompositeDisposable ongoingOperationsDisposable;
    private final ReplaySubject<SubscriptionEvent<? extends Model>> buffer;

    /**
     * Constructs a new SubscriptionProcessor.
     * @param appSync An App Sync endpoint from which to receive subscription events
     * @param modelProvider The processor will subscribe to changes for these types of models
     * @param merger A merger, to apply data back into local storage
     */
    SubscriptionProcessor(
            @NonNull AppSync appSync,
            @NonNull ModelProvider modelProvider,
            @NonNull Merger merger) {
        this.appSync = Objects.requireNonNull(appSync);
        this.modelProvider = Objects.requireNonNull(modelProvider);
        this.merger = Objects.requireNonNull(merger);
        this.ongoingOperationsDisposable = new CompositeDisposable();
        this.buffer = ReplaySubject.create();
    }

    /**
     * Start subscribing to model mutations.
     */
    Disposable startSubscriptions() {
        Set<Observable<SubscriptionEvent<? extends Model>>> subscriptions = new HashSet<>();
        for (Class<? extends Model> clazz : modelProvider.models()) {
            for (SubscriptionType subscriptionType : SubscriptionType.values()) {
                subscriptions.add(subscriptionObservable(appSync, subscriptionType, clazz));
            }
        }
        ongoingOperationsDisposable.add(Observable.merge(subscriptions)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .doOnSubscribe(disposable ->
                LOG.info(String.format(Locale.US,
                    "Began buffering subscription events for remote mutations %s to Cloud models of types %s.",
                    modelProvider.models(), Arrays.toString(SubscriptionType.values())
                ))
            )
            .subscribe(
                buffer::onNext,
                failure -> Amplify.Hub.publish(
                    HubChannel.DATASTORE,
                    HubEvent.create(DataStoreChannelEventName.LOST_CONNECTION, failure)
                ),
                () -> LOG.warn("Subscriptions stream completed.")
            ));

        return ongoingOperationsDisposable;
    }

    @SuppressWarnings("unchecked") // (Class<T>) modelWithMetadata.getModel().getClass()
    private static <T extends Model> Observable<SubscriptionEvent<? extends Model>>
            subscriptionObservable(AppSync appSync, SubscriptionType subscriptionType, Class<T> clazz) {
        return Observable.<GraphQLResponse<ModelWithMetadata<T>>>create(emitter -> {
            CountDownLatch latch = new CountDownLatch(1);
            SubscriptionMethod method = subscriptionMethodFor(appSync, subscriptionType);
            AtomicReference<Cancelable> cancelable = new AtomicReference<>(NoOpCancelable::new);
            emitter.setCancellable(cancelable::get);
            cancelable.set(method.subscribe(
                clazz,
                token -> latch.countDown(),
                emitter::onNext,
                emitter::onError,
                emitter::onComplete
            ));
            latch.await(SUBSCRIPTION_START_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        })
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .map(SubscriptionProcessor::unwrapResponse)
        .map(modelWithMetadata -> SubscriptionEvent.<T>builder()
            .type(fromSubscriptionType(subscriptionType))
            .modelWithMetadata(modelWithMetadata)
            .modelClass((Class<T>) modelWithMetadata.getModel().getClass())
            .build()
        );
    }

    /**
     * Start draining mutations out of the mutation buffer.
     * This should be called after {@link #startSubscriptions()}.
     */
    Disposable startDrainingMutationBuffer() {
        ongoingOperationsDisposable.add(
            buffer
                .doOnSubscribe(disposable ->
                    LOG.info("Starting processing subscription data buffer.")
                )
                .flatMapCompletable(mutation -> merger.merge(mutation.modelWithMetadata()))
                .subscribe(
                    () -> LOG.warn("Reading from subscriptions buffer is completed."),
                    failure -> LOG.warn("Reading subscriptions buffer has failed.", failure)
                )
        );
        return ongoingOperationsDisposable;
    }

    /**
     * Stop any/all ongoing activities.
     */
    void stopAllActivity() {
        ongoingOperationsDisposable.clear();
    }

    @VisibleForTesting
    static SubscriptionMethod subscriptionMethodFor(
            AppSync appSync, SubscriptionType subscriptionType) throws DataStoreException {
        switch (subscriptionType) {
            case ON_UPDATE:
                return appSync::onUpdate;
            case ON_DELETE:
                return appSync::onDelete;
            case ON_CREATE:
                return appSync::onCreate;
            default:
                throw new DataStoreException(
                    "Failed to establish a model subscription.",
                    "Was a new subscription type created?"
                );
        }
    }

    private static <T extends Model> ModelWithMetadata<T> unwrapResponse(
            GraphQLResponse<? extends ModelWithMetadata<T>> response) throws DataStoreException {
        final String errorMessage;
        if (response.hasErrors()) {
            errorMessage = String.format("Errors on subscription: %s", response.getErrors());
        } else if (!response.hasData()) {
            errorMessage = "Empty data received on subscription.";
        } else {
            errorMessage = null;
        }
        if (errorMessage != null) {
            throw new DataStoreException(
                errorMessage, AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
            );
        }
        return response.getData();
    }

    private static SubscriptionEvent.Type fromSubscriptionType(SubscriptionType subscriptionType) {
        switch (subscriptionType) {
            case ON_CREATE:
                return SubscriptionEvent.Type.CREATE;
            case ON_DELETE:
                return SubscriptionEvent.Type.DELETE;
            case ON_UPDATE:
                return SubscriptionEvent.Type.UPDATE;
            default:
                throw new IllegalArgumentException("Unknown subscription type: " + subscriptionType);
        }
    }

    interface SubscriptionMethod {
        <T extends Model> Cancelable subscribe(
                @NonNull Class<T> clazz,
                @NonNull Consumer<String> onStart,
                @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
                @NonNull Consumer<DataStoreException> onFailure,
                @NonNull Action onComplete
        );
    }
}
