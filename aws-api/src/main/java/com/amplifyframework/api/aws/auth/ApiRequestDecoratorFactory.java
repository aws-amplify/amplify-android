/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws.auth;

import androidx.annotation.NonNull;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.ApiAuthProviders;
import com.amplifyframework.api.aws.AppSyncGraphQLRequest;
import com.amplifyframework.api.aws.AuthorizationType;
import com.amplifyframework.api.aws.sigv4.AppSyncV4Signer;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.logging.Logger;

import java.util.Objects;

import okhttp3.Request;

/**
 * Factory class that creates instances of different implementations of {@link RequestDecorator}s.
 */
public final class ApiRequestDecoratorFactory {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-api");
    private static final RequestDecorator NO_OP_REQUEST_SIGNER = new RequestDecorator() {
        @Override
        public Request decorate(Request request) {
            return request;
        }
    };

    private final ApiAuthProviders apiAuthProviders;
    private final String region;
    private final AuthorizationType defaultAuthorizationType;

    /**
     * Constructor that accepts the API auth providers to be used with their respective request signer.
     * @param apiAuthProviders An instance with fully configured auth providers for use when signing requests.
     * @param defaultAuthorizationType The authorization type to use as default.
     * @param region The AWS region where the API is deployed.
     */
    public ApiRequestDecoratorFactory(@NonNull ApiAuthProviders apiAuthProviders,
                                      @NonNull AuthorizationType defaultAuthorizationType,
                                      @NonNull String region) {
        this.apiAuthProviders = Objects.requireNonNull(apiAuthProviders);
        this.defaultAuthorizationType = Objects.requireNonNull(defaultAuthorizationType);
        this.region = Objects.requireNonNull(region);
    }

    /**
     * Return the appropriate request signer after inspecting the request.
     * @param graphQLRequest The graphQL request sent to the API.
     * @return The request signer
     */
    public RequestDecorator fromGraphQLRequest(GraphQLRequest<?> graphQLRequest) {
        // If it's not a an instance of AppSyncGraphQLRequest OR
        // the request's authorization type is null
        AuthorizationType authType = defaultAuthorizationType;
        if (graphQLRequest instanceof AppSyncGraphQLRequest<?>
            && ((AppSyncGraphQLRequest<?>) graphQLRequest).getAuthorizationType() != null) {
            authType = ((AppSyncGraphQLRequest<?>) graphQLRequest).getAuthorizationType();
        }
        return forAuthType(authType);
    }

    /**
     * Given a authorization type, it returns the appropriate request signer.
     * @param authorizationType the authorization type to be used for the request.
     * @return the appropriate request signer for the given authorization type.
     */
    private RequestDecorator forAuthType(@NonNull AuthorizationType authorizationType) {
        if (AuthorizationType.AMAZON_COGNITO_USER_POOLS.equals(authorizationType) &&
            apiAuthProviders.getCognitoUserPoolsAuthProvider() != null) {
            return new JWTTokenRequestDecorator(new JWTTokenRequestDecorator.TokenSupplier() {
                @Override
                public String get() {
                    try {
                        return apiAuthProviders.getCognitoUserPoolsAuthProvider().getLatestAuthToken();
                    } catch (ApiException apiException) {
                        LOG.error("Failed to retrieve token from CognitoUserPoolsAuthProvider", apiException);
                        return null;
                    }
                }
            });
        } else if (AuthorizationType.OPENID_CONNECT.equals(authorizationType) &&
            apiAuthProviders.getOidcAuthProvider() != null) {
            return new JWTTokenRequestDecorator(new JWTTokenRequestDecorator.TokenSupplier() {
                @Override
                public String get() {
                    try {
                        return apiAuthProviders.getOidcAuthProvider().getLatestAuthToken();
                    } catch (ApiException apiException) {
                        LOG.error("Failed to retrieve token from OidcAuthProvider", apiException);
                        return null;
                    }
                }
            });
        } else if (AuthorizationType.API_KEY.equals(authorizationType) &&
            apiAuthProviders.getApiKeyAuthProvider() != null) {
            return new ApiKeyRequestDecorator(apiAuthProviders.getApiKeyAuthProvider());
        } else if (AuthorizationType.AWS_IAM.equals(authorizationType) &&
            apiAuthProviders.getAWSCredentialsProvider() != null) {
            AppSyncV4Signer appSyncV4Signer = new AppSyncV4Signer(region);
            return new IamRequestDecorator(appSyncV4Signer, apiAuthProviders.getAWSCredentialsProvider());
        } else {
            return NO_OP_REQUEST_SIGNER;
        }
    }
}
