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
import androidx.annotation.VisibleForTesting;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.ApiPlugin;
import com.amplifyframework.api.aws.operation.AWSRestOperation;
import com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.DefaultCognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.OidcAuthProvider;
import com.amplifyframework.api.events.ApiEndpointStatusChangeEvent;
import com.amplifyframework.api.events.ApiEndpointStatusChangeEvent.ApiEndpointStatus;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.rest.HttpMethod;
import com.amplifyframework.api.rest.RestOperation;
import com.amplifyframework.api.rest.RestOperationRequest;
import com.amplifyframework.api.rest.RestOptions;
import com.amplifyframework.api.rest.RestResponse;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.AuthRule;
import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.ModelOperation;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.util.UserAgent;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.util.CognitoJWTParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

/**
 * Plugin implementation to be registered with Amplify API category.
 * It uses OkHttp client to execute POST on GraphQL commands.
 */
@SuppressWarnings("TypeParameterHidesVisibleType") // <R> shadows >com.amplifyframework.api.aws.R
public final class AWSApiPlugin extends ApiPlugin<Map<String, OkHttpClient>> {
    private final Map<String, ClientDetails> apiDetails;
    private final GraphQLResponse.Factory gqlResponseFactory;
    private final ApiAuthProviders authProvider;
    private final ExecutorService executorService;

    private final Set<String> restApis;
    private final Set<String> gqlApis;

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
     *
     * @param apiAuthProvider configured instance of {@link ApiAuthProviders}
     */
    public AWSApiPlugin(@NonNull ApiAuthProviders apiAuthProvider) {
        this.apiDetails = new HashMap<>();
        this.gqlResponseFactory = new GsonGraphQLResponseFactory();
        this.authProvider = Objects.requireNonNull(apiAuthProvider);
        this.restApis = new HashSet<>();
        this.gqlApis = new HashSet<>();
        this.executorService = Executors.newCachedThreadPool();
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return "awsAPIPlugin";
    }

    @Override
    public void configure(
            JSONObject pluginConfiguration,
            @NonNull Context context
    ) throws ApiException {
        // Null-check for configuration is done inside readFrom method
        AWSApiPluginConfiguration pluginConfig =
                AWSApiPluginConfigurationReader.readFrom(pluginConfiguration);

        final InterceptorFactory interceptorFactory =
                new AppSyncSigV4SignerInterceptorFactory(authProvider);

        for (Map.Entry<String, ApiConfiguration> entry : pluginConfig.getApis().entrySet()) {
            final String apiName = entry.getKey();
            final ApiConfiguration apiConfiguration = entry.getValue();
            final EndpointType endpointType = apiConfiguration.getEndpointType();
            final OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.addNetworkInterceptor(UserAgentInterceptor.using(UserAgent::string));
            builder.eventListener(new ApiConnectionEventListener());
            if (apiConfiguration.getAuthorizationType() != AuthorizationType.NONE) {
                builder.addInterceptor(interceptorFactory.create(apiConfiguration));
            }
            final OkHttpClient okHttpClient = builder.build();
            final SubscriptionAuthorizer subscriptionAuthorizer =
                    new SubscriptionAuthorizer(apiConfiguration, authProvider);
            final SubscriptionEndpoint subscriptionEndpoint =
                    new SubscriptionEndpoint(apiConfiguration, gqlResponseFactory, subscriptionAuthorizer);
            if (EndpointType.REST.equals(endpointType)) {
                restApis.add(apiName);
            }
            if (EndpointType.GRAPHQL.equals(endpointType)) {
                gqlApis.add(apiName);
            }
            apiDetails.put(apiName, new ClientDetails(apiConfiguration, okHttpClient, subscriptionEndpoint));
        }
    }

    @NonNull
    @Override
    public Map<String, OkHttpClient> getEscapeHatch() {
        final Map<String, OkHttpClient> apiClientsByName = new HashMap<>();
        for (Map.Entry<String, ClientDetails> entry : apiDetails.entrySet()) {
            apiClientsByName.put(entry.getKey(), entry.getValue().getOkHttpClient());
        }
        return Collections.unmodifiableMap(apiClientsByName);
    }

