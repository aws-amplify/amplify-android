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

package com.amplifyframework.api.aws;

import android.net.Uri;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.StreamListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Manages the lifecycle of a single WebSocket connection,
 * and multiple GraphQL subscriptions that work on top of it.
 */
final class SubscriptionEndpoint {
    private static final int CONNECTION_ACKNOWLEDGEMENT_TIMEOUT = 30 /* seconds */;
    private static final int NORMAL_CLOSURE_STATUS = 1000;

    private final ApiConfiguration apiConfiguration;
    private final Map<String, Subscription<?>> subscriptions;
    private final GraphQLResponse.Factory responseFactory;
    private final TimeoutWatchdog timeoutWatchdog;
    private final CountDownLatch connectionAcknowledgement;

    private WebSocket webSocket;

    SubscriptionEndpoint(
            ApiConfiguration apiConfiguration,
            GraphQLResponse.Factory responseFactory) {
        this.apiConfiguration = apiConfiguration;
        this.subscriptions = new ConcurrentHashMap<>();
        this.responseFactory = responseFactory;
        this.timeoutWatchdog = new TimeoutWatchdog();
        this.connectionAcknowledgement = new CountDownLatch(1);
    }

    synchronized <T> String requestSubscription(
            @NonNull GraphQLRequest request,
            @NonNull StreamListener<GraphQLResponse<T>> responseListener,
            @NonNull Class<T> classToCast) {

        if (webSocket == null) {
            webSocket = createWebSocket();
            try {
                connectionAcknowledgement.await(CONNECTION_ACKNOWLEDGEMENT_TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException interruptedException) {
                throw new ApiException(interruptedException);
            }
        }

        final String subscriptionId = UUID.randomUUID().toString();
        try {
            webSocket.send(new JSONObject()
                .put("id", subscriptionId)
                .put("type", "start")
                .put("payload", new JSONObject()
                    .put("data", request.content())
                    .put("extensions", new JSONObject()
                        .put("authorization", SubscriptionAuthorizationHeader.from(apiConfiguration))))
                .toString()
            );
        } catch (JSONException jsonException) {
            throw new RuntimeException("Failed to construct subscription registration message.", jsonException);
        }

        Subscription<T> subscription = new Subscription<>(responseListener, responseFactory, classToCast);
        subscriptions.put(subscriptionId, subscription);
        subscription.awaitSubscriptionReady();

        return subscriptionId;
    }

    private WebSocket createWebSocket() {
        Request request = new Request.Builder()
            .url(buildConnectionRequestUrl())
            .addHeader("Sec-WebSocket-Protocol", "graphql-ws")
            .build();

        webSocket = new OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()
            .newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(@NonNull final WebSocket webSocket, @NonNull final Response response) {
                    sendConnectionInit(webSocket);
                }

                @Override
                public void onMessage(@NonNull final WebSocket webSocket, @NonNull final String message) {
                    processMessage(webSocket, message);
                }

                @Override
                public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                    webSocket.close(NORMAL_CLOSURE_STATUS, null);
                    notifyAllSubscriptionsCompleted();
                }

                @Override
                public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable failure, Response response) {
                    notifyError(failure);
                }
            });

        return webSocket;
    }

    private void sendConnectionInit(WebSocket webSocket) {
        try {
            webSocket.send(new JSONObject()
                .put("type", "connection_init")
                .toString());
        } catch (JSONException jsonException) {
            notifyError(jsonException);
        }
    }

    private void processMessage(WebSocket webSocket, String message) {
        try {
            processJsonMessage(webSocket, message);
        } catch (JSONException jsonException) {
            notifyError(jsonException);
        }
    }

    private void processJsonMessage(WebSocket webSocket, String message) throws JSONException {
        final JSONObject jsonMessage = new JSONObject(message);
        final SubscriptionMessageType subscriptionMessageType =
            SubscriptionMessageType.from(jsonMessage.getString("type"));

        switch (subscriptionMessageType) {
            case CONNECTION_ACK:
                timeoutWatchdog.start(() -> webSocket.close(NORMAL_CLOSURE_STATUS, "WebSocket closed due to timeout."),
                    Integer.parseInt(jsonMessage.getJSONObject("payload").getString("connectionTimeoutMs")));
                connectionAcknowledgement.countDown();
                break;
            case SUBSCRIPTION_ACK:
                notifySubscriptionAcknowledged(jsonMessage.getString("id"));
                break;
            case SUBSCRIPTION_COMPLETE:
                notifySubscriptionCompleted(jsonMessage.getString("id"));
                break;
            case CONNECTION_KEEP_ALIVE:
                timeoutWatchdog.reset();
                break;
            case SUBSCRIPTION_ERROR:
            case SUBSCRIPTION_DATA:
                notifySubscriptionData(jsonMessage.getString("id"), jsonMessage.getString("payload"));
                break;
            default:
                notifyError(new ApiException("Got unknown message type: " + subscriptionMessageType));
        }
    }

    private void notifySubscriptionAcknowledged(final String subscriptionId) {
        Subscription<?> subscription = subscriptions.get(subscriptionId);
        if (subscription != null) {
            subscription.acknowledgeSubscriptionReady();
        } else {
            throw new ApiException("Acknowledgement for unknown subscription: " + subscriptionId);
        }
    }

    private void notifyAllSubscriptionsCompleted() {
        // TODO: if the connection closes, but our subscription didn't ask for that,
        // is that a failure, from its standpoint? Or not?
        for (Subscription<?> dispatcher : new HashSet<>(subscriptions.values())) {
            dispatcher.dispatchCompleted();
        }
    }

    private void notifySubscriptionCompleted(String subscriptionId) {
        final Subscription<?> dispatcher = subscriptions.get(subscriptionId);
        if (dispatcher == null) {
            throw new ApiException("Got subscription completion for unknown subscription:" + subscriptionId);
        }

        dispatcher.dispatchCompleted();
        dispatcher.acknowledgeSubscriptionCompleted();
    }

    private void notifyError(Throwable error) {
        for (Subscription<?> dispatcher : new HashSet<>(subscriptions.values())) {
            dispatcher.dispatchError(new ApiException("Subscription failed.", error));
        }
    }

    private void notifySubscriptionData(String subscriptionId, String data) {
        final Subscription<?> dispatcher = subscriptions.get(subscriptionId);
        if (dispatcher == null) {
            throw new ApiException("Got subscription data for unknown subscription ID: " + subscriptionId);
        }
        dispatcher.dispatchNextMessage(data);
    }

    synchronized void releaseSubscription(String subscriptionId) {
        final Subscription<?> subscription = subscriptions.get(subscriptionId);
        if (subscription == null) {
            throw new IllegalArgumentException("No existing subscription with the given id.");
        }

        try {
            webSocket.send(new JSONObject()
                .put("type", "stop")
                .put("id", subscriptionId)
                .toString());
        } catch (JSONException jsonException) {
            throw new RuntimeException("Failed to construct subscription release message.", jsonException);
        }

        subscription.awaitSubscriptionCompleted();
        subscriptions.remove(subscriptionId);

        // If we have zero subscriptions, close the WebSocket
        if (subscriptions.size() == 0) {
            timeoutWatchdog.stop();
            webSocket.close(NORMAL_CLOSURE_STATUS, "No active subscriptions");
        }
    }

    /*
     * Discover WebSocket endpoint from the AppSync endpoint.
     * AppSync endpoint : https://xxxxxxxxxxxx.appsync-api.ap-southeast-2.amazonaws.com/graphql
     * Discovered WebSocket endpoint : wss:// xxxxxxxxxxxx.appsync-realtime-api.ap-southeast-2.amazonaws.com/graphql
     */
    private String buildConnectionRequestUrl() {
        // Construct the authorization header for connection request
        final byte[] rawHeader = SubscriptionAuthorizationHeader.from(apiConfiguration)
            .toString()
            .getBytes();

        URL appSyncEndpoint = null;
        try {
            appSyncEndpoint = new URL(apiConfiguration.getEndpoint());
        } catch (MalformedURLException malformedUrlException) {
            // throwing in a second ...
        }
        if (appSyncEndpoint == null) {
            throw new RuntimeException("Malformed Api Url" + apiConfiguration.getEndpoint());
        }

        return new Uri.Builder()
            .scheme("wss")
            .authority(appSyncEndpoint.getHost()
                .replace("appsync-api", "appsync-realtime-api"))
            .appendPath(appSyncEndpoint.getPath())
            .appendQueryParameter("header", Base64.encodeToString(rawHeader, Base64.DEFAULT))
            .appendQueryParameter("payload", "e30=")
            .build()
            .toString();
    }

    static final class Subscription<T> {
        private static final int ACKNOWLEDGEMENT_TIMEOUT = 10 /* seconds */;

        private final StreamListener<GraphQLResponse<T>> responseListener;
        private final GraphQLResponse.Factory responseFactory;
        private final Class<T> classToCast;
        private final CountDownLatch subscriptionReadyAcknowledgment;
        private final CountDownLatch subscriptionCompletionAcknowledgement;

        Subscription(
                StreamListener<GraphQLResponse<T>> responseListener,
                GraphQLResponse.Factory responseFactory,
                Class<T> classToCast) {
            this.responseListener = responseListener;
            this.responseFactory = responseFactory;
            this.classToCast = classToCast;
            this.subscriptionReadyAcknowledgment = new CountDownLatch(1);
            this.subscriptionCompletionAcknowledgement = new CountDownLatch(1);
        }

        void acknowledgeSubscriptionReady() {
            subscriptionReadyAcknowledgment.countDown();
        }

        void awaitSubscriptionReady() {
            try {
                if (!subscriptionReadyAcknowledgment.await(ACKNOWLEDGEMENT_TIMEOUT, TimeUnit.SECONDS)) {
                    dispatchError(new ApiException("Subscription not acknowledged."));
                }
            } catch (InterruptedException interruptedException) {
                dispatchError(new ApiException(
                    "Failure awaiting subscription acknowledgement.",
                    interruptedException
                ));
            }
        }

        void acknowledgeSubscriptionCompleted() {
            subscriptionCompletionAcknowledgement.countDown();
        }

        void awaitSubscriptionCompleted() {
            try {
                if (!subscriptionCompletionAcknowledgement.await(ACKNOWLEDGEMENT_TIMEOUT, TimeUnit.SECONDS)) {
                    dispatchError(new ApiException("Subscription completion not acknowledged."));
                }
            } catch (InterruptedException interruptedException) {
                dispatchError(new ApiException(
                    "Failure awaiting acknowledgement of subscription completion.",
                    interruptedException
                ));
            }
        }

        void dispatchNextMessage(String message) {
            responseListener.onNext(responseFactory.buildResponse(message, classToCast));
        }

        void dispatchError(Throwable error) {
            responseListener.onError(error);
        }

        void dispatchCompleted() {
            responseListener.onComplete();
        }

        @SuppressWarnings("LineLength")
        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            Subscription<?> that = (Subscription<?>) thatObject;

            if (!ObjectsCompat.equals(responseListener, that.responseListener)) {
                return false;
            }
            if (!ObjectsCompat.equals(responseFactory, that.responseFactory)) {
                return false;
            }
            if (!ObjectsCompat.equals(classToCast, that.classToCast)) {
                return false;
            }
            if (!ObjectsCompat.equals(subscriptionReadyAcknowledgment, that.subscriptionReadyAcknowledgment)) {
                return false;
            }
            return ObjectsCompat.equals(subscriptionCompletionAcknowledgement, that.subscriptionCompletionAcknowledgement);
        }

        @SuppressWarnings("checkstyle:MagicNumber")
        @Override
        public int hashCode() {
            int result = responseListener != null ? responseListener.hashCode() : 0;
            result = 31 * result + (responseFactory != null ? responseFactory.hashCode() : 0);
            result = 31 * result + (classToCast != null ? classToCast.hashCode() : 0);
            result = 31 * result + subscriptionReadyAcknowledgment.hashCode();
            result = 31 * result + subscriptionCompletionAcknowledgement.hashCode();
            return result;
        }
    }
}
