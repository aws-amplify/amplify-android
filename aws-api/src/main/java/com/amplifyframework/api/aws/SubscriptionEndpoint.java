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
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.ApiException.ApiAuthException;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.category.CategoryType;
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
import java.util.concurrent.atomic.AtomicReference;

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
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.API, "amplify:aws-api");
    private static final int CONNECTION_ACKNOWLEDGEMENT_TIMEOUT = 30 /* seconds */;
    private static final int NORMAL_CLOSURE_STATUS = 1000;
    private static final String UNAUTHORIZED_EXCEPTION = "UnauthorizedException";

    private final ApiConfiguration apiConfiguration;
    private final SubscriptionAuthorizer authorizer;
    private final Map<String, Subscription<?>> subscriptions;
    private final GraphQLResponse.Factory responseFactory;
    private final TimeoutWatchdog timeoutWatchdog;
    private final Set<String> pendingSubscriptionIds;
    private final OkHttpClient okHttpClient;
    private final Object webSocketLock = new Object();
    private WebSocket webSocket;
    private AmplifyWebSocketListener webSocketListener;
    private String apiName;

    SubscriptionEndpoint(
            @NonNull ApiConfiguration apiConfiguration,
            @Nullable OkHttpConfigurator configurator,
            @NonNull GraphQLResponse.Factory responseFactory,
            @NonNull SubscriptionAuthorizer authorizer,
            @Nullable String apiName
    ) {
        this.apiConfiguration = Objects.requireNonNull(apiConfiguration);
        this.subscriptions = new ConcurrentHashMap<>();
        this.responseFactory = Objects.requireNonNull(responseFactory);
        this.authorizer = Objects.requireNonNull(authorizer);
        this.timeoutWatchdog = new TimeoutWatchdog();
        this.pendingSubscriptionIds = Collections.synchronizedSet(new HashSet<>());
        this.apiName = apiName;

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true);

        if (configurator != null) {
            configurator.applyConfiguration(okHttpClientBuilder);
        }

        this.okHttpClient = okHttpClientBuilder.build();
    }

    synchronized <T> void requestSubscription(
        @NonNull GraphQLRequest<T> request,
        @NonNull Consumer<String> onSubscriptionStarted,
        @NonNull Consumer<GraphQLResponse<T>> onNextItem,
        @NonNull Consumer<ApiException> onSubscriptionError,
        @NonNull Action onSubscriptionComplete) {
        requestSubscription(request,
                            apiConfiguration.getAuthorizationType(),
                            onSubscriptionStarted,
                            onNextItem,
                            onSubscriptionError,
                            onSubscriptionComplete);
    }

    synchronized <T> void requestSubscription(
            @NonNull GraphQLRequest<T> request,
            @NonNull AuthorizationType authType,
            @NonNull Consumer<String> onSubscriptionStarted,
            @NonNull Consumer<GraphQLResponse<T>> onNextItem,
            @NonNull Consumer<ApiException> onSubscriptionError,
            @NonNull Action onSubscriptionComplete) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(onSubscriptionStarted);
        Objects.requireNonNull(onNextItem);
        Objects.requireNonNull(onSubscriptionError);
        Objects.requireNonNull(onSubscriptionComplete);

        final String subscriptionId = UUID.randomUUID().toString();
        final AmplifyWebSocketListener socketListener;
        final WebSocket socket;

        synchronized (webSocketLock) {
            // The first call to subscribe OR a disconnected websocket listener will
            // force a new connection to be created.
            if (webSocketListener == null || webSocketListener.isDisconnectedState()) {
                webSocketListener = new AmplifyWebSocketListener();
                try {
                    webSocket = okHttpClient.newWebSocket(new Request.Builder()
                                                              .url(buildConnectionRequestUrl(authType))
                                                              .addHeader("Sec-WebSocket-Protocol", "graphql-ws")
                                                              .header("User-Agent", UserAgent.string())
                                                              .build(), webSocketListener);
                } catch (ApiException apiException) {
                    onSubscriptionError.accept(apiException);
                    return;
                }

            }

            pendingSubscriptionIds.add(subscriptionId);
            socketListener = webSocketListener;
            socket = webSocket;
        }

        // Every request waits here for the connection to be ready.
        Connection connection = socketListener.waitForConnectionReady();
        if (connection.hasFailure()) {
            // If the latch didn't count all the way down
            if (pendingSubscriptionIds.remove(subscriptionId)) {
                // The subscription was pending, so we need to emit an error.
                onSubscriptionError.accept(
                    new ApiException(connection.getFailureReason(), AmplifyException.TODO_RECOVERY_SUGGESTION));
                return;
            }
        }

        try {
            String jsonMessage = new JSONObject()
                .put("id", subscriptionId)
                .put("type", "start")
                .put("payload", new JSONObject()
                .put("data", request.getContent())
                .put("extensions", new JSONObject()
                .put("authorization", authorizer.createHeadersForSubscription(request, authType))))
                .toString();

            socket.send(jsonMessage);
        } catch (JSONException | ApiException exception) {
            // If the subscriptionId was still pending, then we can call the onSubscriptionError
            if (pendingSubscriptionIds.remove(subscriptionId)) {
                if (exception instanceof ApiAuthException) {
                    // Don't wrap it if it's an ApiAuthException.
                    onSubscriptionError.accept((ApiAuthException) exception);
                } else {
                    onSubscriptionError.accept(new ApiException(
                        "Failed to construct subscription registration message.",
                        exception,
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                    ));
                }

            }
            return;
        }

        Subscription<T> subscription = new Subscription<>(
            onNextItem, onSubscriptionError, onSubscriptionComplete,
            responseFactory, request.getResponseType(), request, apiName
        );
        subscriptions.put(subscriptionId, subscription);
        if (subscription.awaitSubscriptionReady()) {
            pendingSubscriptionIds.remove(subscriptionId);
            onSubscriptionStarted.accept(subscriptionId);
        }
    }

    private void notifySubscriptionAcknowledged(final String subscriptionId) throws ApiException {
        Subscription<?> subscription = subscriptions.get(subscriptionId);
        // If the subscription is still present (and it should also be pending if it hasn't been canceled),
        // then invoke the callback
        if (subscription != null && pendingSubscriptionIds.remove(subscriptionId)) {
            subscription.acknowledgeSubscriptionReady();
        } else {
            throw new ApiException(
                "Acknowledgement for unknown subscription: " + subscriptionId,
                AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
    }

    private void notifySubscriptionFailure(final String subscriptionId) {
        Subscription<?> subscription = subscriptions.get(subscriptionId);
        if (subscription != null && pendingSubscriptionIds.remove(subscriptionId)) {
            subscription.acknowledgeSubscriptionFailure();
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
                "Subscription failed.", error,
                "Check your Internet connection. Is your device online?"
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
        // First thing we should do is remove it from the pending subscription collection so
        // the other methods can't grab a hold of the subscription.
        final Subscription<?> subscription = subscriptions.get(subscriptionId);
        boolean wasSubscriptionPending = pendingSubscriptionIds.remove(subscriptionId);
        // If the subscription was not in the either of the subscriptions collections.
        if (subscription == null && !wasSubscriptionPending) {
            throw new ApiException(
                "No existing subscription with the given id.",
                AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }

        // Only do this if the subscription was NOT pending.
        // Otherwise it would probably fail since it was never established in the first place.
        final AmplifyWebSocketListener socketListener = webSocketListener;
        if (!wasSubscriptionPending && socketListener != null && !socketListener.isDisconnectedState()) {
            try {
                String jsonMessage = new JSONObject()
                    .put("type", "stop")
                    .put("id", subscriptionId)
                    .toString();

                webSocket.send(jsonMessage);
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
        synchronized (webSocketLock) {
            subscriptions.remove(subscriptionId);
            if (subscriptions.isEmpty() && pendingSubscriptionIds.isEmpty()) {
                LOG.info("No more active subscriptions. Closing web socket.");
                timeoutWatchdog.stop();
                webSocket.close(NORMAL_CLOSURE_STATUS, "No active subscriptions");
                webSocketListener = null;
            }
        }
    }

    /*
     * Discover WebSocket endpoint from the AppSync endpoint.
     * AppSync endpoint : https://xxxxxxxxxxxx.appsync-api.ap-southeast-2.amazonaws.com/graphql
     * Discovered WebSocket endpoint : wss:// xxxxxxxxxxxx.appsync-realtime-api.ap-southeast-2.amazonaws.com/graphql
     */
    private String buildConnectionRequestUrl(AuthorizationType authType) throws ApiException {
        // Construct the authorization header for connection request
        final byte[] rawHeader = authorizer.createHeadersForConnection(authType)
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

        DomainType domainType = DomainType.from(apiConfiguration.getEndpoint());

        String authority = appSyncEndpoint.getHost();
        if (domainType == DomainType.STANDARD) {
            authority = authority.replace("appsync-api", "appsync-realtime-api");
        }

        String path = appSyncEndpoint.getPath();
        if (domainType == DomainType.CUSTOM) {
            path = path + "/realtime";
        }

        return new Uri.Builder()
            .scheme("wss")
            .authority(authority)
            .path(path)
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
        private final GraphQLRequest<T> request;
        private final CountDownLatch subscriptionReadyAcknowledgment;
        private final CountDownLatch subscriptionCompletionAcknowledgement;
        private boolean failed;
        private String apiName;

        Subscription(
                Consumer<GraphQLResponse<T>> onNextItem,
                Consumer<ApiException> onSubscriptionError,
                Action onSubscriptionComplete,
                GraphQLResponse.Factory responseFactory,
                Type responseType,
                GraphQLRequest<T> request,
                String apiName
        ) {
            this.onNextItem = onNextItem;
            this.onSubscriptionError = onSubscriptionError;
            this.onSubscriptionComplete = onSubscriptionComplete;
            this.responseFactory = responseFactory;
            this.responseType = responseType;
            this.request = request;
            this.apiName = apiName;
            this.subscriptionReadyAcknowledgment = new CountDownLatch(1);
            this.subscriptionCompletionAcknowledgement = new CountDownLatch(1);
            this.failed = false;
        }

        void acknowledgeSubscriptionReady() {
            subscriptionReadyAcknowledgment.countDown();
        }

        void acknowledgeSubscriptionFailure() {
            failed = true;
            subscriptionReadyAcknowledgment.countDown();
        }

        boolean awaitSubscriptionReady() {
            try {
                if (!subscriptionReadyAcknowledgment.await(ACKNOWLEDGEMENT_TIMEOUT, TimeUnit.SECONDS)) {
                    dispatchError(new ApiException(
                        "Timed out waiting for subscription start_ack.",
                        "Check your Internet connection. Is your device online?"
                    ));
                    return false;
                } else if (failed) {
                    // An error was already dispatched at the time of failure, so don't dispatch a second one.
                    return false;
                }
            } catch (InterruptedException interruptedException) {
                // Triggered when the Future created in SubscriptionOperation is cancelled, which happens when the
                // subscription Observable is disposed, which happens when a SUBSCRIPTION_ERROR occurs.  Don't dispatch
                // any error because the caller has likely already been disposed.
                LOG.warn("Thread interrupted awaiting subscription acknowledgement.", interruptedException);
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
                    "Thread interrupted awaiting subscription completion.",
                    interruptedException,
                    AmplifyException.TODO_RECOVERY_SUGGESTION
                ));
            }
        }

        // This method should be used in place of GraphQLResponse.Factory buildResponse.
        // We need to use this method to pass apiName for LazyModel
        private GraphQLResponse<T> buildResponse(String jsonResponse) throws ApiException {
            if (!(responseFactory instanceof GsonGraphQLResponseFactory)) {
                throw new ApiException(
                        "Amplify encountered an error while deserializing an object. " +
                        "GraphQLResponse.Factory was not of type GsonGraphQLResponseFactory",
                        AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION);
            }

            try {
                return ((GsonGraphQLResponseFactory) responseFactory)
                        .buildResponse(request, jsonResponse, apiName);
            } catch (ClassCastException cce) {
                throw new ApiException("Amplify encountered an error while deserializing an object",
                        AmplifyException.TODO_RECOVERY_SUGGESTION);
            }
        }

        void dispatchNextMessage(String message) {
            try {
                onNextItem.accept(buildResponse(message));
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

    final class AmplifyWebSocketListener extends WebSocketListener {
        private final CountDownLatch connectionResponse;
        private final AtomicReference<EndpointStatus> endpointStatus;

        AmplifyWebSocketListener() {
            this(new CountDownLatch(1));
        }

        AmplifyWebSocketListener(CountDownLatch latch) {
            this.connectionResponse = latch;
            this.endpointStatus = new AtomicReference<>(EndpointStatus.DISCONNECTED);
        }

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
            notifyAllSubscriptionsCompleted();
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable failure, Response response) {
            LOG.warn("Websocket connection failed.", failure);
            endpointStatus.set(EndpointStatus.CONNECTION_FAILED);
            webSocket.cancel();
            // This will free up any pending subscriptions that haven't been established yet.
            connectionResponse.countDown();
            // This will broadcast the error to all subscriptions
            notifyError(failure);
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            super.onClosed(webSocket, code, reason);
            endpointStatus.set(EndpointStatus.DISCONNECTED);
        }

        public boolean isDisconnectedState() {
            return endpointStatus.get().isDisconnectedState();
        }

        public Connection waitForConnectionReady() {
            try {
                if (!connectionResponse.await(CONNECTION_ACKNOWLEDGEMENT_TIMEOUT, TimeUnit.SECONDS)) {
                    LOG.warn("Timed out waiting for connection acknowledgement.");
                    return new Connection("Timed out waiting for connection acknowledgement.");
                }
            } catch (InterruptedException exception) {
                LOG.warn("Thread interrupted waiting for connection acknowledgement");
                return new Connection("Thread interrupted waiting for connection acknowledgement");
            }
            LOG.debug("Current endpoint status: " + endpointStatus.get());
            if (EndpointStatus.CONNECTION_FAILED.equals(endpointStatus.get())) {
                return new Connection("Connection failed.");
            }
            return new Connection();
        }

        private void sendConnectionInit(WebSocket webSocket) {
            try {
                String jsonMessage = new JSONObject()
                    .put("type", "connection_init")
                    .toString();

                webSocket.send(jsonMessage);
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
                        timeoutWatchdog.start(() -> {
                            LOG.warn("WebSocket closed due to timeout.");

                            webSocket.close(
                                    NORMAL_CLOSURE_STATUS,
                                    "WebSocket closed due to timeout."
                            );
                        },
                            Integer.parseInt(
                                jsonMessage.getJSONObject("payload").getString("connectionTimeoutMs")
                            )
                        );
                        endpointStatus.set(EndpointStatus.CONNECTED);
                        connectionResponse.countDown();
                        break;
                    case CONNECTION_ERROR:
                        endpointStatus.set(EndpointStatus.CONNECTION_FAILED);
                        LOG.warn("Websocket listener received a CONNECTION_ERROR event. " + message);
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
                        notifySubscriptionFailure(jsonMessage.getString("id"));
                        notifySubscriptionData(jsonMessage.getString("id"), jsonMessage.getString("payload"));
                        break;
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
    }

    static final class Connection {
        private final String failureReason;

        Connection() {
            this.failureReason = null;
        }

        Connection(String failureReason) {
            this.failureReason = failureReason;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public boolean hasFailure() {
            return failureReason != null;
        }
    }

    enum EndpointStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        CONNECTION_FAILED;

        boolean isDisconnectedState() {
            return this.equals(DISCONNECTED) || this.equals(CONNECTION_FAILED);
        }
    }
}
