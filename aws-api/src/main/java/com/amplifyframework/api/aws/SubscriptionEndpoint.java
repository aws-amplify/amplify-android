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

import android.annotation.SuppressLint;
import android.net.Uri;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
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
    private final CountDownLatch connectionResponse;
    private String connectionFailure;
    private WebSocket webSocket;

    SubscriptionEndpoint(
            @NonNull ApiConfiguration apiConfiguration,
            @NonNull GraphQLResponse.Factory responseFactory) {
        this.apiConfiguration = Objects.requireNonNull(apiConfiguration);
        this.subscriptions = new ConcurrentHashMap<>();
        this.responseFactory = Objects.requireNonNull(responseFactory);
        this.timeoutWatchdog = new TimeoutWatchdog();
        this.connectionResponse = new CountDownLatch(1);
    }

    synchronized <T> String requestSubscription(
            @NonNull GraphQLRequest<T> request,
            @NonNull Consumer<String> onSubscriptionStarted,
            @NonNull Consumer<GraphQLResponse<T>> onNextItem,
            @NonNull Consumer<ApiException> onSubscriptionError,
            @NonNull Action onSubscriptionComplete) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(onSubscriptionStarted);
        Objects.requireNonNull(onNextItem);
        Objects.requireNonNull(onSubscriptionError);
        Objects.requireNonNull(onSubscriptionComplete);

        if (webSocket == null) {
            try {
                connectionFailure = null;
                webSocket = createWebSocket();
            } catch (ApiException exception) {
                onSubscriptionError.accept(new ApiException(
                        "Failed to create websocket for subscription",
                        exception,
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                ));
                return null;
            }

            try {
                connectionResponse.await(CONNECTION_ACKNOWLEDGEMENT_TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException interruptedException) {
                // Outcome is inspected below
            }

            if (connectionResponse.getCount() != 0) {
                onSubscriptionError.accept(new ApiException(
                    "Subscription timed out waiting for acknowledgement",
                    AmplifyException.TODO_RECOVERY_SUGGESTION
                ));
                return null;
            } else if (connectionFailure != null) {
                onSubscriptionError.accept(new ApiException(
                    connectionFailure, "Check if you are authorized to make this subscription"
                ));
                return null;
            }
        }

        final String subscriptionId = UUID.randomUUID().toString();
        try {
            webSocket.send(new JSONObject()
                .put("id", subscriptionId)
                .put("type", "start")
                .put("payload", new JSONObject()
                .put("data", request.getContent())
                .put("extensions", new JSONObject()
                .put("authorization", SubscriptionAuthorizationHeader.from(apiConfiguration))))
                .toString()
            );
        } catch (JSONException | ApiException exception) {
            onSubscriptionError.accept(new ApiException(
                    "Failed to construct subscription registration message.",
                    exception,
                    AmplifyException.TODO_RECOVERY_SUGGESTION
            ));
        }

        Subscription<T> subscription = new Subscription<>(
            onNextItem, onSubscriptionError, onSubscriptionComplete,
            responseFactory, request.getModelClass()
        );
        subscriptions.put(subscriptionId, subscription);
        if (subscription.awaitSubscriptionReady()) {
            onSubscriptionStarted.accept(subscriptionId);
        }

        return subscriptionId;
    }

    @SuppressLint("SyntheticAccessor")
    private WebSocket createWebSocket() throws ApiException {
        Request request = new Request.Builder()
            .url(buildConnectionRequestUrl())
            .addHeader("Sec-WebSocket-Protocol", "graphql-ws")
            .build();

        return new OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()
            .newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(@NonNull final WebSocket webSocket, @NonNull final Response response) {
                    sendConnectionInit(webSocket);
                }

                @Override
                public void onMessage(@NonNull final WebSocket webSocket, @NonNull final String message) {
                    try {
                        processJsonMessage(webSocket, message);
                    } catch (ApiException exception) {
                        notifyError(exception);
                    }
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

    private void processJsonMessage(WebSocket webSocket, String message) throws ApiException {
        try {
            final JSONObject jsonMessage = new JSONObject(message);
            final SubscriptionMessageType subscriptionMessageType =
                    SubscriptionMessageType.from(jsonMessage.getString("type"));

            switch (subscriptionMessageType) {
                case CONNECTION_ACK:
                    timeoutWatchdog.start(() -> webSocket.close(
                            NORMAL_CLOSURE_STATUS,
                            "WebSocket closed due to timeout."
                        ),
                        Integer.parseInt(
                            jsonMessage.getJSONObject("payload").getString("connectionTimeoutMs")
                        )
                    );
                    connectionResponse.countDown();
                    break;
                case CONNECTION_ERROR:
                    connectionFailure = message;
                    connectionResponse.countDown();
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
                    notifyError(new ApiException(
                            "Got unknown message type: " + subscriptionMessageType,
                            AmplifyException.TODO_RECOVERY_SUGGESTION
                    ));
            }
        } catch (JSONException exception) {
            throw new ApiException(
                    "Error processing Json message in subscription endpoint",
                    exception,
                    AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
    }

    private void notifySubscriptionAcknowledged(final String subscriptionId) throws ApiException {
        Subscription<?> subscription = subscriptions.get(subscriptionId);
        if (subscription != null) {
            subscription.acknowledgeSubscriptionReady();
        } else {
            throw new ApiException(
                "Acknowledgement for unknown subscription: " + subscriptionId,
                AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
    }

    private void notifyAllSubscriptionsCompleted() {
        // TODO: if the connection closes, but our subscription didn't ask for that,
        //  is that a failure, from its standpoint? Or not?
        for (Subscription<?> dispatcher : new HashSet<>(subscriptions.values())) {
            dispatcher.dispatchCompleted();
        }
    }

    private void notifySubscriptionCompleted(String subscriptionId) throws ApiException {
        final Subscription<?> dispatcher = subscriptions.get(subscriptionId);
        if (dispatcher == null) {
            throw new ApiException(
                "Got subscription completion for unknown subscription:" + subscriptionId,
                AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }

        dispatcher.dispatchCompleted();
        dispatcher.acknowledgeSubscriptionCompleted();
    }

    private void notifyError(Throwable error) {
        for (Subscription<?> dispatcher : new HashSet<>(subscriptions.values())) {
            dispatcher.dispatchError(new ApiException(
                    "Subscription failed.",
                    error,
                    AmplifyException.TODO_RECOVERY_SUGGESTION
            ));
        }
    }

    private void notifySubscriptionData(String subscriptionId, String data) throws ApiException {
        final Subscription<?> dispatcher = subscriptions.get(subscriptionId);
        if (dispatcher == null) {
            throw new ApiException(
                "Got subscription data for unknown subscription ID: " + subscriptionId,
                AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
        dispatcher.dispatchNextMessage(data);
    }

    synchronized void releaseSubscription(String subscriptionId) throws ApiException {
        final Subscription<?> subscription = subscriptions.get(subscriptionId);
        if (subscription == null) {
            throw new ApiException(
                "No existing subscription with the given id.",
                AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }

        try {
            webSocket.send(new JSONObject()
                .put("type", "stop")
                .put("id", subscriptionId)
                .toString());
        } catch (JSONException jsonException) {
            throw new ApiException(
                "Failed to construct subscription release message.",
                jsonException,
                AmplifyException.TODO_RECOVERY_SUGGESTION
            );
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
    private String buildConnectionRequestUrl() throws ApiException {
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

        private final Consumer<GraphQLResponse<T>> onNextItem;
        private final Consumer<ApiException> onSubscriptionError;
        private final Action onSubscriptionComplete;
        private final GraphQLResponse.Factory responseFactory;
        private final Class<T> classToCast;
        private final CountDownLatch subscriptionReadyAcknowledgment;
        private final CountDownLatch subscriptionCompletionAcknowledgement;

        Subscription(
                Consumer<GraphQLResponse<T>> onNextItem,
                Consumer<ApiException> onSubscriptionError,
                Action onSubscriptionComplete,
                GraphQLResponse.Factory responseFactory,
                Class<T> classToCast) {
            this.onNextItem = onNextItem;
            this.onSubscriptionError = onSubscriptionError;
            this.onSubscriptionComplete = onSubscriptionComplete;
            this.responseFactory = responseFactory;
            this.classToCast = classToCast;
            this.subscriptionReadyAcknowledgment = new CountDownLatch(1);
            this.subscriptionCompletionAcknowledgement = new CountDownLatch(1);
        }

        void acknowledgeSubscriptionReady() {
            subscriptionReadyAcknowledgment.countDown();
        }

        boolean awaitSubscriptionReady() {
            try {
                if (!subscriptionReadyAcknowledgment.await(ACKNOWLEDGEMENT_TIMEOUT, TimeUnit.SECONDS)) {
                    dispatchError(new ApiException(
                        "Subscription not acknowledged.",
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                    ));
                    return false;
                }
            } catch (InterruptedException interruptedException) {
                dispatchError(new ApiException(
                    "Failure awaiting subscription acknowledgement.",
                    interruptedException,
                    AmplifyException.TODO_RECOVERY_SUGGESTION
                ));
                return false;
            }

            return true;
        }

        void acknowledgeSubscriptionCompleted() {
            subscriptionCompletionAcknowledgement.countDown();
        }

        void awaitSubscriptionCompleted() {
            try {
                if (!subscriptionCompletionAcknowledgement.await(ACKNOWLEDGEMENT_TIMEOUT, TimeUnit.SECONDS)) {
                    dispatchError(new ApiException(
                        "Subscription completion not acknowledged.",
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                    ));
                }
            } catch (InterruptedException interruptedException) {
                dispatchError(new ApiException(
                    "Failure awaiting acknowledgement of subscription completion.",
                    interruptedException,
                    AmplifyException.TODO_RECOVERY_SUGGESTION
                ));
            }
        }

        void dispatchNextMessage(String message) {
            try {
                onNextItem.accept(responseFactory.buildSingleItemResponse(message, classToCast));
            } catch (ApiException exception) {
                dispatchError(exception);
            }
        }

        void dispatchError(ApiException error) {
            onSubscriptionError.accept(error);
        }

        void dispatchCompleted() {
            onSubscriptionComplete.call();
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

            if (!ObjectsCompat.equals(onNextItem, that.onNextItem)) {
                return false;
            }
            if (!ObjectsCompat.equals(onSubscriptionError, that.onSubscriptionError)) {
                return false;
            }
            if (!ObjectsCompat.equals(onSubscriptionComplete, that.onSubscriptionComplete)) {
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
            return ObjectsCompat.equals(
                    subscriptionCompletionAcknowledgement,
                    that.subscriptionCompletionAcknowledgement
            );
        }

        @SuppressWarnings("checkstyle:MagicNumber")
        @Override
        public int hashCode() {
            int result = onNextItem.hashCode();
            result = 31 * result + onSubscriptionError.hashCode();
            result = 31 * result + onSubscriptionComplete.hashCode();
            result = 31 * result + responseFactory.hashCode();
            result = 31 * result + classToCast.hashCode();
            result = 31 * result + subscriptionReadyAcknowledgment.hashCode();
            result = 31 * result + subscriptionCompletionAcknowledgement.hashCode();
            return result;
        }
    }
}
