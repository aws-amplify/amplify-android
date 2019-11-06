package com.amplifyframework.api.aws;

import android.util.Log;

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.StreamListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Listener for events on the websocket connection established for subscription
 */
final class AWSWebsocketListener<T> extends WebSocketListener {

    private static final String TAG = AWSWebsocketListener.class.getSimpleName();

    private static final int NORMAL_CLOSURE_STATUS = 1000;
    private StreamListener<GraphQLResponse<T>> callback;
    private String gqlDocument;
    private Map<String, String> variables;
    private ApiConfiguration apiConfiguration;
    private Class<T> classToCast;
    private final String subscriptionId;


    public AWSWebsocketListener(String gqlDocument, Map<String, String> variables,
                                StreamListener<GraphQLResponse<T>> subscriptionListener,
                                ApiConfiguration apiConfiguration,
                                Class<T> classToCast,
                                String subscriptionId) {
        this.callback = subscriptionListener;
        this.gqlDocument = gqlDocument;
        this.variables = variables;
        this.apiConfiguration = apiConfiguration;
        this.classToCast = classToCast;
        this.subscriptionId = subscriptionId;
    }

    @Override
    public void onOpen(WebSocket webSocket, okhttp3.Response response) {
        // Connection was successfully opened, send a connection_init message
        JSONObject connectionInitMessage = new JSONObject();
        try {
            connectionInitMessage.put("type", "connection_init");
        } catch (JSONException e) {
            Log.e(TAG, "Error constructing JSON object", e);
        }
        webSocket.send(connectionInitMessage.toString());
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {

        // Message received on the websocket connection, process based on the message type
        if (text.contains("connection_ack")) {
            // connection_ack message received, register a subscription

            JSONObject subscriptionRegistrationMessage = new JSONObject();
            try {
                subscriptionRegistrationMessage.put("id", subscriptionId);
                subscriptionRegistrationMessage.put("type", "start");
                subscriptionRegistrationMessage.put("payload", getSubscriptionRegistrationPayload());
            } catch (JSONException e) {
                Log.e(TAG, "Error constructing JSON object", e);
            }

            String reg = subscriptionRegistrationMessage.toString();
            webSocket.send(reg);
            Log.e("Connected", text);
        } else if(text.contains("start_ack")) {
            Log.e(TAG, "Subscribed successfully");
        } else if (text.contains("\"complete\"")) {
            callback = null;
        } else if (text.contains("ka")){
            Log.e(TAG, "Keep Alive message received");
        }
        else {
            Log.e(TAG, "Message received successfully " + text);

            // Parse the response
            callback.onNext((new GsonGraphQLResponseFactory().buildResponse(text, classToCast)));
        }

    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null);
        Log.w(TAG, "Webscoket connection was closed. " + reason);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
        Log.e(TAG, "Subscription failure encountered", t);
        callback.onError(new RuntimeException("Error in Subscription", t));
        // Some retry logic here
    }

    private JSONObject getSubscriptionRegistrationPayload() {
        JSONObject payload = new JSONObject();

        try {
            payload.put("data", getDataJson());
            payload.put("extensions", getExtension());
        } catch (JSONException e) {
            Log.e(TAG, "Error constructing JSON object", e);
        }
        return payload;
    }

    private JSONObject getExtension() {
        JSONObject extension = new JSONObject();
        try {
            extension.put("authorization", SubscriptionAuthorizationUtility.getAuthorizationDetails(false, apiConfiguration, gqlDocument, variables));
        } catch (JSONException e) {
            Log.e(TAG, "Error constructing JSON object", e);
        }

        return extension;
    }

    private String getDataJson() {
        String query = gqlDocument;
        JSONObject dataJson = new JSONObject();
        try {
            dataJson.put("query", query);
            JSONObject variableJson = new JSONObject();
            for (Map.Entry<String, String> e : variables.entrySet()){
                variableJson.put(e.getKey(), e.getValue());
            }
            dataJson.put("variables", variableJson);
        } catch (JSONException e) {
            Log.e(TAG, "Error constructing JSON object", e);
        }

        return dataJson.toString();
    }
}