    @NonNull
    @Override
    public String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @Nullable
    @Override
    public <R> GraphQLOperation<R> query(
            @NonNull GraphQLRequest<R> graphQLRequest,
            @NonNull Consumer<GraphQLResponse<R>> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        final String apiName;
        try {
            apiName = getSelectedApiName(EndpointType.GRAPHQL);
        } catch (ApiException exception) {
            onFailure.accept(exception);
            return null;
        }
        return query(apiName, graphQLRequest, onResponse, onFailure);
    }

    @Nullable
    @Override
    public <R> GraphQLOperation<R> query(
            @NonNull String apiName,
            @NonNull GraphQLRequest<R> graphQLRequest,
            @NonNull Consumer<GraphQLResponse<R>> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        try {
            final GraphQLOperation<R> operation =
                    buildAppSyncGraphQLOperation(apiName, graphQLRequest, onResponse, onFailure);
            operation.start();
            return operation;
        } catch (ApiException exception) {
            onFailure.accept(exception);
            return null;
        }
    }

    @Nullable
    @Override
    public <R> GraphQLOperation<R> mutate(
            @NonNull GraphQLRequest<R> graphQlRequest,
            @NonNull Consumer<GraphQLResponse<R>> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        final String apiName;
        try {
            apiName = getSelectedApiName(EndpointType.GRAPHQL);
        } catch (ApiException exception) {
            onFailure.accept(exception);
            return null;
        }
        return mutate(apiName, graphQlRequest, onResponse, onFailure);
    }

    @Nullable
    @Override
    public <R> GraphQLOperation<R> mutate(
            @NonNull String apiName,
            @NonNull GraphQLRequest<R> graphQLRequest,
            @NonNull Consumer<GraphQLResponse<R>> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        try {
            final GraphQLOperation<R> operation =
                    buildAppSyncGraphQLOperation(apiName, graphQLRequest, onResponse, onFailure);
            operation.start();
            return operation;
        } catch (ApiException exception) {
            onFailure.accept(exception);
            return null;
        }
    }

    @Nullable
    @Override
    public <R> GraphQLOperation<R> subscribe(
            @NonNull GraphQLRequest<R> graphQLRequest,
            @NonNull Consumer<String> onSubscriptionEstablished,
            @NonNull Consumer<GraphQLResponse<R>> onNextResponse,
            @NonNull Consumer<ApiException> onSubscriptionFailure,
            @NonNull Action onSubscriptionComplete) {
        final String apiName;
        try {
            apiName = getSelectedApiName(EndpointType.GRAPHQL);
        } catch (ApiException exception) {
            onSubscriptionFailure.accept(exception);
            return null;
        }
        return subscribe(
                apiName,
                graphQLRequest,
                onSubscriptionEstablished,
                onNextResponse,
                onSubscriptionFailure,
                onSubscriptionComplete
        );
    }

