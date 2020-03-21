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
import androidx.annotation.WorkerThread;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.logging.Logger;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import io.reactivex.Emitter;
import io.reactivex.Observable;

/**
 * An implementation detail of the {@link SubscriptionProcessor}.
 *
 * The {@link RemoteModelMutations} asks the {@link ModelProvider} for the collection
 * of models that should be managed. For each, a subscription is formed via the {@link AppSync}
 * client.
 *
 * Any/all responses received on those multiple subscriptions get batched together into a single
 * observable stream. The {@link RemoteModelMutations#observe()} is the single intended top-level
 * entry to this class, and its use is to monitor subscription data for all managed models,
 * over a single {@link Observable}.
 */
final class RemoteModelMutations {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final AppSync appSync;
    private final ModelProvider modelProvider;
    private final Set<Subscription> subscriptions;

    /**
     * Constructs a new RemoteModelMutations.
     * @param appSync An interface to an AppSync endpoint
     * @param modelProvider Provides the model classes managed by this system
     */
    RemoteModelMutations(
            @NonNull AppSync appSync,
            @NonNull ModelProvider modelProvider) {
        this.appSync = Objects.requireNonNull(appSync);
        this.modelProvider = Objects.requireNonNull(modelProvider);
        this.subscriptions = new HashSet<>();
    }

    @WorkerThread
    Observable<Mutation<? extends Model>> observe() {
        return Observable.defer(() -> Observable.create(emitter -> {
            emitter.setCancellable(() -> {
                synchronized (subscriptions) {
                    for (final Subscription subscription : subscriptions) {
                        subscription.end();
                    }
                    subscriptions.clear();
                }
            });

            synchronized (subscriptions) {
                for (Class<? extends Model> modelClass : modelProvider.models()) {
                    for (SubscriptionType subscriptionType : SubscriptionType.values()) {
                        subscriptions.add(Subscription.request()
                            .appSync(appSync)
                            .modelClass(modelClass)
                            .subscriptionType(subscriptionType)
                            .commonEmitter(emitter)
                            .begin());
                    }
                }
            }
        }));
    }

    static final class Subscription {
        private final Cancelable cancelable;

        Subscription(final Cancelable cancelable) {
            this.cancelable = cancelable;
        }

        synchronized void end() {
            cancelable.cancel();
        }

        static <T extends Model> Request<T> request() {
            return new Request<>();
        }

        /**
         * A request to begin a subscription to mutations for a given type of model.
         *
         * @param <T> Type of model for which to subscribe to mutations
         */
        static final class Request<T extends Model> {
            private AppSync appSync;
            private Class<T> modelClass;
            private SubscriptionType subscriptionType;
            private Emitter<Mutation<? extends Model>> commonEmitter;

            Request<T> appSync(AppSync appSync) {
                this.appSync = appSync;
                return this;
            }

            @SuppressWarnings("unchecked") // TODO: can this be improved?
            Request<T> modelClass(Class<? extends Model> modelClass) {
                this.modelClass = (Class<T>) modelClass;
                return this;
            }

            Request<T> subscriptionType(SubscriptionType subscriptionType) {
                this.subscriptionType = subscriptionType;
                return this;
            }

            Request<T> commonEmitter(Emitter<Mutation<? extends Model>> commonEmitter) {
                this.commonEmitter = commonEmitter;
                return this;
            }

            @SuppressWarnings("checkstyle:WhitespaceAround")
            @SuppressLint("SyntheticAccessor")
            Subscription begin() throws DataStoreException {
                final Consumer<String> onStarted = ignored -> {};

                final Consumer<GraphQLResponse<ModelWithMetadata<T>>> onNext =
                    itemConsumer(commonEmitter, modelClass, subscriptionType);

                final Consumer<DataStoreException> onFailure = commonEmitter::onError;

                final Action onComplete = () -> LOG.info(String.format(
                    "Subscription to %s:%s is completed.", modelClass.getSimpleName(), subscriptionType
                ));

                final Cancelable cancelable;
                switch (subscriptionType) {
                    case ON_UPDATE:
                        cancelable = appSync.onUpdate(modelClass, onStarted, onNext, onFailure, onComplete);
                        break;
                    case ON_DELETE:
                        cancelable = appSync.onDelete(modelClass, onStarted, onNext, onFailure, onComplete);
                        break;
                    case ON_CREATE:
                        cancelable = appSync.onCreate(modelClass, onStarted, onNext, onFailure, onComplete);
                        break;
                    default:
                        throw new DataStoreException(
                            "Failed to establish a model subscription.",
                            "Was a new subscription type created?"
                        );
                }
                return new Subscription(cancelable);
            }

            private static Mutation.Type fromSubscriptionType(SubscriptionType subscriptionType) {
                switch (subscriptionType) {
                    case ON_CREATE:
                        return Mutation.Type.CREATE;
                    case ON_DELETE:
                        return Mutation.Type.DELETE;
                    case ON_UPDATE:
                        return Mutation.Type.UPDATE;
                    default:
                        throw new IllegalArgumentException("Unknown subscription type: " + subscriptionType);
                }
            }

            private static <T extends Model> Consumer<GraphQLResponse<ModelWithMetadata<T>>> itemConsumer(
                Emitter<Mutation<? extends Model>> commonEmitter,
                Class<T> modelClazz,
                SubscriptionType subscriptionType) {
                return response -> {
                    if (response.hasErrors()) {
                        commonEmitter.onError(new DataStoreException(
                            String.format(
                                "Errors on subscription %s:%s: %s.",
                                modelClazz.getSimpleName(), subscriptionType, response.getErrors()
                            ),
                            AmplifyException.TODO_RECOVERY_SUGGESTION
                        ));
                    } else if (!response.hasData()) {
                        commonEmitter.onError(new DataStoreException(
                            String.format(
                                "Empty data received for %s:%s subscription.",
                                modelClazz.getSimpleName(), subscriptionType
                            ),
                            AmplifyException.TODO_RECOVERY_SUGGESTION
                        ));
                    } else {
                        commonEmitter.onNext(Mutation.<T>builder()
                            .modelWithMetadata(response.getData())
                            .modelClass(modelClazz)
                            .type(fromSubscriptionType(subscriptionType))
                            .build());
                    }
                };
            }
        }
    }
}
