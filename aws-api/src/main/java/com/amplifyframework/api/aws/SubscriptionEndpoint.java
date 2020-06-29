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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.UserAgent;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private static final Logger LOG = Amplify.Logging.forNamespace("aws-api:websocket");
    private static final int CONNECTION_ACKNOWLEDGEMENT_TIMEOUT = 30 /* seconds */;
    private static final int NORMAL_CLOSURE_STATUS = 1000;

    private final ApiConfiguration apiConfiguration;
    private final SubscriptionAuthorizer authorizer;
    private final Map<String, Subscription<?>> subscriptions;
    private final GraphQLResponse.Factory responseFactory;
    private final TimeoutWatchdog timeoutWatchdog;
    private final CountDownLatch connectionResponse;
    private final Set<String> pendingSubscriptions;
    private String subscriptionUrl;
    private String connectionFailure;
    private WebSocket webSocket;

    SubscriptionEndpoint(
            @NonNull ApiConfiguration apiConfiguration,
            @NonNull GraphQLResponse.Factory responseFactory,
            @NonNull SubscriptionAuthorizer authorizer
    ) throws ApiException {
        this.apiConfiguration = Objects.requireNonNull(apiConfiguration);
        this.subscriptions = new ConcurrentHashMap<>();
        this.pendingSubscriptions = Collections.synchronizedSet(new HashSet<>());
        this.responseFactory = Objects.requireNonNull(responseFactory);
        this.authorizer = Objects.requireNonNull(authorizer);
        this.timeoutWatchdog = new TimeoutWatchdog();
        this.connectionResponse = new CountDownLatch(1);
        this.subscriptionUrl = buildConnectionRequestUrl();
    }

    synchronized <T> void requestSubscription(
            @NonNull String subscriptionId,
            @NonNull GraphQLRequest<T> request,
            @NonNull Consumer<String> onSubscriptionStarted,
            @NonNull Consumer<GraphQLResponse<T>> onNextItem,
            @NonNull Consumer<ApiException> onSubscriptionError,
            @NonNull Action onSubscriptionComplete) {
        Objects.requireNonNull(subscriptionId);
        Objects.requireNonNull(request);
        Objects.requireNonNull(onSubscriptionStarted);
        Objects.requireNonNull(onNextItem);
        Objects.requireNonNull(onSubscriptionError);
        Objects.requireNonNull(onSubscriptionComplete);
        LOG.debug("Subscription request method called.");

        // If the subscriptions already exists OR we can't add to the pendingSubscriptions set,
        // then it is a duplicate subscriptionId.
        if (subscriptions.containsKey(subscriptionId) || !pendingSubscriptions.add(subscriptionId)) {
            onSubscriptionError.accept(
                new ApiException(
                        "A subscription with id " + subscriptionId + " already exists.",
                        null,
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                ));
            return;
        }

        // The first call to subscribe will trigger the websocket to be setup.
        if (webSocket == null) {
            connectionFailure = null;
            webSocket = createWebSocket();
            try {
                connectionResponse.await(CONNECTION_ACKNOWLEDGEMENT_TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException interruptedException) {
                // If it was interrupted, we just want to bail since it was probably triggered
                // by the Future being cancelled in SubscriptionOperation
                return;
            }
            if (connectionResponse.getCount() != 0) {
                // Only try to call the handlers if the call is still pending, otherwise
                // you'll probably be invoking a callback that was already diposed.
                if (pendingSubscriptions.remove(subscriptionId)){
                    onSubscriptionError.accept(new ApiException(
                        "Subscription timed out waiting for acknowledgement",
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                    ));
                }
                return;
            } else if (connectionFailure != null) {
                // Only try to call the handlers if the call is still pending, otherwise
                // you'll probably be invoking a callback that was already diposed.
                if (pendingSubscriptions.remove(subscriptionId)) {
                    onSubscriptionError.accept(new ApiException(
                        connectionFailure, "Check if you are authorized to make this subscription"
                    ));
                }
                return;
            }
        }

        try {
            webSocket.send(new JSONObject()
                .put("id", subscriptionId)
                .put("type", "start")
                .put("payload", new JSONObject()
                .put("data", request.getContent())
                .put("extensions", new JSONObject()
                .put("authorization", authorizer.createHeadersForSubscription(request))))
                .toString()
            );
        } catch (JSONException | ApiException exception) {
            // If the subscriptionId was still pending, then we can call the onSubscriptionError
            if (pendingSubscriptions.remove(subscriptionId)){
                onSubscriptionError.accept(new ApiException(
                    "Failed to construct subscription registration message.",
                    exception,
                    AmplifyException.TODO_RECOVERY_SUGGESTION
                ));
            }

            return;
        }

        Subscription<T> subscription = new Subscription<>(
            onNextItem, onSubscriptionError, onSubscriptionComplete,
            responseFactory, request.getResponseType()
        );
        subscriptions.put(subscriptionId, subscription);
        if (subscription.awaitSubscriptionReady()) {
            onSubscriptionStarted.accept(subscriptionId);
        }
    }

    private WebSocket createWebSocket() {
        Request request = new Request.Builder()
            .url(subscriptionUrl)
            .addHeader("Sec-WebSocket-Protocol", "graphql-ws")
            .build();

        return new OkHttpClient.Builder()
            .addNetworkInterceptor(UserAgentInterceptor.using(UserAgent::string))
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
                    "Error processing Json message in subscription endpoint.",
                    exception,
                    AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
    }

    private void notifySubscriptionAcknowledged(final String subscriptionId) throws ApiException {
        Subscription<?> subscription = subscriptions.get(subscriptionId);
        // If the subscription is still present (and it should also be pending if it hasn't been canceled),
        // then invoke the callback
        if (subscription != null && pendingSubscriptions.remove(subscriptionId)) {
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
        // First thing we should do is remove it from the subscriptions collections so
        // the other methods can't grab a hold of the subscription.
        final Subscription<?> subscription = subscriptions.remove(subscriptionId);
        boolean wasSubscriptionPending = pendingSubscriptions.remove(subscriptionId);
        // If the subscription was not in the subscriptions collections AND was also
        // not in the pending collection.
        if (subscription == null && !wasSubscriptionPending) {
            throw new ApiException(
                "No existing subscription with the given id.",
                AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }

        // Only do this if the subscription was NOT pending.
        // Otherwise it would probably fail since it was never established in the first place.
        if (!wasSubscriptionPending) {
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
        }

        // If we have zero subscriptions, close the WebSocket
        if (subscriptions.size() == 0) {
            timeoutWatchdog.stop();
            webSocket.close(NORMAL_CLOSURE_STATUS, "No active subscriptions");
            webSocket = null; //force creation of a new websocket
        }
    }

    /*
     * Discover WebSocket endpoint from the AppSync endpoint.
     * AppSync endpoint : https://xxxxxxxxxxxx.appsync-api.ap-southeast-2.amazonaws.com/graphql
     * Discovered WebSocket endpoint : wss:// xxxxxxxxxxxx.appsync-realtime-api.ap-southeast-2.amazonaws.com/graphql
     */
    private String buildConnectionRequestUrl() throws ApiException {
        // Construct the authorization header for connection request
        final byte[] rawHeader = authorizer.createHeadersForConnection()
            .toString()
            .getBytes();

        URL appSyncEndpoint = null;
        try {
            appSyncEndpoint = new URL(apiConfiguration.getEndpoint());
        } catch (MalformedURLException malformedUrlException) {
            // throwing in a second ...
        }
        if (appSyncEndpoint == null) {
            throw new ApiException(
                    "Malformed API Url: " + apiConfiguration.getEndpoint(),
                    "Verify that GraphQL endpoint is properly formatted."
            );
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
        private final Type responseType;
        private final CountDownLatch subscriptionReadyAcknowledgment;
        private final CountDownLatch subscriptionCompletionAcknowledgement;

        Subscription(
                Consumer<GraphQLResponse<T>> onNextItem,
                Consumer<ApiException> onSubscriptionError,
                Action onSubscriptionComplete,
                GraphQLResponse.Factory responseFactory,
                Type responseType) {
            this.onNextItem = onNextItem;
            this.onSubscriptionError = onSubscriptionError;
            this.onSubscriptionComplete = onSubscriptionComplete;
            this.responseFactory = responseFactory;
            this.responseType = responseType;
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
                onNextItem.accept(responseFactory.buildResponse(null, message, responseType));
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
            if (!ObjectsCompat.equals(responseType, that.responseType)) {
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

        @Override
        public int hashCode() {
            int result = onNextItem.hashCode();
            result = 31 * result + onSubscriptionError.hashCode();
            result = 31 * result + onSubscriptionComplete.hashCode();
            result = 31 * result + responseFactory.hashCode();
            result = 31 * result + responseType.hashCode();
            result = 31 * result + subscriptionReadyAcknowledgment.hashCode();
            result = 31 * result + subscriptionCompletionAcknowledgement.hashCode();
            return result;
        }
    }
}
