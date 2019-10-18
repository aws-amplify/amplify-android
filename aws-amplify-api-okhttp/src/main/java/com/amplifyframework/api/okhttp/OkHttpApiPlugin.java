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

package com.amplifyframework.api.okhttp;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.ApiPlugin;
import com.amplifyframework.api.graphql.GraphQLCallback;
import com.amplifyframework.api.graphql.GraphQLQuery;
import com.amplifyframework.api.graphql.OperationType;
import com.amplifyframework.api.graphql.Query;
import com.amplifyframework.api.graphql.ResponseFactory;
import com.amplifyframework.core.plugin.PluginException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Plugin implementation to be registered with Amplify API category.
 * It uses OkHttp client to execute POST on graphql commands.
 */
public final class OkHttpApiPlugin extends ApiPlugin<OkHttpClient> {

    /** Header name for API key. */
    public static final String API_KEY_HEADER = "x-api-key";

    private static final String TAG = OkHttpApiPlugin.class.getSimpleName();

    private final Map<String, ClientDetails> httpClients;
    private final ResponseFactory responseFactory = new GsonResponseFactory();

    /**
     * Default constructor for this plugin.
     */
    public OkHttpApiPlugin() {
        httpClients = new HashMap<>();
        Log.d(TAG, "OkHttpAPIPlugin created");
    }

    @Override
    public String getPluginKey() {
        return "okAPI";
    }

    @Override
    public void configure(@NonNull Object pluginConfiguration,
                          Context context) throws PluginException {
        OkHttpApiPluginConfiguration configuration =
                (OkHttpApiPluginConfiguration) pluginConfiguration;

        for (Map.Entry<String, ApiConfiguration> entry : configuration.getApis().entrySet()) {
            final String apiName = entry.getKey();
            final ApiConfiguration apiConfiguration = entry.getValue();

            Headers.Builder headerBuilder = new Headers.Builder();
            switch (apiConfiguration.getAuthType()) {
                case API_KEY:
                    headerBuilder.add(API_KEY_HEADER, apiConfiguration.getApiKey());
                    break;
                case AWS_IAM:
                case AMAZON_COGNITO_USER_POOLS:
                case OPENID_CONNECT:
                default:
                    throw new PluginException.PluginConfigurationException(
                            "Unsupported authentication mode.");
            }

            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();

                        Request request = original.newBuilder()
                                .headers(headerBuilder.build())
                                .method(original.method(), original.body())
                                .build();

                        return chain.proceed(request);
                    })
                    .build();

            httpClients.put(apiName, new ClientDetails(apiConfiguration.getEndpoint(), httpClient));
            Log.i(TAG, String.format("HttpClient for %s configured", apiName));
        }
        Log.i(TAG, "OkHttpAPIPlugin configured");
    }

    @Override
    public <T> GraphQLQuery<T> graphql(@NonNull String apiName,
                                       @NonNull OperationType operationType,
                                       @NonNull String document,
                                       @NonNull Class<T> classToCast) {
        return graphql(apiName, operationType, document, classToCast, null);
    }

    @Override
    public <T> GraphQLQuery<T> graphql(@NonNull String apiName,
                                       @NonNull OperationType operationType,
                                       @NonNull String document,
                                       @NonNull Class<T> classToCast,
                                       GraphQLCallback<T> callback) {
        Log.i(TAG, "Invoking query from plugin: " + document);
        GraphQLQuery<T> gqlQuery =
            new GraphQLQuery<>(operationType, document, classToCast) .withCallback(callback);
        enqueue(apiName, gqlQuery);

        return gqlQuery;
    }

    //Helper method to construct a POST request for given query
    private void enqueue(String apiName, final Query<?> query) {
        try {
            final ClientDetails clientDetails = httpClients.get(apiName);
            if (clientDetails == null) {
                throw new ApiException("No client information for API named " + apiName);
            }
            final String endpoint = clientDetails.getEndpoint();
            final OkHttpClient httpClient = clientDetails.getClient();

            httpClient.newCall(
                    new Request.Builder()
                            .url(endpoint)
                            .addHeader("accept", "application/json")
                            .addHeader("content-type", "application/json")
                            .post(
                                    RequestBody.create(MediaType.parse("application/json"), query.getContent())
                            )
                            .build())
                    .enqueue(new okhttp3.Callback() {
                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            final ResponseBody responseBody = response.body();
                            String json = null;
                            if (responseBody != null) {
                                json = responseBody.string();
                            }
                            Log.i("GRAPHQL", query.getContent());
                            query.onResponse(responseFactory, json);
                        }

                        @Override
                        public void onFailure(Call call, IOException ioe) {
                            ioe.printStackTrace();
                            query.onError(ioe);
                        }
                    });
        } catch (Exception error) {
            query.onError(error);
        }
    }

    @Override
    public OkHttpClient getEscapeHatch() {
        return null;
    }

    /**
     * Wrapper class to pair http client with dedicated endpoint.
     */
    class ClientDetails {
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
