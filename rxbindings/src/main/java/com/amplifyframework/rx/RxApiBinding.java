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

package com.amplifyframework.rx;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.rest.RestOptions;
import com.amplifyframework.api.rest.RestResponse;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.rx.RxAdapters.CancelableBehaviors;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

/**
 * An implementation of the RxApiCategoryBehavior which satisfies the API contract by wrapping
 * {@link ApiCategoryBehavior} in Rx primitives.
 */
public final class RxApiBinding implements RxApiCategoryBehavior {
    private final ApiCategoryBehavior api;

    RxApiBinding() {
        this(Amplify.API);
    }

    @VisibleForTesting
    RxApiBinding(ApiCategory api) {
        this.api = api;
    }

    @NonNull
    @Override
    public <T> Single<GraphQLResponse<T>> query(@NonNull GraphQLRequest<T> graphQlRequest) {
        return toSingle((onResult, onError) -> api.query(graphQlRequest, onResult, onError));
    }

    @NonNull
    @Override
    public <T> Single<GraphQLResponse<T>> query(
            @NonNull String apiName, @NonNull GraphQLRequest<T> graphQlRequest) {
        return toSingle((onResult, onError) -> api.query(apiName, graphQlRequest, onResult, onError));
    }

    @NonNull
    @Override
    public <T> Single<GraphQLResponse<T>> mutate(@NonNull GraphQLRequest<T> graphQlRequest) {
        return toSingle((onResult, onError) -> api.mutate(graphQlRequest, onResult, onError));
    }

    @NonNull
    @Override
    public <T> Single<GraphQLResponse<T>> mutate(
            @NonNull String apiName, @NonNull GraphQLRequest<T> graphQlRequest) {
        return toSingle((onResult, onError) -> api.mutate(apiName, graphQlRequest, onResult, onError));
    }

    @NonNull
    @Override
    public <T> RxSubscriptionOperation<GraphQLResponse<T>> subscribe(@NonNull GraphQLRequest<T> graphQlRequest) {
        return new RxSubscriptionOperation<GraphQLResponse<T>>((onStart, onItem, onError, onComplete) -> {
            return api.subscribe(graphQlRequest, onStart, onItem, onError, onComplete);
        });
    }

    @NonNull
    @Override
    public <T> RxSubscriptionOperation<GraphQLResponse<T>> subscribe(@NonNull String apiName,
                                                                     @NonNull GraphQLRequest<T> graphQlRequest) {
        return new RxSubscriptionOperation<GraphQLResponse<T>>((onStart, onItem, onError, onComplete) -> {
            return api.subscribe(apiName, graphQlRequest, onStart, onItem, onError, onComplete);
        });
    }

    @NonNull
    @Override
    public Single<RestResponse> get(@NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.get(request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> get(@NonNull String apiName, @NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.get(apiName, request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> put(@NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.put(request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> put(@NonNull String apiName, @NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.put(apiName, request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> post(@NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.post(request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> post(@NonNull String apiName, @NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.post(apiName, request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> delete(@NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.delete(request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> delete(@NonNull String apiName, @NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.delete(apiName, request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> head(@NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.head(request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> head(@NonNull String apiName, @NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.head(apiName, request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> patch(@NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.patch(request, onResult, onError));
    }

    @NonNull
    @Override
    public Single<RestResponse> patch(@NonNull String apiName, @NonNull RestOptions request) {
        return toSingle((onResult, onError) -> api.patch(apiName, request, onResult, onError));
    }

    private <T> Single<T> toSingle(CancelableBehaviors.ResultEmitter<T, ApiException> method) {
        return CancelableBehaviors.toSingle(method);
    }

    private <T> Observable<T> toObservable(
            CancelableBehaviors.StreamEmitter<String, T, ApiException> method) {
        return CancelableBehaviors.toObservable(method);
    }

    /**
     * A class that represents a subscription operation and exposes
     * observables for consumers to listen to subscription data and
     * status events.
     * @param <T> The type representing the subscription data.
     */
    public static final class RxSubscriptionOperation<T> implements Cancelable {
        private BehaviorSubject<ConnectionState> connectionStateSubject;
        private Observable<T> subscriptionData;
        private Cancelable amplifyOperation;
        private String subscriptionId;

        private Consumer<String> onConnected = new Consumer<String>() {
            @Override
            public void accept(@NonNull String subscriptionId) {
                RxSubscriptionOperation.this.subscriptionId = subscriptionId;
                connectionStateSubject.onNext(ConnectionState.CONNECTED);
            }
        };

        RxSubscriptionOperation(CancelableBehaviors.StreamEmitter<String, T, ApiException> callbacks) {
            connectionStateSubject = BehaviorSubject.create();
            subscriptionData = Observable.create(emitter -> {
                amplifyOperation = callbacks.streamTo(onConnected::accept,
                                                      emitter::onNext,
                                                      emitter::onError,
                                                      emitter::onComplete);
            });
        }

        /**
         * Returns an {@link Observable} which consumers can use to
         * retrieve data received by the subscription operation.
         * @return Reference to the {@link Observable} with subscription data.
         */
        public Observable<T> observeSubscriptionData() {
            return subscriptionData;
        }

        /**
         * Once the subscription starts, this method returns
         * the value of the subscriptionId.
         * @return The value of the subscriptionId.
         */
        public String getSubscriptionId() {
            return subscriptionId;
        }

        /**
         * Returns an {@link Observable} which consumers can use to
         * receive notfication about the status of the subscription connection. Currently,
         * only {@link ConnectionState#CONNECTED} is emitted.
         * @return Reference to the {@link Observable} that receives connection events.
         */
        public Observable<ConnectionState> observeConnectionState() {
            return connectionStateSubject;
        }

        @Override
        public void cancel() {
            if (amplifyOperation != null) {
                amplifyOperation.cancel();
            }
            connectionStateSubject.onComplete();
        }

        /**
         * Enum representing connection states of a
         * subscription operation.
         */
        public enum ConnectionState {
            /**
             * The subscription successfully established a connection and is
             * ready to receive data.
             */
            CONNECTED
        }
    }
}
