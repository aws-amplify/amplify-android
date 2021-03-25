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
import androidx.annotation.Nullable;

import com.amplifyframework.AmplifyException;
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
    private static final RequestDecorator NO_OP_REQUEST_DECORATOR = new RequestDecorator() {
        @Override
        public Request decorate(Request request) {
            return request;
        }
    };

    private final ApiAuthProviders apiAuthProviders;
    private final String region;
    private final AuthorizationType defaultAuthorizationType;
    private final String apiKey;

    /**
     * Constructor that accepts the API auth providers to be used with their respective request decorator.
     * @param apiAuthProviders An instance with fully configured auth providers for use when signing requests.
     * @param defaultAuthorizationType The authorization type to use as default.
     * @param region The AWS region where the API is deployed.
     */
    public ApiRequestDecoratorFactory(@NonNull ApiAuthProviders apiAuthProviders,
                                      @NonNull AuthorizationType defaultAuthorizationType,
                                      @NonNull String region) {
        this(apiAuthProviders, defaultAuthorizationType, region, null);
    }

    /**
     * Constructor that accepts the API auth providers to be used with their respective request decorator.
     * @param apiAuthProviders An instance with fully configured auth providers for use when signing requests.
     * @param defaultAuthorizationType The authorization type to use as default.
     * @param region The AWS region where the API is deployed.
     * @param apiKey The API key to use for APIs with API_KEY authentication type.
     */
    public ApiRequestDecoratorFactory(@NonNull ApiAuthProviders apiAuthProviders,
                                      @NonNull AuthorizationType defaultAuthorizationType,
                                      @NonNull String region,
                                      @Nullable String apiKey) {
        this.apiAuthProviders = Objects.requireNonNull(apiAuthProviders);
        this.defaultAuthorizationType = Objects.requireNonNull(defaultAuthorizationType);
        this.region = Objects.requireNonNull(region);
        this.apiKey = apiKey;
    }

    /**
     * Return the appropriate request decorator after inspecting the request.
     * @param graphQLRequest The graphQL request sent to the API.
     * @return The request decorator.
     * @throws ApiException If it's unable to retrieve the decorator for the given request.
     */
    public RequestDecorator fromGraphQLRequest(GraphQLRequest<?> graphQLRequest) throws ApiException {
        // Start with the default auth type.
        AuthorizationType authType = defaultAuthorizationType;
        // If it is an instance of AppSyncGraphQLRequest AND
        // the request's authorization type is not null
        if (graphQLRequest instanceof AppSyncGraphQLRequest<?>
            && ((AppSyncGraphQLRequest<?>) graphQLRequest).getAuthorizationType() != null) {
            authType = ((AppSyncGraphQLRequest<?>) graphQLRequest).getAuthorizationType();
        }
        return forAuthType(authType);
    }

    /**
     * Given a authorization type, it returns the appropriate request decorator.
     * @param authorizationType the authorization type to be used for the request.
     * @return the appropriate request decorator for the given authorization type.
     */
    private RequestDecorator forAuthType(@NonNull AuthorizationType authorizationType) throws ApiException {
        if (AuthorizationType.AMAZON_COGNITO_USER_POOLS.equals(authorizationType) &&
            apiAuthProviders.getCognitoUserPoolsAuthProvider() != null) {
            // By calling getLatestAuthToken() here instead of inside the lambda block, makes the exception
            // handling a little bit cleaner. If getLatestAuthToken() is called from inside the lambda expression
            // below, we'd have to surround it with a try catch. By doing it this way, if there's a problem,
            // the ApiException will just be bubbled up. Same for OPENID_CONNECT.
            final String token = apiAuthProviders.getCognitoUserPoolsAuthProvider().getLatestAuthToken();
            return new JWTTokenRequestDecorator(() -> token);
        } else if (AuthorizationType.OPENID_CONNECT.equals(authorizationType) &&
            apiAuthProviders.getOidcAuthProvider() != null) {
            final String token = apiAuthProviders.getOidcAuthProvider().getLatestAuthToken();
            return new JWTTokenRequestDecorator(() -> token);
        } else if (AuthorizationType.API_KEY.equals(authorizationType)) {
            if (apiAuthProviders.getApiKeyAuthProvider() != null) {
                return new ApiKeyRequestDecorator(apiAuthProviders.getApiKeyAuthProvider());
            } else if (apiKey != null) {
                return new ApiKeyRequestDecorator(() -> apiKey);
            } else {
                throw new ApiException("Attempting to authentication type API_KEY without an API key provider or " +
                                           "an API key in the config file", AmplifyException.TODO_RECOVERY_SUGGESTION);
            }
        } else if (AuthorizationType.AWS_IAM.equals(authorizationType) &&
            apiAuthProviders.getAWSCredentialsProvider() != null) {
            AppSyncV4Signer appSyncV4Signer = new AppSyncV4Signer(region);
            return new IamRequestDecorator(appSyncV4Signer, apiAuthProviders.getAWSCredentialsProvider());
        } else {
            return NO_OP_REQUEST_DECORATOR;
        }
    }
}
