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
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.AmplifyDisposables;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreException.GraphQLResponseException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.AppSyncExtensions;
import com.amplifyframework.datastore.appsync.AppSyncExtensions.AppSyncErrorType;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.Empty;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.UnicastSubject;

/**
 * Observes mutations occurring on a remote {@link AppSync} system. The mutations arrive
 * over a long-lived subscription, as {@link SubscriptionEvent}s.
 * For every type of model provided by a {@link ModelProvider}, the SubscriptionProcessor
 * marries mutated models back into the local DataStore, through the {@link Merger}.
 */
final class SubscriptionProcessor {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private static final long TIMEOUT_SECONDS_PER_MODEL = 20;
    private static final long NETWORK_OP_TIMEOUT_SECONDS = 60;

    private final AppSync appSync;
    private final ModelProvider modelProvider;
    private final SchemaRegistry schemaRegistry;
    private final Merger merger;
    private final QueryPredicateProvider queryPredicateProvider;
    private final Consumer<Throwable> onFailure;
    private final CompositeDisposable ongoingOperationsDisposable;
    private final long adjustedTimeoutSeconds;
    private UnicastSubject<SubscriptionEvent<? extends Model>> buffer;

    /**
     * Constructs a new SubscriptionProcessor.
     * @param builder A SubscriptionProcessor Builder.
     */
    private SubscriptionProcessor(Builder builder) {
        this.appSync = builder.appSync;
        this.modelProvider = builder.modelProvider;
        this.merger = builder.merger;
        this.queryPredicateProvider = builder.queryPredicateProvider;
        this.onFailure = builder.onFailure;
        this.schemaRegistry = builder.schemaRegistry;

        this.ongoingOperationsDisposable = new CompositeDisposable();

        // Operation times out after 60 seconds. If there are more than 5 models,
        // then 20 seconds are added to the timer per additional model count.
        this.adjustedTimeoutSeconds = Math.max(
            NETWORK_OP_TIMEOUT_SECONDS,
            TIMEOUT_SECONDS_PER_MODEL * Math.max(
                    modelProvider.models().size(),
                    modelProvider.modelSchemas().size()
            )
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
    synchronized void startSubscriptions() throws DataStoreException {
        int subscriptionCount = modelProvider.modelNames().size() * SubscriptionType.values().length;
        // Create a latch with the number of subscriptions are requesting. Each of these will be
        // counted down when each subscription's onStarted event is called.
        AbortableCountDownLatch<DataStoreException> latch = new AbortableCountDownLatch<>(subscriptionCount);

        // Need to create a new buffer so we can properly handle retries and stop/start scenarios.
        // Re-using the same buffer has some unexpected results due to the queueing aspect of the subject.
        buffer = UnicastSubject.create();

        Set<Observable<SubscriptionEvent<? extends Model>>> subscriptions = new HashSet<>();
        for (ModelSchema modelSchema : modelProvider.modelSchemas().values()) {
            for (SubscriptionType subscriptionType : SubscriptionType.values()) {
                subscriptions.add(subscriptionObservable(appSync, subscriptionType, latch, modelSchema));
            }
        }

        ongoingOperationsDisposable.add(Observable.merge(subscriptions)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .doOnSubscribe(disposable -> LOG.info("Starting processing subscription events."))
            .doOnError(failure -> LOG.warn("Reading subscription events has failed.", failure))
            .doOnComplete(() -> LOG.warn("Reading subscription events is completed."))
            .subscribe(buffer::onNext, buffer::onError, buffer::onComplete)
        );

        boolean subscriptionsStarted;
        try {
            LOG.debug("Waiting for subscriptions to start.");
            subscriptionsStarted = latch.abortableAwait(adjustedTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            LOG.warn("Subscription operations were interrupted during setup.");
            return;
        }

        if (subscriptionsStarted) {
            Amplify.Hub.publish(HubChannel.DATASTORE,
                                HubEvent.create(DataStoreChannelEventName.SUBSCRIPTIONS_ESTABLISHED));
            LOG.info(String.format(Locale.US,
                "Started subscription processor for models: %s of types %s.",
                modelProvider.modelNames(), Arrays.toString(SubscriptionType.values())
            ));
        } else {
            throw new DataStoreException("Timed out waiting for subscription processor to start.", "Retry");
        }
    }

    private boolean isExceptionType(DataStoreException exception, AppSyncErrorType errorType) {
        if (exception instanceof GraphQLResponseException) {
            List<GraphQLResponse.Error> errors = ((GraphQLResponseException) exception).getErrors();
            GraphQLResponse.Error firstError = errors.get(0);
            if (Empty.check(firstError.getExtensions())) {
                return false;
            }
            AppSyncExtensions extensions = new AppSyncExtensions(firstError.getExtensions());
            return errorType.equals(extensions.getErrorType());
        }
        return false;
    }

    private <T extends Model> Observable<SubscriptionEvent<? extends Model>>
            subscriptionObservable(AppSync appSync,
                                   SubscriptionType subscriptionType,
                                   AbortableCountDownLatch<DataStoreException> latch,
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
                    if (isExceptionType(dataStoreException, AppSyncErrorType.UNAUTHORIZED)) {
                        // Ignore Unauthorized errors, so that DataStore can still be used even if the user is only
                        // authorized to read a subset of the models.
                        latch.countDown();
                        LOG.warn("Unauthorized failure:" + subscriptionType.name() + " " + modelSchema.getName());
                    } else if (isExceptionType(dataStoreException, AppSyncErrorType.OPERATION_DISABLED)) {
                        // Ignore OperationDisabled errors, so that DataStore can be used even without subscriptions.
                        // This logic is only in place to address a specific use case, and should not be used without
                        // unless you have consulted with AWS.  It is subject to be deprecated/removed in the future.
                        latch.countDown();
                        LOG.warn("Operation disabled:" + subscriptionType.name() + " " + modelSchema.getName());
                    } else {
                        if (latch.getCount() > 0) {
                            // An error occurred during startup.  Abort and notify the Orchestrator by throwing the
                            // exception from startSubscriptions.
                            latch.abort(dataStoreException);
                        } else {
                            // An error occurred after startup. Notify the Orchestrator via the onFailure action.
                            onFailure.accept(dataStoreException);
                        }
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
            QueryPredicate predicate = queryPredicateProvider.getPredicate(modelSchema.getName());
            return predicate.evaluate(modelWithMetadata.getModel());
        })
        .map(modelWithMetadata -> SubscriptionEvent.<T>builder()
            .type(fromSubscriptionType(subscriptionType))
            .modelWithMetadata(modelWithMetadata)
            .modelSchema(modelSchema)
            .build()
        );
    }

    /**
     * Start draining mutations out of the mutation buffer.
     * This should be called after {@link #startSubscriptions()}.
     */
    void startDrainingMutationBuffer() {
        ongoingOperationsDisposable.add(
            buffer
                .doOnSubscribe(disposable -> LOG.info("Starting processing subscription data buffer."))
                .flatMapCompletable(this::mergeEvent)
                .doOnError(failure -> LOG.warn("Reading subscriptions buffer has failed.", failure))
                .doOnComplete(() -> LOG.warn("Reading from subscriptions buffer is completed."))
                .subscribe()
        );
    }

    private Completable mergeEvent(SubscriptionEvent<? extends Model> event) {
        ModelWithMetadata<? extends Model> original = event.modelWithMetadata();
        if (original.getModel() instanceof SerializedModel) {
            SerializedModel originalModel = (SerializedModel) original.getModel();
            SerializedModel newModel = SerializedModel.builder()
                    .modelSchema(event.modelSchema())
                    .serializedData(SerializedModel.parseSerializedData(
                            originalModel.getSerializedData(),
                            event.modelSchema().getName(),
                            schemaRegistry
                    ))
                    .build();
            return merger.merge(new ModelWithMetadata<>(newModel, original.getSyncMetadata()));
        } else {
            return merger.merge(original);
        }
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
    public static final class Builder implements AppSyncStep, ModelProviderStep, SchemaRegistryStep, MergerStep,
            QueryPredicateProviderStep, OnFailureStep, BuildStep {
        private AppSync appSync;
        private ModelProvider modelProvider;
        private Merger merger;
        private QueryPredicateProvider queryPredicateProvider;
        private Consumer<Throwable> onFailure;
        private SchemaRegistry schemaRegistry;

        @NonNull
        @Override
        public ModelProviderStep appSync(@NonNull AppSync appSync) {
            this.appSync = Objects.requireNonNull(appSync);
            return Builder.this;
        }

        @NonNull
        @Override
        public SchemaRegistryStep modelProvider(@NonNull ModelProvider modelProvider) {
            this.modelProvider = Objects.requireNonNull(modelProvider);
            return Builder.this;
        }

        @NonNull
        @Override
        public MergerStep schemaRegistry(@NonNull SchemaRegistry schemaRegistry) {
            this.schemaRegistry = Objects.requireNonNull(schemaRegistry);
            return Builder.this;
        }

        @NonNull
        @Override
        public QueryPredicateProviderStep merger(@NonNull Merger merger) {
            this.merger = Objects.requireNonNull(merger);
            return Builder.this;
        }

        @NonNull
        @Override
        public OnFailureStep queryPredicateProvider(QueryPredicateProvider queryPredicateProvider) {
            this.queryPredicateProvider = Objects.requireNonNull(queryPredicateProvider);
            return Builder.this;
        }

        @NonNull
        @Override
        public SubscriptionProcessor build() {
            return new SubscriptionProcessor(this);
        }

        @NonNull
        @Override
        public BuildStep onFailure(Consumer<Throwable> onFailure) {
            this.onFailure = Objects.requireNonNull(onFailure);
            return Builder.this;
        }
    }

    interface AppSyncStep {
        @NonNull
        ModelProviderStep appSync(@NonNull AppSync appSync);
    }

    interface ModelProviderStep {
        @NonNull
        SchemaRegistryStep modelProvider(@NonNull ModelProvider modelProvider);
    }

    interface SchemaRegistryStep {
        @NonNull
        MergerStep schemaRegistry(@NonNull SchemaRegistry schemaRegistry);
    }

    interface MergerStep {
        @NonNull
        QueryPredicateProviderStep merger(@NonNull Merger merger);
    }

    interface QueryPredicateProviderStep {
        @NonNull
        OnFailureStep queryPredicateProvider(QueryPredicateProvider queryPredicateProvider);
    }

    interface OnFailureStep {
        @NonNull
        BuildStep onFailure(Consumer<Throwable> onFailure);
    }

    interface BuildStep {
        @NonNull
        SubscriptionProcessor build();
    }
}
