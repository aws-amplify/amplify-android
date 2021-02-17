/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import android.util.Log;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.sigv4.ApiKeyAuthProvider;
import com.amplifyframework.api.aws.sigv4.AppSyncV4Signer;
import com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.DefaultCognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.OidcAuthProvider;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.ModelOperation;

import com.amazonaws.DefaultRequest;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.mobile.client.AWSMobileClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

final class SubscriptionAuthorizer {
    private static final String TAG = "SubscriptionAuthorizer";
    private static final String AUTH_DEPENDENCY_PLUGIN_KEY = "awsCognitoAuthPlugin";

    private final ApiConfiguration configuration;
    private final ApiAuthProviders authProviders;
    private final AuthProviderChainRepository authProviderChains;

    SubscriptionAuthorizer(ApiConfiguration configuration) {
        this(configuration, ApiAuthProviders.noProviderOverrides(), null);
    }

    SubscriptionAuthorizer(ApiConfiguration configuration,
                           ApiAuthProviders authProviders,
                           AuthProviderChainRepository authProviderChains) {
        this.configuration = configuration;
        this.authProviders = authProviders;
        this.authProviderChains = authProviderChains;
    }

    /**
     * Return authorization json to be used explicitly for subscription registration.
     */
    JSONObject createHeadersForSubscription(GraphQLRequest<?> request) throws ApiException {
        //TODO: Duplicated code between this and createHeadersForConnection
        if (authProviderChains != null) {
            String modelName = ((AppSyncGraphQLRequest<?>) request).getModelSchema().getName();
            AuthProviderChainRepository.AuthProviderChain chain =
                authProviderChains.getChain(modelName, ModelOperation.READ.name());
            JSONObject result = null;
            while (result == null && chain != null && chain.getCurrent() != null) {
                Log.d(TAG, "Subscription attempt => Auth chain state: " + chain.toString());
                try {
                    result = createHeaders(request, false, AuthorizationType.from(chain.getCurrent().name()));
                } catch (ApiException apiException) {
                    Log.w(TAG, "Unable to create headers for subscription request.", apiException);
                    chain.nextProvider();
                }
            }
            if (result == null) {
                //TODO: suggestions
                throw new ApiException("Unable to establish subscription", "");
            }
            return result;
        } else {
            return createHeaders(request, false);
        }
    }

    /**
     * Return authorization json to be used explicitly for establishing connection.
     */
    JSONObject createHeadersForConnection() throws ApiException {
        return createHeaders(null, true);
    }

    /**
     * Return authorization json to be used explicitly for establishing connection.
     */
    JSONObject createHeadersForConnection(GraphQLRequest<?> request) throws ApiException {
        if (authProviderChains != null) {
            String modelName = ((AppSyncGraphQLRequest<?>) request).getModelSchema().getName();
            AuthProviderChainRepository.AuthProviderChain chain =
                authProviderChains.getChain(modelName, ModelOperation.READ.name());
            JSONObject result = null;
            while (result == null && chain != null && chain.getCurrent() != null) {
                try {
                    result = createHeaders(request, true, AuthorizationType.from(chain.getCurrent().name()));
                } catch (ApiException apiException) {
                    Log.w(TAG, "Unable to create headers for subscription request.", apiException);
                    chain.nextProvider();
                }
            }
            if (result == null) {
                throw new ApiException("Unable to establish subscription", "");
            }
            return result;
        } else {
            return createHeaders(request, true);
        }
    }

    private JSONObject createHeaders(GraphQLRequest<?> request, boolean connectionFlag) throws ApiException {
        return createHeaders(request, connectionFlag, configuration.getAuthorizationType());
    }

    // TODO: Maybe createHeaders can be added as a second method of the AWSRequestSigner interface.
    private JSONObject createHeaders(GraphQLRequest<?> request,
                                     boolean connectionFlag,
                                     AuthorizationType authorizationType) throws ApiException {

        switch (authorizationType) {
            case API_KEY:
                ApiKeyAuthProvider keyProvider = authProviders.getApiKeyAuthProvider();
                if (keyProvider == null) {
                    keyProvider = configuration::getApiKey;
                }
                return forApiKey(keyProvider);
            case AWS_IAM:
                AWSCredentialsProvider credentialsProvider = authProviders.getAWSCredentialsProvider();
                if (credentialsProvider == null) {
                    credentialsProvider = getAWSMobileClient();
                }
                return forIam(credentialsProvider, request, connectionFlag);
            case AMAZON_COGNITO_USER_POOLS:
                CognitoUserPoolsAuthProvider cognitoProvider = authProviders.getCognitoUserPoolsAuthProvider();
                if (cognitoProvider == null) {
                    cognitoProvider = new DefaultCognitoUserPoolsAuthProvider();
                }
                return forCognitoUserPools(cognitoProvider);
            case OPENID_CONNECT:
                OidcAuthProvider oidcProvider = authProviders.getOidcAuthProvider();
                if (oidcProvider == null) {
                    oidcProvider = () -> {
                        throw new ApiException(
                                "OidcAuthProvider interface is not implemented.",
                                "Please implement OidcAuthProvider interface to return " +
                                        "appropriate token from the appropriate service."
                        );
                    };
                }
                return forOidc(oidcProvider);
            case NONE:
            default:
                return new JSONObject();
        }
    }

