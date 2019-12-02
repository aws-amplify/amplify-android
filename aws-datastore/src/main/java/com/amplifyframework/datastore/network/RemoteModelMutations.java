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

import androidx.annotation.WorkerThread;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.StreamListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.logging.Logger;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.Emitter;
import io.reactivex.Observable;

class RemoteModelMutations {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final ApiCategoryBehavior api;
    private final ConfiguredApiProvider apiNameProvider;
    private final ModelProvider modelProvider;
    private final Set<Subscription<? extends Model>> subscriptions;

    RemoteModelMutations(
            ApiCategoryBehavior api,
            ConfiguredApiProvider apiNameProvider,
            ModelProvider modelProvider) {
        this.modelProvider = modelProvider;
        this.api = api;
        this.apiNameProvider = apiNameProvider;
        this.subscriptions = new HashSet<>();
    }

    @WorkerThread
    Observable<Mutation<? extends Model>> observe() {
        return Observable.defer(() -> Observable.create(emitter -> {
            emitter.setCancellable(() -> {
                synchronized (subscriptions) {
                    for (Subscription<? extends Model> subscription : subscriptions) {
                        subscription.end();
                    }
                    subscriptions.clear();
                }
            });

            for (Class<? extends Model> modelClass : modelProvider.models()) {
                for (SubscriptionType subscriptionType : SubscriptionType.values()) {
                    subscriptions.add(Subscription.request()
                        .api(api)
                        .apiNameProvider(apiNameProvider)
                        .modelClass(modelClass)
                        .subscriptionType(subscriptionType)
                        .commonEmitter(emitter)
                        .begin());
                }
            }
        }));
    }

    static final class Subscription<T extends Model> {
        private final GraphQLOperation<T> operation;

        Subscription(final GraphQLOperation<T> operation) {
            this.operation = operation;
        }

        synchronized void end() {
            operation.cancel();
        }

        static <T extends Model> Request<T> request() {
            return new Request<>();
        }

        /**
         * A request to begin a subscription to mutations for a given type of model.
         * @param <T> Type of model for which to subscribe to mutations
         */
        static final class Request<T extends Model> {
            private ApiCategoryBehavior api;
            private ConfiguredApiProvider apiNameProvider;
            private Class<T> modelClass;
            private SubscriptionType subscriptionType;
            private Emitter<Mutation<? extends Model>> commonEmitter;

            Request<T> api(ApiCategoryBehavior api) {
                this.api = api;
                return this;
            }

            Request<T> apiNameProvider(ConfiguredApiProvider apiNameProvider) {
                this.apiNameProvider = apiNameProvider;
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

            Subscription<T> begin() throws DataStoreException {
                String apiName = apiNameProvider.getDataStoreApiName();
                EmittingSubscriptionListener<T> listener =
                    new EmittingSubscriptionListener<>(commonEmitter, modelClass, subscriptionType);
                return new Subscription<>(api.subscribe(apiName, modelClass, subscriptionType, listener));
            }
        }

        /**
         * A listener to the {@link ApiCategoryBehavior#subscribe(String, GraphQLRequest, StreamListener)},
         * which responds to new data items by posting them onto an Rx {@link Emitter}. The intention
         * is for a single {@link Emitter} to be shared among several different implementations of this
         * listener.
         * @param <T> Type type of data being received
         */
        static final class EmittingSubscriptionListener<T extends Model> implements StreamListener<GraphQLResponse<T>> {
            private final Emitter<Mutation<? extends Model>> commonEmitter;
            private final Class<T> modelClazz;
            private final SubscriptionType subscriptionType;

            EmittingSubscriptionListener(
                    Emitter<Mutation<? extends Model>> commonEmitter,
                    Class<T> modelClazz,
                    SubscriptionType subscriptionType) {
                this.commonEmitter = commonEmitter;
                this.modelClazz = modelClazz;
                this.subscriptionType = subscriptionType;
            }

            @Override
            public void onNext(GraphQLResponse<T> item) {
                if (item.hasErrors()) {
                    commonEmitter.onError(new DataStoreException(
                            String.format(
                                "Errors on subscription %s:%s: %s.",
                                modelClazz.getSimpleName(), subscriptionType, item.getErrors()
                            ),
                            AmplifyException.TODO_RECOVERY_SUGGESTION
                    ));
                } else if (!item.hasData()) {
                    commonEmitter.onError(new DataStoreException(
                            String.format(
                                "Empty data received for %s:%s subscription.",
                                modelClazz.getSimpleName(), subscriptionType
                            ),
                            AmplifyException.TODO_RECOVERY_SUGGESTION
                    ));
                } else {
                    commonEmitter.onNext(Mutation.<T>builder()
                        .model(item.getData())
                        .modelClass(modelClazz)
                        .type(fromSubscriptionType(subscriptionType))
                        .build());
                }
            }

            @Override
            public void onComplete() {
                LOG.info(String.format(
                    "Subscription to %s:%s is completed.",
                    modelClazz.getSimpleName(), subscriptionType
                ));
            }

            @Override
            public void onError(Throwable error) {
                commonEmitter.onError(error);
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
        }
    }
}