    @Nullable
    @Override
    public <R> GraphQLOperation<R> subscribe(
            @NonNull String apiName,
            @NonNull GraphQLRequest<R> graphQLRequest,
            @NonNull Consumer<String> onSubscriptionEstablished,
            @NonNull Consumer<GraphQLResponse<R>> onNextResponse,
            @NonNull Consumer<ApiException> onSubscriptionFailure,
            @NonNull Action onSubscriptionComplete) {
        final ClientDetails clientDetails = apiDetails.get(apiName);
        if (clientDetails == null) {
            onSubscriptionFailure.accept(
                    new ApiException(
                            "No client information for API named " + apiName,
                            "Check your amplify configuration to make sure there " +
                                    "is a correctly configured section for " + apiName
                    )
            );
            return null;
        }

        GraphQLRequest<R> request = graphQLRequest;
        if (request instanceof AppSyncGraphQLRequest) {
            try {
                AppSyncGraphQLRequest<R> appSyncRequest = (AppSyncGraphQLRequest<R>) request;
                AuthRule ownerRuleWithReadRestriction = null;
                ArrayList<String> readAuthorizedGroups = new ArrayList<>();

                // Note that we are intentionally supporting only one owner rule with a READ operation at this time.
                // If there is more than one, the operation will fail because AppSync generates a parameter for each
                // one. The question then is which one do we pass. JavaScript currently doesn't support this use case
                // and it's not clear what a good solution would be until AppSync supports real time filters.
                for (AuthRule authRule : appSyncRequest.getModelSchema().getAuthRules()) {
                    if (isReadRestrictingOwner(authRule)) {
                        if (ownerRuleWithReadRestriction == null) {
                            ownerRuleWithReadRestriction = authRule;
                        } else {
                            onSubscriptionFailure.accept(new ApiException("Detected multiple owner type auth rules " +
                                    "with a READ operation", "We currently do not support this use case. Please " +
                                    "limit your type to just one owner auth rule with a READ operation restriction."));
                            return null;
                        }
                    } else if (isReadRestrictingStaticGroup(authRule)) {
                        readAuthorizedGroups.addAll(authRule.getGroups());
                    }
                }

                // We only add the owner parameter to the subscription if there is an owner rule with a READ restriction
                // and either there are no group auth rules with read access or there are but the user isn't in any of
                // them.
                if (ownerRuleWithReadRestriction != null && (
                        readAuthorizedGroups.isEmpty() ||
                        Collections.disjoint(readAuthorizedGroups, getUserGroups(
                                clientDetails.apiConfiguration.getAuthorizationType()
                        ))
                    )
                ) {
                    String key = ownerRuleWithReadRestriction.getOwnerFieldOrDefault();
                    String type = "String!";
                    String value = getIdentityValue(
                            ownerRuleWithReadRestriction.getIdentityClaimOrDefault(),
                            clientDetails.apiConfiguration.getAuthorizationType()
                    );

                    request = appSyncRequest.newBuilder()
                            .variable(key, type, value)
                            .build();
                }
            } catch (AmplifyException exception) {
                onSubscriptionFailure.accept(new ApiException("Failed to set owner field on AppSyncGraphQLRequest",
                        exception, "See attached exception for details."));
                return null;
            }
        }

        SubscriptionOperation<R> operation = SubscriptionOperation.<R>builder()
            .subscriptionEndpoint(clientDetails.getSubscriptionEndpoint())
            .graphQlRequest(request)
            .responseFactory(gqlResponseFactory)
            .executorService(executorService)
            .onSubscriptionStart(onSubscriptionEstablished)
            .onNextItem(onNextResponse)
            .onSubscriptionError(onSubscriptionFailure)
            .onSubscriptionComplete(onSubscriptionComplete)
            .build();
        operation.start();
        return operation;
    }

    private boolean isReadRestrictingOwner(AuthRule authRule) {
        return AuthStrategy.OWNER.equals(authRule.getAuthStrategy())
            && authRule.getOperationsOrDefault().contains(ModelOperation.READ);
    }

    private boolean isReadRestrictingStaticGroup(AuthRule authRule) {
        return AuthStrategy.GROUPS.equals(authRule.getAuthStrategy())
            && authRule.getGroups() != null && !authRule.getGroups().isEmpty()
            && authRule.getOperationsOrDefault().contains(ModelOperation.READ);
    }

    private String getIdentityValue(String identityClaim, AuthorizationType authType) throws ApiException {
        String identityValue = null;

        try {
            identityValue = CognitoJWTParser
                    .getPayload(getAuthToken(authType))
                    .getString(identityClaim);
        } catch (JSONException error) {
            // Could not read identity value from the token...
            // Exception will be thrown so do nothing for now
        }

        if (identityValue == null || identityValue.isEmpty()) {
            throw new ApiException(
                    "Attempted to subscribe to a model with owner based authorization without " + identityClaim + " " +
                    "which was specified (or defaulted to) as the identity claim.",
                    "If you did not specify a custom identityClaim in your schema, make sure you are logged in. If " +
                            "you did, check that the value you specified in your schema is present in the access key."
            );
        }

        return identityValue;
    }

