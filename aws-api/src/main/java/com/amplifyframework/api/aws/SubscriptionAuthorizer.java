/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.api.ApiException.ApiAuthException;
import com.amplifyframework.api.aws.auth.IamRequestDecorator;
import com.amplifyframework.api.aws.sigv4.ApiKeyAuthProvider;
import com.amplifyframework.api.aws.sigv4.AppSyncV4Signer;
import com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.DefaultCognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.FunctionAuthProvider;
import com.amplifyframework.api.aws.sigv4.OidcAuthProvider;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.auth.CognitoCredentialsProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

final class SubscriptionAuthorizer {
    private static final String AUTH_DEPENDENCY_PLUGIN_KEY = "awsCognitoAuthPlugin";

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
    JSONObject createHeadersForSubscription(GraphQLRequest<?> request,
                                            AuthorizationType authorizationType) throws ApiException {
        return createHeaders(request, authorizationType, false);
    }

    /**
     * Return authorization json to be used explicitly for establishing connection.
     */
    JSONObject createHeadersForConnection(AuthorizationType authorizationType) throws ApiException {
        return createHeaders(null, authorizationType, true);
    }

    private JSONObject createHeaders(GraphQLRequest<?> request,
                                     AuthorizationType authType,
                                     boolean connectionFlag) throws ApiException {

        switch (authType) {
            case API_KEY:
                ApiKeyAuthProvider keyProvider = authProviders.getApiKeyAuthProvider();
                if (keyProvider == null) {
                    keyProvider = configuration::getApiKey;
                }
                return forApiKey(keyProvider);
            case AWS_IAM:
                CredentialsProvider credentialsProvider = authProviders.getAWSCredentialsProvider();
                if (credentialsProvider == null) {
                    credentialsProvider = new CognitoCredentialsProvider();
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
                        throw new ApiAuthException(
                                "OidcAuthProvider interface is not implemented.",
                                "Please implement OidcAuthProvider interface to return " +
                                        "appropriate token from the appropriate service."
                        );
                    };
                }
                return forOidc(oidcProvider);
            case AWS_LAMBDA:
                FunctionAuthProvider functionAuthProvider = authProviders.getFunctionAuthProvider();
                if (functionAuthProvider == null) {
                    functionAuthProvider = () -> {
                        throw new ApiAuthException(
                                "FunctionAuthProvider interface is not implemented.",
                                "Please implement FunctionAuthProvider interface to return " +
                                        "appropriate token from the appropriate service."
                        );
                    };
                }
                return forAwsLambda(functionAuthProvider);

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

    private JSONObject forAwsLambda(FunctionAuthProvider functionAuthProvider) throws ApiException {
        try {
            return new JSONObject()
                    .put("host", getHost())
                    .put("Authorization", functionAuthProvider.getLatestAuthToken());
        } catch (JSONException jsonException) {
            // This error should never be thrown
            throw new ApiException(
                    "Error constructing the authorization json for the AWS_LAMBDA auth type.",
                    jsonException, AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
            );
        }
    }

    private JSONObject forIam(
            CredentialsProvider credentialsProvider,
            GraphQLRequest<?> request,
            boolean connectionFlag
    ) throws ApiException {
        final URI apiUrl = getRequestEndpoint(connectionFlag);
        final String requestContent = request != null ? request.getContent() : "{}";

        // Construct a request to be signed
        Request okHttpRequest = new Request.Builder()
                .url(apiUrl.toString())
                .addHeader("accept", "application/json, text/javascript")
                .addHeader("content-type", "application/json; charset=UTF-8")
                .post(RequestBody.create(requestContent.getBytes(), MediaType.parse("application/json; charset=UTF-8")))
                .build();
        // Sign with Kotlin signer; connection resource path is set by `getRequestEndpoint()`
        // TODO : dev preview previously headers were signed here. check if IamRequestDecorator call is necessary
        AppSyncV4Signer signer = new AppSyncV4Signer(configuration.getRegion());
        Request decorated = new IamRequestDecorator(signer, credentialsProvider, "appsync").decorate(okHttpRequest);
        Map<String, List<String>> signedHeaders = decorated.headers().toMultimap();
        Map<String, String> simpleSignedHeaders = new HashMap<>();
        signedHeaders.forEach((k, v) -> simpleSignedHeaders.put(k, v.get(0)));
        return new JSONObject(simpleSignedHeaders);
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
}
