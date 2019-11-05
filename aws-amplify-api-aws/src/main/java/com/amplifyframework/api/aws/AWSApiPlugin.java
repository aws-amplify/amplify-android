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

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazonaws.DefaultRequest;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.regions.Regions;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.ApiPlugin;
import com.amplifyframework.api.aws.sigv4.AppSyncV4Signer;
import com.amplifyframework.api.aws.sigv4.BasicCognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.DefaultCognitoUserPoolAuthProvider;
import com.amplifyframework.api.aws.sigv4.OidcAuthProvider;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.StreamListener;
import com.amplifyframework.core.plugin.PluginException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Plugin implementation to be registered with Amplify API category.
 * It uses OkHttp client to execute POST on GraphQL commands.
 */
public final class AWSApiPlugin extends ApiPlugin<Map<String, OkHttpClient>> {

    private static final String TAG = AWSApiPlugin.class.getSimpleName();

    private final Map<String, ClientDetails> httpClients;
    private final GsonGraphQLResponseFactory gqlResponseFactory;
    private final ApiAuthProviders authProvider;

    /**
     * Default constructor for this plugin without any override.
     */
    public AWSApiPlugin() {
        this(ApiAuthProviders.noProviderOverrides());
    }

    /**
     * Constructs an instance of AWSApiPlugin with
     * configured auth providers to override default modes
     * of authorization.
     * If no Auth provider implementation is provided, then
     * the plugin will assume default behavior for that specific
     * mode of authorization.
     * @param apiAuthProvider configured instance of {@link ApiAuthProviders}
     */
    public AWSApiPlugin(ApiAuthProviders apiAuthProvider) {
        this.httpClients = new HashMap<>();
        this.gqlResponseFactory = new GsonGraphQLResponseFactory();
        this.authProvider = apiAuthProvider;
    }

    @Override
    public String getPluginKey() {
        return "AWSAPIPlugin";
    }

