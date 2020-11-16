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
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.AmplifyDisposables;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.DataStoreConfigurationProvider;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.AppSyncExtensions;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.ReplaySubject;

/**
 * Observes mutations occurring on a remote {@link AppSync} system. The mutations arrive
 * over a long-lived subscription, as {@link SubscriptionEvent}s.
 * For every type of model provided by a {@link ModelProvider}, the SubscriptionProcessor
 * marries mutated models back into the local DataStore, through the {@link Merger}.
 */
final class SubscriptionProcessor {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private static final long TIMEOUT_SECONDS_PER_MODEL = 2;
    private static final long NETWORK_OP_TIMEOUT_SECONDS = 10;

    private final AppSync appSync;
    private final ModelProvider modelProvider;
    private final Merger merger;
    private final DataStoreConfigurationProvider dataStoreConfigurationProvider;
    private final CompositeDisposable ongoingOperationsDisposable;
    private final long adjustedTimeoutSeconds;
    private ReplaySubject<SubscriptionEvent<? extends Model>> buffer;

    /**
     * Constructs a new SubscriptionProcessor.
     * @param builder A SubscriptionProcessor Builder.
     */
    private SubscriptionProcessor(Builder builder) {
        this.appSync = builder.appSync;
        this.modelProvider = builder.modelProvider;
        this.merger = builder.merger;
        this.dataStoreConfigurationProvider = builder.dataStoreConfigurationProvider;
        this.ongoingOperationsDisposable = new CompositeDisposable();

        // Operation times out after 10 seconds. If there are more than 5 models,
        // then 2 seconds are added to the timer per additional model count.
        this.adjustedTimeoutSeconds = Math.max(
            NETWORK_OP_TIMEOUT_SECONDS,
            TIMEOUT_SECONDS_PER_MODEL * modelProvider.models().size()
        );
    }

    /**
     * Returns a step builder to begin construction of a new {@link SubscriptionProcessor} instance.
     * @return  The first step in a sequence of steps to build an instance of the subscription processor.
     */
    public static AppSyncStep builder() {
        return new Builder();
    }

