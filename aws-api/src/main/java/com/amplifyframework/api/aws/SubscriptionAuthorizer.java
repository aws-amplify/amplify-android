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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.sigv4.ApiKeyAuthProvider;
import com.amplifyframework.api.aws.sigv4.AppSyncV4Signer;
import com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.DefaultCognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.OidcAuthProvider;
import com.amplifyframework.api.graphql.GraphQLRequest;

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
    private final ApiConfiguration configuration;
    private final ApiAuthProviders authProviders;

    SubscriptionAuthorizer(ApiConfiguration configuration) {
        this(configuration, ApiAuthProviders.noProviderOverrides());
    }

    SubscriptionAuthorizer(ApiConfiguration configuration, ApiAuthProviders authProviders) {
        this.configuration = configuration;
        this.authProviders = authProviders;
    }

    /**
     * Return authorization json to be used explicitly for subscription registration.
     */
    JSONObject createHeaderForSubscription(GraphQLRequest<?> request) throws ApiException {
        return createHeader(request, false);
    }

    /**
     * Return authorization json to be used explicitly for establishing connection.
     */
    JSONObject createHeaderForConnection() throws ApiException {
        return createHeader(null, true);
    }

    private JSONObject createHeader(GraphQLRequest<?> request, boolean connectionFlag) throws ApiException {
        switch (configuration.getAuthorizationType()) {
            case API_KEY:
                ApiKeyAuthProvider keyProvider = authProviders.getApiKeyAuthProvider();
                if (keyProvider == null) {
                    keyProvider = configuration::getApiKey;
                }
                return forApiKey(keyProvider);
            case AWS_IAM:
                AWSCredentialsProvider credentialsProvider = authProviders.getAWSCredentialsProvider();
                if (credentialsProvider == null) {
                    credentialsProvider = AWSMobileClient.getInstance();
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
                                AmplifyException.TODO_RECOVERY_SUGGESTION
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
        final String apiKey = keyProvider.getAPIKey();
        try {
            return new JSONObject()
                    .put("host", getHost())
                    .put("x-amz-date", Iso8601Timestamp.now())
                    .put("x-api-key", apiKey);
        } catch (JSONException jsonException) {
            throw new ApiException(
                    "Error constructing the authorization json for Api key. ",
                    jsonException, AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
    }

    private JSONObject forCognitoUserPools(CognitoUserPoolsAuthProvider cognitoProvider) throws ApiException {
        try {
            return new JSONObject()
                    .put("host", getHost())
                    .put("Authorization", cognitoProvider.getLatestAuthToken());
        } catch (JSONException jsonException) {
            throw new ApiException(
                    "Error constructing the authorization json for Cognito User Pools.",
                    jsonException, AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
    }

    private JSONObject forOidc(OidcAuthProvider oidcProvider) throws ApiException {
        try {
            return new JSONObject()
                    .put("host", getHost())
                    .put("Authorization", oidcProvider.getLatestAuthToken());
        } catch (JSONException jsonException) {
            throw new ApiException(
                    "Error constructing the authorization json for Open ID Connect.",
                    jsonException, AmplifyException.TODO_RECOVERY_SUGGESTION
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
        final String requestContent = getRequestContent(request, connectionFlag);

        // Construct a request to be signed
        DefaultRequest<?> canonicalRequest = new DefaultRequest<>("appsync");
        canonicalRequest.addHeader("accept", "application/json, text/javascript");
        canonicalRequest.addHeader("content-encoding", "amz-1.0");
        canonicalRequest.addHeader("content-type", "application/json; charset=UTF-8");
        canonicalRequest.setHttpMethod(HttpMethodName.valueOf("POST"));
        canonicalRequest.setEndpoint(apiUrl);
        canonicalRequest.setContent(new ByteArrayInputStream(requestContent.getBytes()));

        // Sign with AppSync's SigV4 signer that also considers connection resource path
        new AppSyncV4Signer(apiRegion, connectionFlag).sign(
                canonicalRequest,
                credentialsProvider.getCredentials()
        );

        // Extract header from signed request and return
        Map<String, String> signedHeaders = canonicalRequest.getHeaders();
        JSONObject authorization = new JSONObject();
        try {
            for (Map.Entry<String, String> headerEntry : signedHeaders.entrySet()) {
                authorization.put(headerEntry.getKey(), headerEntry.getValue());
            }
        } catch (JSONException jsonException) {
            throw new ApiException(
                    "Error constructing the authorization json for AWS IAM.",
                    jsonException, AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
        return authorization;
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
                    uriException, AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
    }

    private String getRequestContent(GraphQLRequest<?> request, boolean connectionFlag) throws ApiException {
        if (connectionFlag) {
            return "{}";
        }
        try {
            return new JSONObject()
                    .put("query", request.getContent())
                    .put("variables", new JSONObject(request.getVariables()))
                    .toString();
        } catch (JSONException jsonException) {
            throw new ApiException(
                    "Error constructing JSON object with the subscription request data.",
                    jsonException, AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
    }
}