    private ArrayList<String> getUserGroups(AuthorizationType authType) throws ApiException {
        // TODO: this doesn't work with OIDC right now. Implement custom groups claim.
        ArrayList<String> groups = new ArrayList<>();
        final String GROUPS_KEY = "cognito:groups";

        try {
            JSONObject accessToken = CognitoJWTParser.getPayload(getAuthToken(authType));

            if (accessToken.has(GROUPS_KEY)) {
                JSONArray jsonGroups = accessToken.getJSONArray(GROUPS_KEY);

                for (int i = 0; i < jsonGroups.length(); i++) {
                    groups.add(jsonGroups.getString(i));
                }
            }
        } catch (JSONException error) {
            throw new ApiException(
                    "Failed to parse groups from auth rule.",
                    error,
                    "This should never happen - see attached exception for more details and report to us on GitHub."
            );
        }

        return groups;
    }

    private String getAuthToken(AuthorizationType authType) throws ApiException {
        final String token;
        switch (authType) {
            case AMAZON_COGNITO_USER_POOLS:
                CognitoUserPoolsAuthProvider cognitoProvider = authProvider.getCognitoUserPoolsAuthProvider();
                if (cognitoProvider == null) {
                    cognitoProvider = new DefaultCognitoUserPoolsAuthProvider();
                }
                return cognitoProvider.getLatestAuthToken();
            case OPENID_CONNECT:
                OidcAuthProvider oidcProvider = authProvider.getOidcAuthProvider();
                if (oidcProvider == null) {
                    throw new ApiException(
                        "OidcAuthProvider interface is not implemented.",
                        "Configure AWSApiPlugin with ApiAuthProviders containing an implementation of " +
                            "OidcAuthProvider interface that can vend a valid JWT token."
                    );
                }
                return oidcProvider.getLatestAuthToken();
            case API_KEY:
            case AWS_IAM:
            case NONE:
            default:
                throw new ApiException(
                    "Failed to obtain access token from the configured auth provider.",
                    "Verify that the API is configured with either Cognito User Pools or OpenID Connect."
                );
        }
    }

    @Nullable
    @Override
    public RestOperation get(
            @NonNull RestOptions options,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        final String apiName;
        try {
            apiName = getSelectedApiName(EndpointType.REST);
        } catch (ApiException exception) {
            onFailure.accept(exception);
            return null;
        }
        return get(apiName, options, onResponse, onFailure);
    }

    @Nullable
    @Override
    public RestOperation get(
            @NonNull String apiName,
            @NonNull RestOptions options,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        try {
            return createRestOperation(
                    apiName,
                    HttpMethod.GET,
                    options,
                    onResponse,
                    onFailure
            );
        } catch (ApiException exception) {
            onFailure.accept(exception);
            return null;
        }
    }

    @Nullable
    @Override
    public RestOperation put(
            @NonNull RestOptions options,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        final String apiName;
        try {
            apiName = getSelectedApiName(EndpointType.REST);
        } catch (ApiException exception) {
            onFailure.accept(exception);
            return null;
        }
        return put(apiName, options, onResponse, onFailure);
    }

    @Nullable
    @Override
    public RestOperation put(
            @NonNull String apiName,
            @NonNull RestOptions options,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        try {
            return createRestOperation(
                    apiName,
                    HttpMethod.PUT,
                    options,
                    onResponse,
                    onFailure
            );
        } catch (ApiException exception) {
            onFailure.accept(exception);
            return null;
        }
    }

    @Nullable
    @Override
    public RestOperation post(
            @NonNull RestOptions options,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        final String apiName;
        try {
            apiName = getSelectedApiName(EndpointType.REST);
        } catch (ApiException exception) {
            onFailure.accept(exception);
            return null;
        }
        return post(apiName, options, onResponse, onFailure);
    }

    @Nullable
    @Override
    public RestOperation post(
            @NonNull String apiName,
            @NonNull RestOptions options,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        try {
            return createRestOperation(
                    apiName,
                    HttpMethod.POST,
                    options,
                    onResponse,
                    onFailure
            );
        } catch (ApiException exception) {
            onFailure.accept(exception);
            return null;
        }
    }

