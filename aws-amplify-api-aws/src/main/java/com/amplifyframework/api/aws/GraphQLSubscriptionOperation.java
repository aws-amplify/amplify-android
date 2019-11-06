package com.amplifyframework.api.aws;

import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.StreamListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

/**
 * An AWS GraphQL Subscription operation
 * @param <T> type representing graphQLResponse data
 */
public final class GraphQLSubscriptionOperation<T> extends GraphQLOperation<T> {

    private static final String TAG = GraphQLSubscriptionOperation.class.getSimpleName();

    private final String endpoint;
    private final OkHttpClient client;
    private final StreamListener<GraphQLResponse<T>> subscriptionListener;
    private final ApiConfiguration apiConfiguration;

    private WebSocket websocket;
    private final String subscriptionId;

    /**
     * Constructs a new AWSGraphQLOperation.
     * @param endpoint API endpoint being hit
     * @param client OkHttp client being used to hit the endpoint
     * @param request GraphQL request being enacted
     * @param responseFactory an implementation of GsonGraphQLResponseFactory
     * @param classToCast class to cast the response to
     * @param subscriptionListener
     *        listener to be invoked when response is available, or if
     *        errors are encountered while obtaining a response
     */
    GraphQLSubscriptionOperation(String endpoint,
                                 OkHttpClient client,
                                 GraphQLRequest request,
                                 GraphQLResponse.Factory responseFactory,
                                 Class<T> classToCast,
                                 StreamListener<GraphQLResponse<T>> subscriptionListener,
                                 ApiConfiguration apiConfig) {
        super(request, responseFactory, classToCast, null);
        this.endpoint = endpoint;
        this.client = client;
        this.subscriptionListener = subscriptionListener;
        this.apiConfiguration = apiConfig;
        this.subscriptionId = UUID.randomUUID().toString();
    }
    @Override
    public void start() {
        Request request = null;
        try {
            request = new Request.Builder()
                    .url(getConnectionRequestUrl(endpoint,
                            apiConfiguration,
                            this.getRequest().getDocument(),
                            this.getRequest().getVariables()))
                    .addHeader("Sec-WebSocket-Protocol", "graphql-ws")
                    .build();
        } catch (JSONException e) {
            subscriptionListener.onError(new RuntimeException("Failed to get connection url. Please check your configuration.", e));
        }
        AWSWebsocketListener<T> listener = new AWSWebsocketListener<T>(this.getRequest().getDocument(), this.getRequest().getVariables(),
                subscriptionListener, apiConfiguration, this.getClassToCast(), subscriptionId);
        websocket = client.newWebSocket(request, listener);
    }

    private String getConnectionRequestUrl(String endpoint, ApiConfiguration apiConfiguration, String document, Map<String, String> variable) throws JSONException {
        // Construct the authorization header for connection request
        String encodedHeader = Base64.encodeToString(
                SubscriptionAuthorizationUtility.getAuthorizationDetails(true, apiConfiguration, document, variable)
                        .toString()
                        .getBytes(),
                Base64.DEFAULT);

        /**
         * Discover gogi endpoint from the appsync endpoint.
         * AppSync endpoint : https://xxxxxxxxxxxx.appsync-api.ap-southeast-2.amazonaws.com/graphql
         * Discovered gogi endpoint : wss:// xxxxxxxxxxxx.appsync-realtime-api.ap-southeast-2.amazonaws.com/graphql
         *
         */
        URL appSyncEndpoint = null;
        try {
            appSyncEndpoint = new URL(endpoint);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Error getting appsync api url ", e);
        }

        Uri.Builder connectionUriBuilder = new Uri.Builder();
        connectionUriBuilder.scheme("wss")
                .authority(appSyncEndpoint.getHost()
                        .replace("appsync-api", "appsync-realtime-api")).appendPath(appSyncEndpoint.getPath())
                .appendQueryParameter("header", encodedHeader)
                .appendQueryParameter("payload", "e30=");
        return connectionUriBuilder.build().toString();
    }

    @Override
    public void cancel() {
        JSONObject unsubscribeJson = new JSONObject();
        try {
            unsubscribeJson.put("type", "stop");
            unsubscribeJson.put("id", subscriptionId);
        } catch (JSONException e) {
            Log.e(TAG, "Error constructing JSON object", e);
        }
        websocket.send(unsubscribeJson.toString());

    }
}
