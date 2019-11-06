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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.ApiPlugin;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.StreamListener;
import com.amplifyframework.core.plugin.PluginException;

import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;

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

        InterceptorFactory interceptorFactory = new AppSyncSigV4SignerInterceptorFactory(context, authProvider);

        for (Map.Entry<String, ApiConfiguration> entry : pluginConfig.getApis().entrySet()) {
            final String apiName = entry.getKey();
            final ApiConfiguration apiConfiguration = entry.getValue();

            OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(interceptorFactory.create(apiConfiguration))
                .build();

            ClientDetails clientDetails =
                new ClientDetails(apiConfiguration.getEndpoint(), httpClient);

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
        throw new UnsupportedOperationException("This has not been implemented, yet.");
    }

    /**
     * Wrapper class to pair http client with dedicated endpoint.
     */
    static class ClientDetails {
        private final String endpoint;
        private final OkHttpClient client;

        /**
         * Constructs a client detail object containing client and url.
         * It associates a http client with its dedicated endpoint.
         */
        ClientDetails(String endpoint, OkHttpClient client) {
            this.endpoint = endpoint;
            this.client = client;
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

