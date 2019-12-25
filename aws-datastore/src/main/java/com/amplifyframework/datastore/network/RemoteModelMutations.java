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

import android.annotation.SuppressLint;
import androidx.annotation.WorkerThread;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.StreamListener;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.logging.Logger;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.Emitter;
import io.reactivex.Observable;

final class RemoteModelMutations {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final AppSyncEndpoint appSyncEndpoint;
    private final ModelProvider modelProvider;
    private final Set<Subscription> subscriptions;

    RemoteModelMutations(
            AppSyncEndpoint appSyncEndpoint,
            ModelProvider modelProvider) {
        this.modelProvider = modelProvider;
        this.appSyncEndpoint = appSyncEndpoint;
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
                            .appSyncEndpoint(appSyncEndpoint)
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
         * @param <T> Type of model for which to subscribe to mutations
         */
        static final class Request<T extends Model> {
            private AppSyncEndpoint appSyncEndpoint;
            private Class<T> modelClass;
            private SubscriptionType subscriptionType;
            private Emitter<Mutation<? extends Model>> commonEmitter;

            Request<T> appSyncEndpoint(AppSyncEndpoint appSyncEndpoint) {
                this.appSyncEndpoint = appSyncEndpoint;
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

            Subscription begin() throws DataStoreException {
                StreamListener<GraphQLResponse<ModelWithMetadata<T>>, DataStoreException> listener =
                    SubscriptionFunnel.instance(commonEmitter, modelClass, subscriptionType);

                final Cancelable cancelable;
                switch (subscriptionType) {
                    case ON_UPDATE:
                        cancelable = appSyncEndpoint.onUpdate(modelClass, listener);
                        break;
                    case ON_DELETE:
                        cancelable = appSyncEndpoint.onDelete(modelClass, listener);
                        break;
                    case ON_CREATE:
                        cancelable = appSyncEndpoint.onCreate(modelClass, listener);
                        break;
                    default:
                        throw new DataStoreException(
                            "Failed to establish a model subscription.",
                            "Was a new subscription type created?"
                        );
                }
                return new Subscription(cancelable);
            }
        }

        /**
         * "Funnels" subscription events for a specific type, onto an emitter that is
         * shared by all types of events, and for all subscription types.
         * A listener to the {@link AppSyncEndpoint},
         * which responds to new data items by posting them onto an Rx {@link Emitter}. The intention
         * is for a single {@link Emitter} to be shared among several different implementations of this
         * listener.
         */
        static final class SubscriptionFunnel {
            @SuppressWarnings("checkstyle:all") SubscriptionFunnel() {}

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

            @SuppressLint("SyntheticAccessor")
            static <T extends Model> StreamListener<GraphQLResponse<ModelWithMetadata<T>>, DataStoreException> instance(
                    Emitter<Mutation<? extends Model>> commonEmitter,
                    Class<T> modelClazz,
                    SubscriptionType subscriptionType) {

                final Consumer<GraphQLResponse<ModelWithMetadata<T>>> itemConsumer = response -> {
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
                            .model(response.getData().getModel())
                            .modelClass(modelClazz)
                            .type(fromSubscriptionType(subscriptionType))
                            .build());
                    }
                };

                //noinspection CodeBlock2Expr
                final Action completionAction = () -> {
                    LOG.info(String.format(
                        "Subscription to %s:%s is completed.",
                        modelClazz.getSimpleName(), subscriptionType
                    ));
                };

                return StreamListener.instance(itemConsumer, commonEmitter::onError, completionAction);
            }
        }
    }
}