    @Override
    public void configure(@NonNull JSONObject pluginConfigurationJson, Context context) throws PluginException {
        AWSApiPluginConfiguration pluginConfig =
            AWSApiPluginConfigurationReader.readFrom(pluginConfigurationJson);

        for (Map.Entry<String, ApiConfiguration> entry : pluginConfig.getApis().entrySet()) {
            final String apiName = entry.getKey();
            final ApiConfiguration apiConfiguration = entry.getValue();

            OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new AppSyncSigV4SignerInterceptorFactory(context, authProvider, apiConfiguration)
                        .create(apiConfiguration))
                .build();

            ClientDetails clientDetails =
                new ClientDetails(apiConfiguration.getEndpoint(), httpClient, apiConfiguration);

            httpClients.put(apiName, clientDetails);
        }
    }

    @Override
    public Map<String, OkHttpClient> getEscapeHatch() {
        final Map<String, OkHttpClient> apiClientsByName = new HashMap<>();
        for (Map.Entry<String, ClientDetails> entry : httpClients.entrySet()) {
            apiClientsByName.put(entry.getKey(), entry.getValue().getClient());
        }
        return Collections.unmodifiableMap(apiClientsByName);
    }

    @Override
    public <T> GraphQLOperation<T> query(
            @NonNull String apiName,
            @NonNull String gqlDocument,
            @Nullable Map<String, String> variables,
            @NonNull Class<T> classToCast,
            @Nullable ResultListener<GraphQLResponse<T>> responseListener) {

        final ClientDetails clientDetails = httpClients.get(apiName);
        if (clientDetails == null) {
            throw new ApiException("No client information for API named " + apiName);
        }

        GraphQLRequest qraphQlRequest = new GraphQLRequest(gqlDocument);
        if (variables != null) {
            for (String key : variables.keySet()) {
                qraphQlRequest.variable(key, variables.get(key));
            }
        }

        GraphQLOperation<T> operation =
            new AWSGraphQLOperation<>(clientDetails.getEndpoint(),
                clientDetails.getClient(),
                qraphQlRequest,
                gqlResponseFactory,
                classToCast,
                responseListener);

        operation.start();
        return operation;
    }

    @Override
    public <T> GraphQLOperation<T> mutate(
            @NonNull String apiName,
            @NonNull String gqlDocument,
            @Nullable Map<String, String> variables,
            @NonNull Class<T> classToCast,
            @Nullable ResultListener<GraphQLResponse<T>> responseListener) {

        final ClientDetails clientDetails = httpClients.get(apiName);
        if (clientDetails == null) {
            throw new ApiException("No client information for API named " + apiName);
        }

        GraphQLRequest qraphQlRequest = new GraphQLRequest(gqlDocument);
        if (variables != null) {
            for (String key : variables.keySet()) {
                qraphQlRequest.variable(key, variables.get(key));
            }
        }

        GraphQLOperation<T> operation =
                new AWSGraphQLOperation<>(clientDetails.getEndpoint(),
                        clientDetails.getClient(),
                        qraphQlRequest,
                        gqlResponseFactory,
                        classToCast,
                        responseListener);

        operation.start();
        return operation;
    }

    @Override
    public <T> GraphQLOperation<T> subscribe(
            @NonNull String apiName,
            @NonNull String gqlDocument,
            @Nullable Map<String, String> variables,
            @NonNull Class<T> classToCast,
            @Nullable StreamListener<GraphQLResponse<T>> subscriptionListener) {
        // throw new UnsupportedOperationException("This has not been implemented, yet.");
        // Get the required OkHttp client
        final ClientDetails clientDetails = httpClients.get(apiName);
        if (clientDetails == null) {
            throw new ApiException("No client information for API named " + apiName);
        }

        GraphQLRequest qraphQlRequest = new GraphQLRequest(gqlDocument);
        if (variables != null) {
            for (String key : variables.keySet()) {
                qraphQlRequest.variable(key, variables.get(key));
            }
        }
        // make the call
        OkHttpClient client = clientDetails.getClient();
        Request request = null;
        try {
            request = new Request.Builder()
                    .url(getConnectionRequestUrl(clientDetails.getEndpoint(), clientDetails.getApiConfiguration(), gqlDocument, variables))
                    .addHeader("Sec-WebSocket-Protocol", "graphql-ws")
                    .build();
        } catch (JSONException e) {
            subscriptionListener.onError(new RuntimeException("Failed to get connection url. Please check awsconfiguration.json", e));
        }
        AppSyncWebSocketListener<T> listener = new AppSyncWebSocketListener(gqlDocument, variables,
                subscriptionListener, clientDetails.getApiConfiguration());
        WebSocket websocket = client.newWebSocket(request, listener);

        // setup the listener
        return null;
    }

    /**
     * Listener for events on the websocket connection established for subscription
     */
    private final class AppSyncWebSocketListener<T> extends WebSocketListener {

        private static final int NORMAL_CLOSURE_STATUS = 1000;
        private StreamListener<GraphQLResponse<T>> callback;
        private String gqlDocument;
        private Map<String, String> variables;
        private ApiConfiguration apiConfiguration;


        public AppSyncWebSocketListener(String gqlDocument, Map<String, String> variables, StreamListener<GraphQLResponse<T>> subscriptionListener, ApiConfiguration apiConfiguration) {
            this.callback = subscriptionListener;
            this.gqlDocument = gqlDocument;
            this.variables = variables;
            this.apiConfiguration = apiConfiguration;
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
                    subscriptionRegistrationMessage.put("id", "1");
                    subscriptionRegistrationMessage.put("type", "start");
                    subscriptionRegistrationMessage.put("payload", getSubscriptionRegistrationPayload());
                } catch (JSONException e) {
                    Log.e(TAG, "Error constructing JSON object", e);
                }

                String reg = subscriptionRegistrationMessage.toString();
                webSocket.send(reg);
                Log.e("Connected", text);
            } else if(text.contains("start_ack")) {
                // callback.onResponse(new Response<T>(new Response.Builder<T>(subscription).data((T)"Subscription successful")));
                Log.e(TAG, "Subscribed successfully");
            } else if (text.contains("\"complete\"")) {
                callback = null;
            } else if (text.contains("ka")){
                Log.e(TAG, "Keep Alive message received");
            }
            else {
                Log.e(TAG, "Message received successfully " + text);
                // Extract the payload which can be either data or error.
                String data = null;
                try {
                    JSONObject response = new JSONObject(text);
                    data = response.getString("payload");
                } catch (JSONException e) {
                    Log.e(TAG, "Error constructing JSON Object from the ", e);
                }

                // Parse the response using OperationResponseParser
                ResponseBody messageBody = ResponseBody.create(MediaType.parse("text/plain"), data);
            }

        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
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
                extension.put("authorization", getAuthorizationDetails(false, apiConfiguration, gqlDocument, variables));
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

    private String getConnectionRequestUrl(String endpoint, ApiConfiguration apiConfiguration, String document, Map<String, String> variable) throws JSONException {
        // Construct the authorization header for connection request
        String encodedHeader = Base64.encodeToString(
                getAuthorizationDetails(true, apiConfiguration, document, variable)
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

    /**
     * Return authorization json to be used for connection and subscription registration.
     *
     * @return
     */
    private JSONObject getAuthorizationDetails(boolean connectionFlag, ApiConfiguration apiConfiguration, String gqlDocument, Map<String, String> variable) throws JSONException {
        JSONObject authorizationMessage = new JSONObject();
        // Get the Auth Mode from configuration json
        String authMode = null;
            authMode = apiConfiguration.getAuthorizationType().toString();

        // Construct the Json based on the Auth Mode
        switch (authMode) {
            case "API_KEY" :
                authorizationMessage = getAuthorizationDetailsForApiKey(apiConfiguration);
                break;
            case "AWS_IAM" :
                authorizationMessage = getAuthorizationDetailsForIAM(connectionFlag, apiConfiguration, gqlDocument, variable);
                break;
            case "AMAZON_COGNITO_USER_POOLS" :
                authorizationMessage = getAuthorizationDetailsForUserpools(apiConfiguration);
                break;
            case "OPENID_CONNECT" :
                authorizationMessage = getAuthorizationDetailsForOidc(apiConfiguration);
                break;
            default :
                Log.e(TAG, "Unsupported Auth type detected");
                break;
        }

        return authorizationMessage;
    }

    private JSONObject getAuthorizationDetailsForApiKey(ApiConfiguration apiConfiguration) {
        JSONObject authorizationMessage = new JSONObject();
        try {
            authorizationMessage.put("host",
                    getHost(apiConfiguration.getEndpoint()));
            authorizationMessage.put("x-amz-date",
                    ISO8601Timestamp.now());
            authorizationMessage.put("x-api-key",
                    apiConfiguration.getApiKey());
        } catch (JSONException | MalformedURLException e) {
            Log.e(TAG, "Error constructing the authorization json", e);
        }
        return authorizationMessage;
    }

    private JSONObject getAuthorizationDetailsForIAM(boolean connectionFlag,
                                                     ApiConfiguration apiConfiguration,
                                                     String queryDocument,
                                                     Map<String, String> variable) throws JSONException {
        JSONObject authorizationMessage = new JSONObject();
        JSONObject identityPoolJSON;

        String identityPoolId = null;
        String regionStr = null;

        DefaultRequest canonicalRequest = new DefaultRequest("appsync");
        try {
            if (connectionFlag) {
                canonicalRequest.setEndpoint(new URI(apiConfiguration.getEndpoint() + "/connect"));
            } else {
                canonicalRequest.setEndpoint(new URI(apiConfiguration.getEndpoint()));
            }
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error constructing canonical URI for IAM request signature", e);
        }
        canonicalRequest.addHeader("accept", "application/json, text/javascript");
        canonicalRequest.addHeader("content-encoding", "amz-1.0");
        canonicalRequest.addHeader("content-type", "application/json; charset=UTF-8");

        canonicalRequest.setHttpMethod(HttpMethodName.valueOf("POST"));
        if (connectionFlag) {
            canonicalRequest.setContent(new ByteArrayInputStream("{}".getBytes()));
        } else {
            canonicalRequest.setContent(new ByteArrayInputStream(getDataJson(queryDocument, variable).getBytes()));
        }
        if (connectionFlag){
            new AppSyncV4Signer(regionStr, AppSyncV4Signer.ResourcePath.IAM_CONNECTION_RESOURCE_PATH)
                    .sign(canonicalRequest, AWSMobileClient.getInstance().getCredentials());
        } else {
            new AppSyncV4Signer(regionStr).sign(canonicalRequest,
                    AWSMobileClient.getInstance().getCredentials());
        }

        Map<String, String> signedHeaders = canonicalRequest.getHeaders();
        try {
            for(Map.Entry headerEntry : signedHeaders.entrySet()) {
                if (! headerEntry.getKey().equals("host")) {
                    authorizationMessage.put((String) headerEntry.getKey(), headerEntry.getValue());
                } else {
                    authorizationMessage.put("host", getHost(apiConfiguration.getEndpoint()));
                }
            }
        } catch (JSONException | MalformedURLException e) {
            Log.e(TAG, "Error constructing authorization message json", e);
        }
        return authorizationMessage;
    }

    private JSONObject getAuthorizationDetailsForUserpools(ApiConfiguration apiConfiguration) {
        JSONObject authorizationMessage = new JSONObject();

        CognitoUserPoolsAuthProvider authProvider = new DefaultCognitoUserPoolAuthProvider();
        String token = authProvider.getLatestAuthToken();
        try {
            authorizationMessage.put("host", getHost(apiConfiguration.getEndpoint()));
            authorizationMessage.put("Authorization", token);
        } catch (JSONException | MalformedURLException e) {
            Log.e(TAG, "Error constructing authorization message json", e);
        }

        return authorizationMessage;
    }

    private JSONObject getAuthorizationDetailsForOidc(ApiConfiguration apiConfiguration) {
        JSONObject authorizationMessage = new JSONObject();
        try {
            OidcAuthProvider oidcAuthProvider = ApiAuthProviders.noProviderOverrides().getOidcAuthProvider();
            String token = oidcAuthProvider.getLatestAuthToken();
            authorizationMessage.put("host", getHost(apiConfiguration.getEndpoint()));
            authorizationMessage.put("Authorization", token);
        } catch (JSONException | MalformedURLException e) {
            Log.e(TAG, "Error constructing authorization message json", e);
        }
        return authorizationMessage;
    }

    private String getHost(String apiUrl) throws MalformedURLException {
        return new URL(apiUrl).getHost();
    }

    private String getDataJson(String queryDcoument, Map<String, String> variables) {
        String query = queryDcoument;
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

    /**
     * Utility to create a ISO 8601 compliant timestamps
     */
    public static final class ISO8601Timestamp {
        public static String now() {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            return formatter.format(new Date());
        }
    }

    /**
     * Wrapper class to pair http client with dedicated endpoint.
     */
    class ClientDetails {
        private final String endpoint;
        private final OkHttpClient client;
        private final ApiConfiguration apiConfiguration;

        /**
         * Constructs a client detail object containing client and url.
         * It associates a http client with its dedicated endpoint.
         */
        ClientDetails(String endpoint, OkHttpClient client, ApiConfiguration apiConfiguration) {
            this.endpoint = endpoint;
            this.client = client;
            this.apiConfiguration = apiConfiguration;
        }



        public ApiConfiguration getApiConfiguration() {
            return apiConfiguration;
        }

        /**
         * Gets the endpoint.
         * @return endpoint
         */
        public String getEndpoint() {
            return endpoint;
        }

        /**
         * Gets the HTTP client.
         * @return OkHttp client
         */
        public OkHttpClient getClient() {
            return client;
        }
    }
}