    @Nullable
    @Override
    public RestOperation delete(
            @NonNull RestOptions options,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        final String apiName;
        try {
            apiName = getSelectedApiName(EndpointType.REST);
        } catch (ApiException exception) {
            onFailure.accept(exception);
            return null;
        }
        return delete(apiName, options, onResponse, onFailure);
    }

    @Nullable
    @Override
    public RestOperation delete(
            @NonNull String apiName,
            @NonNull RestOptions options,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        try {
            return createRestOperation(
                    apiName,
                    HttpMethod.DELETE,
                    options,
                    onResponse,
                    onFailure
            );
        } catch (ApiException exception) {
            onFailure.accept(exception);
            return null;
        }
    }

    @Nullable
    @Override
    public RestOperation head(
            @NonNull RestOptions options,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        final String apiName;
        try {
            apiName = getSelectedApiName(EndpointType.REST);
        } catch (ApiException exception) {
            onFailure.accept(exception);
            return null;
        }
        return head(apiName, options, onResponse, onFailure);
    }

    @Nullable
    @Override
    public RestOperation head(
            @NonNull String apiName,
            @NonNull RestOptions options,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        try {
            return createRestOperation(
                    apiName,
                    HttpMethod.HEAD,
                    options,
                    onResponse,
                    onFailure
            );
        } catch (ApiException exception) {
            onFailure.accept(exception);
            return null;
        }
    }

    @Nullable
    @Override
    public RestOperation patch(
            @NonNull RestOptions options,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        final String apiName;
        try {
            apiName = getSelectedApiName(EndpointType.REST);
        } catch (ApiException exception) {
            onFailure.accept(exception);
            return null;
        }
        return patch(apiName, options, onResponse, onFailure);
    }

    @Nullable
    @Override
    public RestOperation patch(
            @NonNull String apiName,
            @NonNull RestOptions options,
            @NonNull Consumer<RestResponse> onResponse,
            @NonNull Consumer<ApiException> onFailure) {
        try {
            return createRestOperation(
                    apiName,
                    HttpMethod.PATCH,
                    options,
                    onResponse,
                    onFailure
            );
        } catch (ApiException exception) {
            onFailure.accept(exception);
            return null;
        }
    }

    @VisibleForTesting
    String getSelectedApiName(EndpointType endpointType) throws ApiException {
        switch (endpointType) {
            case REST:
                return selectApiName(restApis);
            case GRAPHQL:
                return selectApiName(gqlApis);
            default:
                throw new ApiException(endpointType.name() + " is not a " +
                        "supported endpoint type.",
                        "Please use REST or GraphQL as endpoint type.");
        }
    }

    private String selectApiName(Set<String> apiClients) throws ApiException {
        if (apiClients.isEmpty()) {
            throw new ApiException("There is no API configured for this " +
                    "plugin with matching endpoint type.",
                    "Please add at least one API in amplifyconfiguration.json.");
        }
        if (apiClients.size() > 1) {
            throw new ApiException("There is more than one API configured " +
                    "for this plugin with matching endpoint type.",
                    "Please specify the name of API to invoke in the API method.");
        }
        return apiClients.iterator().next();
    }

    private <R> AppSyncGraphQLOperation<R> buildAppSyncGraphQLOperation(
            @NonNull String apiName,
            @NonNull GraphQLRequest<R> graphQLRequest,
            @NonNull Consumer<GraphQLResponse<R>> onResponse,
            @NonNull Consumer<ApiException> onFailure)
            throws ApiException {
        final ClientDetails clientDetails = apiDetails.get(apiName);
        if (clientDetails == null) {
            throw new ApiException(
                    "No client information for API named " + apiName,
                    "Check your amplify configuration to make sure there " +
                            "is a correctly configured section for " + apiName
            );
        }

        return AppSyncGraphQLOperation.<R>builder()
                .endpoint(clientDetails.getApiConfiguration().getEndpoint())
                .client(clientDetails.getOkHttpClient())
                .request(graphQLRequest)
                .responseFactory(gqlResponseFactory)
                .onResponse(onResponse)
                .onFailure(onFailure)
                .build();
    }

