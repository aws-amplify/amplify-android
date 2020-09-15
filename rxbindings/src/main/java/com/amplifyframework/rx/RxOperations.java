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
import androidx.annotation.Nullable;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.rx.RxAdapters.CancelableBehaviors.StreamEmitter;

import java.util.Objects;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

/**
 * Top level container class created to hold Rx-specific operation types.
 */
public final class RxOperations {
    /**
     * A class that represents a subscription operation and exposes
     * observables for consumers to listen to subscription data and
     * status events.
     * @param <T> The type representing the subscription data.
     */
    public static final class RxSubscriptionOperation<T> implements Cancelable {
        private BehaviorSubject<ConnectionStateEvent> connectionStateSubject;
        private Observable<T> subscriptionData;
        private Cancelable amplifyOperation;
        private OnConnectedConsumer onConnected;

        /**
         * Constructor for RxSubscriptionOperation.
         * @param callbacks Implementation of {@link StreamEmitter} used to map callbacks
         *                  for the callback-style invocations.
         */
        public RxSubscriptionOperation(StreamEmitter<String, T, ApiException> callbacks) {
            onConnected = new OnConnectedConsumer();
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
         * Returns an {@link Observable} which consumers can use to
         * receive notfication about the status of the subscription connection. Currently,
         * only {@link ConnectionState#CONNECTED} is emitted.
         * @return Reference to the {@link Observable} that receives connection events.
         */
        public Observable<ConnectionStateEvent> observeConnectionState() {
            return connectionStateSubject;
        }

        @Override
        public void cancel() {
            connectionStateSubject.onComplete();
            if (amplifyOperation != null) {
                amplifyOperation.cancel();
            }
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

        /**
         * Describes events emitted by the {@link RxSubscriptionOperation} class.
         */
        public static final class ConnectionStateEvent {
            private ConnectionState connectionState;
            private String subscriptionId;

            ConnectionStateEvent(@NonNull ConnectionState connectionState,
                                 @Nullable String subscriptionId) {
                this.connectionState = connectionState;
                this.subscriptionId = subscriptionId;
            }

            /**
             * The connection state reported in the event.
             * @return The {@link ConnectionState} representing the state of the connection.
             */
            @NonNull
            public ConnectionState getConnectionState() {
                return connectionState;
            }

            /**
             * The subscriptionId associated with the event.
             * @return The value of the subscriptionId.
             */
            @Nullable
            public String getSubscriptionId() {
                return subscriptionId;
            }

            @Override
            public int hashCode() {
                int result = connectionState.hashCode();
                result = 31 * result + (subscriptionId != null ? subscriptionId.hashCode() : 0);
                return result;
            }

            @Override
            public boolean equals(@Nullable Object obj) {
                if (this == obj) {
                    return true;
                } else if (obj == null || getClass() != obj.getClass()) {
                    return false;
                } else {
                    ConnectionStateEvent privateNote = (ConnectionStateEvent) obj;
                    return Objects.equals(getConnectionState(), privateNote.getConnectionState()) &&
                        Objects.equals(getSubscriptionId(), privateNote.getSubscriptionId());
                }
            }

            @NonNull
            @Override
            public String toString() {
                return "ConnectionStateEvent{connectionState=" + connectionState + "," +
                    "subscriptionId=" + subscriptionId + "}";
            }
        }

        private final class OnConnectedConsumer implements Consumer<String> {
            @Override
            public void accept(@NonNull String subscriptionId) {
                connectionStateSubject.onNext(new ConnectionStateEvent(ConnectionState.CONNECTED, subscriptionId));
            }
        }
    }
}
