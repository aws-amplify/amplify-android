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
import androidx.core.util.ObjectsCompat;

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

    private final Map<String, ClientDetails> apiDetails;
    private final GraphQLResponse.Factory gqlResponseFactory;
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
        this.apiDetails = new HashMap<>();
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

        final InterceptorFactory interceptorFactory =
            new AppSyncSigV4SignerInterceptorFactory(context, authProvider);

        for (Map.Entry<String, ApiConfiguration> entry : pluginConfig.getApis().entrySet()) {
            final String apiName = entry.getKey();
            final ApiConfiguration apiConfiguration = entry.getValue();
            final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(interceptorFactory.create(apiConfiguration))
                .build();
            final SubscriptionEndpoint subscriptionEndpoint =
                new SubscriptionEndpoint(apiConfiguration, gqlResponseFactory);
            apiDetails.put(apiName, new ClientDetails(apiConfiguration, okHttpClient, subscriptionEndpoint));
        }
    }

    @Override
    public Map<String, OkHttpClient> getEscapeHatch() {
        final Map<String, OkHttpClient> apiClientsByName = new HashMap<>();
        for (Map.Entry<String, ClientDetails> entry : apiDetails.entrySet()) {
            apiClientsByName.put(entry.getKey(), entry.getValue().okHttpClient());
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
        final GraphQLOperation<T> operation =
            buildSingleResponseOperation(apiName, gqlDocument, variables, classToCast, responseListener);
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
        final GraphQLOperation<T> operation =
            buildSingleResponseOperation(apiName, gqlDocument, variables, classToCast, responseListener);
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

        final ClientDetails clientDetails = apiDetails.get(apiName);
        if (clientDetails == null) {
            throw new ApiException("No client information for API named " + apiName);
        }

        SubscriptionOperation<T> operation = SubscriptionOperation.<T>builder()
            .subscriptionManager(clientDetails.webSocketEndpoint())
            .endpoint(clientDetails.apiConfiguration().getEndpoint())
            .client(clientDetails.okHttpClient())
            .graphQLRequest(GraphQLRequest.builder()
                .addVariables(variables)
                .document(gqlDocument)
                .build())
            .responseFactory(gqlResponseFactory)
            .classToCast(classToCast)
            .streamListener(subscriptionListener)
            .build();
        operation.start();
        return operation;
    }

    private <T> SingleResultOperation<T> buildSingleResponseOperation(
            @NonNull String apiName,
            @NonNull String gqlDocument,
            @Nullable Map<String, String> variables,
            @NonNull Class<T> classToCast,
            @Nullable ResultListener<GraphQLResponse<T>> responseListener) {

        final ClientDetails clientDetails = apiDetails.get(apiName);
        if (clientDetails == null) {
            throw new ApiException("No client information for API named " + apiName);
        }

        return SingleResultOperation.<T>builder()
            .endpoint(clientDetails.apiConfiguration().getEndpoint())
            .client(clientDetails.okHttpClient())
            .request(GraphQLRequest.builder()
                .document(gqlDocument)
                .addVariables(variables)
                .build())
            .responseFactory(gqlResponseFactory)
            .classToCast(classToCast)
            .responseListener(responseListener)
            .build();
    }

    /**
     * Wrapper class to pair http client with dedicated endpoint.
     */
    static final class ClientDetails {
        private final ApiConfiguration apiConfiguration;
        private final OkHttpClient okHttpClient;
        private final SubscriptionEndpoint subscriptionEndpoint;

        /**
         * Constructs a client detail object containing client and url.
         * It associates a http client with its dedicated endpoint.
         */
        ClientDetails(
                final ApiConfiguration apiConfiguration,
                final OkHttpClient okHttpClient,
                final SubscriptionEndpoint subscriptionEndpoint) {
            this.apiConfiguration = apiConfiguration;
            this.okHttpClient = okHttpClient;
            this.subscriptionEndpoint = subscriptionEndpoint;
        }

        /**
         * Gets the API configuration.
         * @return API configuration
         */
        ApiConfiguration apiConfiguration() {
            return apiConfiguration;
        }

        /**
         * Gets the HTTP client.
         * @return OkHttp client
         */
        OkHttpClient okHttpClient() {
            return okHttpClient;
        }

        SubscriptionEndpoint webSocketEndpoint() {
            return subscriptionEndpoint;
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            ClientDetails that = (ClientDetails) thatObject;

            if (!ObjectsCompat.equals(apiConfiguration, that.apiConfiguration)) {
                return false;
            }
            if (!ObjectsCompat.equals(okHttpClient, that.okHttpClient)) {
                return false;
            }
            return ObjectsCompat.equals(subscriptionEndpoint, that.subscriptionEndpoint);
        }

        @SuppressWarnings("checkstyle:MagicNumber")
        @Override
        public int hashCode() {
            int result = apiConfiguration != null ? apiConfiguration.hashCode() : 0;
            result = 31 * result + (okHttpClient != null ? okHttpClient.hashCode() : 0);
            result = 31 * result + (subscriptionEndpoint != null ? subscriptionEndpoint.hashCode() : 0);
            return result;
        }
    }
}