    /**
     * Creates a HTTP REST operation.
     * @param type     Operation type
     * @param options  Request options
     * @param onResponse Called when a response is available
     * @param onFailure  Called when no response is available
     * @return A REST Operation
     */
    private RestOperation createRestOperation(
            String apiName,
            HttpMethod type,
            RestOptions options,
            Consumer<RestResponse> onResponse,
            Consumer<ApiException> onFailure) throws ApiException {
        final ClientDetails clientDetails = apiDetails.get(apiName);
        if (clientDetails == null) {
            throw new ApiException(
                    "No client information for API named " + apiName,
                    "Check your amplify configuration to make sure there " +
                            "is a correctly configured section for " + apiName
            );
        }
        RestOperationRequest operationRequest;
        switch (type) {
            // These ones are special, they don't use any data.
            case HEAD:
            case GET:
            case DELETE:
                if (options.hasData()) {
                    throw new ApiException("HTTP method does not support data object! " + type,
                            "Try sending the request without any data in the options.");
                }
                operationRequest = new RestOperationRequest(
                        type,
                        options.getPath(),
                        options.getHeaders(),
                        options.getQueryParameters());
                break;
            case PUT:
            case POST:
            case PATCH:
                operationRequest = new RestOperationRequest(
                        type,
                        options.getPath(),
                        options.getData() == null ? new byte[0] : options.getData(),
                        options.getHeaders(),
                        options.getQueryParameters());
                break;
            default:
                throw new ApiException("Unknown REST operation type: " + type,
                        "Send support type for the request.");
        }
        AWSRestOperation operation = new AWSRestOperation(operationRequest,
                clientDetails.apiConfiguration.getEndpoint(),
                clientDetails.okHttpClient,
                onResponse,
                onFailure
        );
        operation.start();
        return operation;
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

        ApiConfiguration getApiConfiguration() {
            return apiConfiguration;
        }

        OkHttpClient getOkHttpClient() {
            return okHttpClient;
        }

        SubscriptionEndpoint getSubscriptionEndpoint() {
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

        @Override
        public int hashCode() {
            int result = apiConfiguration != null ? apiConfiguration.hashCode() : 0;
            result = 31 * result + (okHttpClient != null ? okHttpClient.hashCode() : 0);
            result = 31 * result + (subscriptionEndpoint != null ? subscriptionEndpoint.hashCode() : 0);
            return result;
        }
    }

    /**
     * This class implements OkHttp's {@link EventListener}. Its main purpose
     * is to listen to network-related events reported by the http client and trigger
     * a Hub event if necessary.
     */
    private static final class ApiConnectionEventListener extends EventListener {
        private final AtomicReference<ApiEndpointStatus> currentNetworkStatus;

        ApiConnectionEventListener() {
            currentNetworkStatus = new AtomicReference<>(ApiEndpointStatus.UNKOWN);
        }

        @Override
        public void connectFailed(@NonNull Call call,
                                  @NonNull InetSocketAddress inetSocketAddress,
                                  @NonNull Proxy proxy,
                                  @Nullable Protocol protocol,
                                  @NonNull IOException ioe) {
            super.connectFailed(call, inetSocketAddress, proxy, protocol, ioe);
            transitionTo(ApiEndpointStatus.NOT_REACHABLE);
        }

        @Override
        public void connectionAcquired(@NonNull Call call, @NonNull Connection connection) {
            super.connectionAcquired(call, connection);
            transitionTo(ApiEndpointStatus.REACHABLE);
        }

        private void transitionTo(ApiEndpointStatus newStatus) {
            ApiEndpointStatus previousStatus = currentNetworkStatus.getAndSet(newStatus);
            if (previousStatus != newStatus) {
                ApiEndpointStatusChangeEvent apiEndpointStatusChangeEvent = previousStatus.transitionTo(newStatus);
                Amplify.Hub.publish(HubChannel.API, apiEndpointStatusChangeEvent.toHubEvent());
            }
        }
    }
}