    /**
     * Start subscribing to model mutations.
     */
    synchronized void startSubscriptions() {
        int subscriptionCount = modelProvider.modelNames().size() * SubscriptionType.values().length;
        // Create a latch with the number of subscriptions are requesting. Each of these will be
        // counted down when each subscription's onStarted event is called.
        CountDownLatch latch = new CountDownLatch(subscriptionCount);
        // Need to create a new buffer so we can properly handle retries and stop/start scenarios.
        // Re-using the same buffer has some unexpected results due to the replay aspect of the subject.
        buffer = ReplaySubject.create();

        Set<Observable<SubscriptionEvent<? extends Model>>> subscriptions = new HashSet<>();
        for (ModelSchema modelSchema : modelProvider.modelSchemas().values()) {
            for (SubscriptionType subscriptionType : SubscriptionType.values()) {
                subscriptions.add(subscriptionObservable(appSync, subscriptionType, latch, modelSchema));
            }
        }

        ongoingOperationsDisposable.add(Observable.merge(subscriptions)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(
                buffer::onNext,
                exception -> {
                    // If the downstream buffer already has an error, don't invoke it again.
                    if (!buffer.hasThrowable()) {
                        buffer.onError(exception);
                    }
                },
                buffer::onComplete
            ));
        boolean subscriptionsStarted;
        try {
            LOG.debug("Waiting for subscriptions to start.");
            subscriptionsStarted = latch.await(adjustedTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            LOG.warn("Subscription operations were interrupted during setup.");
            return;
        }
        if (subscriptionsStarted) {
            Amplify.Hub.publish(HubChannel.DATASTORE,
                                HubEvent.create(DataStoreChannelEventName.SUBSCRIPTIONS_ESTABLISHED));
            LOG.info(String.format(Locale.US,
                "Began buffering subscription events for remote mutations %s to Cloud models of types %s.",
                modelProvider.modelNames(), Arrays.toString(SubscriptionType.values())
            ));
        } else {
            LOG.warn("Subscription processor failed to start within the expected timeout.");
        }
    }

    private boolean isUnauthorizedException(DataStoreException exception) {
        if (exception instanceof DataStoreException.GraphQLResponseException) {
            List<GraphQLResponse.Error> errors = ((DataStoreException.GraphQLResponseException) exception).getErrors();
            GraphQLResponse.Error firstError = errors.get(0);
            AppSyncExtensions extensions = new AppSyncExtensions(firstError.getExtensions());
            return AppSyncExtensions.AppSyncErrorType.UNAUTHORIZED.equals(extensions.getErrorType());
        }
        return false;
    }

    @SuppressWarnings("unchecked") // (Class<T>) modelWithMetadata.getModel().getClass()
    private <T extends Model> Observable<SubscriptionEvent<? extends Model>>
            subscriptionObservable(AppSync appSync,
                                   SubscriptionType subscriptionType,
                                   CountDownLatch latch,
                                   ModelSchema modelSchema) {
        return Observable.<GraphQLResponse<ModelWithMetadata<T>>>create(emitter -> {
            SubscriptionMethod method = subscriptionMethodFor(appSync, subscriptionType);
            AtomicReference<String> subscriptionId = new AtomicReference<>();
            Cancelable cancelable = method.subscribe(
                modelSchema,
                token -> {
                    LOG.debug("Subscription started for " + subscriptionType.name() + " " + modelSchema.getName() +
                            " subscriptionId: " + token);
                    subscriptionId.set(token);
                    latch.countDown();
                },
                emitter::onNext,
                dataStoreException -> {
                    // Only call onError if the Observable hasn't been disposed and it's not an Unauthorized error.
                    // Unauthorized errors are ignored, so that DataStore can still be used even if the user is only
                    // authorized to read a subset of the models.
                    if (!emitter.isDisposed()) {
                        if (isUnauthorizedException(dataStoreException)) {
                            LOG.warn("Unauthorized failure for " + subscriptionType + " " + modelSchema.getName());
                        } else {
                            emitter.onError(dataStoreException);
                        }
                    }
                    if (latch.getCount() != 0) {
                        LOG.warn("Releasing latch due to an error: " + dataStoreException.getMessage());
                        latch.countDown();
                    }
                },
                () -> {
                    LOG.debug("Subscription completed:" + subscriptionId.get());
                    emitter.onComplete();
                }
            );
            // When the observable is disposed, we need to call cancel() on the subscription
            // so it can properly dispose of resources if necessary. For the AWS API plugin,
            // this means closing the underlying network connection.
            emitter.setDisposable(AmplifyDisposables.fromCancelable(cancelable));
        })
        .doOnError(subscriptionError -> LOG.warn("An error occurred on the remote " + subscriptionType.name() +
                " subscription for model " + modelSchema.getName(), subscriptionError)
        )
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .map(SubscriptionProcessor::unwrapResponse)
        .filter(modelWithMetadata -> {
            QueryPredicate predicate =
                    dataStoreConfigurationProvider.getConfiguration().getSyncQueryPredicate(modelSchema.getName());
            return predicate.evaluate(modelWithMetadata.getModel());
        })
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
    void startDrainingMutationBuffer(Action onPipelineBroken) {
        ongoingOperationsDisposable.add(
            buffer
                .doOnSubscribe(disposable ->
                    LOG.info("Starting processing subscription data buffer.")
                )
                .flatMapCompletable(mutation -> merger.merge(mutation.modelWithMetadata()))
                .doOnError(failure -> LOG.warn("Reading subscriptions buffer has failed.", failure))
                .doOnComplete(() -> LOG.warn("Reading from subscriptions buffer is completed."))
                .onErrorComplete()
                .subscribe(onPipelineBroken::call)
        );
    }

    /**
     * Stop any active subscriptions, and stop draining the mutation buffer.
     */
    synchronized void stopAllSubscriptionActivity() {
        LOG.info("Stopping subscription processor.");
        ongoingOperationsDisposable.clear();
        LOG.info("Stopped subscription processor.");
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
                @NonNull ModelSchema modelSchema,
                @NonNull Consumer<String> onStart,
                @NonNull Consumer<GraphQLResponse<ModelWithMetadata<T>>> onResponse,
                @NonNull Consumer<DataStoreException> onFailure,
                @NonNull Action onComplete
        );
    }

    /**
     * Builds instances of {@link SubscriptionProcessor}s.
     */
    public static final class Builder implements AppSyncStep, ModelProviderStep, MergerStep,
            DataStoreConfigurationProviderStep, BuildStep {
        private AppSync appSync;
        private ModelProvider modelProvider;
        private Merger merger;
        private DataStoreConfigurationProvider dataStoreConfigurationProvider;

        @NonNull
        @Override
        public ModelProviderStep appSync(@NonNull AppSync appSync) {
            this.appSync = Objects.requireNonNull(appSync);
            return Builder.this;
        }

        @NonNull
        @Override
        public MergerStep modelProvider(@NonNull ModelProvider modelProvider) {
            this.modelProvider = Objects.requireNonNull(modelProvider);
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
        public BuildStep dataStoreConfigurationProvider(DataStoreConfigurationProvider dataStoreConfigurationProvider) {
            this.dataStoreConfigurationProvider = Objects.requireNonNull(dataStoreConfigurationProvider);
            return Builder.this;
        }

        @NonNull
        @Override
        public SubscriptionProcessor build() {
            return new SubscriptionProcessor(this);
        }
    }

    interface AppSyncStep {
        @NonNull
        ModelProviderStep appSync(@NonNull AppSync appSync);
    }

    interface ModelProviderStep {
        @NonNull
        MergerStep modelProvider(@NonNull ModelProvider modelProvider);
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
        SubscriptionProcessor build();
    }
}