    private JSONObject forApiKey(ApiKeyAuthProvider keyProvider) throws ApiException {
        try {
            return new JSONObject()
                    .put("host", getHost())
                    .put("x-amz-date", Iso8601Timestamp.now())
                    .put("x-api-key", keyProvider.getAPIKey());
        } catch (JSONException jsonException) {
            // This error should never be thrown
            throw new ApiException(
                    "Error constructing the authorization json for Api key.",
                    jsonException, AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
            );
        }
    }

    private JSONObject forCognitoUserPools(CognitoUserPoolsAuthProvider cognitoProvider) throws ApiException {
        try {
            return new JSONObject()
                    .put("host", getHost())
                    .put("Authorization", cognitoProvider.getLatestAuthToken());
        } catch (JSONException jsonException) {
            // This error should never be thrown
            throw new ApiException(
                    "Error constructing the authorization json for Cognito User Pools.",
                    jsonException, AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
            );
        }
    }

    private JSONObject forOidc(OidcAuthProvider oidcProvider) throws ApiException {
        try {
            return new JSONObject()
                    .put("host", getHost())
                    .put("Authorization", oidcProvider.getLatestAuthToken());
        } catch (JSONException jsonException) {
            // This error should never be thrown
            throw new ApiException(
                    "Error constructing the authorization json for Open ID Connect.",
                    jsonException, AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
            );
        }
    }

    private JSONObject forIam(
            AWSCredentialsProvider credentialsProvider,
            GraphQLRequest<?> request,
            boolean connectionFlag
    ) throws ApiException {
        final URI apiUrl = getRequestEndpoint(connectionFlag);
        final String apiRegion = apiUrl.getAuthority().split("\\.")[2];
        final String requestContent = request != null ? request.getContent() : "{}";

        // Construct a request to be signed
        DefaultRequest<?> canonicalRequest = new DefaultRequest<>("appsync");
        canonicalRequest.setEndpoint(apiUrl);
        canonicalRequest.addHeader("accept", "application/json, text/javascript");
        canonicalRequest.addHeader("content-encoding", "amz-1.0");
        canonicalRequest.addHeader("content-type", "application/json; charset=UTF-8");
        canonicalRequest.setHttpMethod(HttpMethodName.valueOf("POST"));
        canonicalRequest.setContent(new ByteArrayInputStream(requestContent.getBytes()));

        // Sign with AppSync's SigV4 signer that also considers connection resource path
        new AppSyncV4Signer(apiRegion, connectionFlag).sign(
                canonicalRequest,
                credentialsProvider.getCredentials()
        );

        // Extract header from signed request and return
        Map<String, String> signedHeaders = canonicalRequest.getHeaders();
        return new JSONObject(signedHeaders);
    }

    private String getHost() {
        return Uri.parse(configuration.getEndpoint()).getHost();
    }

    private URI getRequestEndpoint(boolean connectionFlag) throws ApiException {
        try {
            String baseUrl = configuration.getEndpoint();
            String connectionUrl = connectionFlag ? baseUrl + "/connect" : baseUrl;
            return new URI(connectionUrl);
        } catch (URISyntaxException uriException) {
            throw new ApiException(
                    "Error constructing canonical URI for IAM request signature",
                    uriException,
                    "Verify that the API configuration contains valid GraphQL endpoint."
            );
        }
    }

    private AWSCredentialsProvider getAWSMobileClient() throws ApiException {
        try {
            return (AWSMobileClient) Amplify.Auth.getPlugin(AUTH_DEPENDENCY_PLUGIN_KEY).getEscapeHatch();
        } catch (IllegalStateException exception) {
            throw new ApiException(
                    "AWSApiPlugin depends on AWSCognitoAuthPlugin, but it is currently missing.",
                    exception,
                    "Before configuring Amplify, be sure to add AWSCognitoAuthPlugin same as you " +
                            "added AWSApiPlugin."
            );
        }
    }
}
