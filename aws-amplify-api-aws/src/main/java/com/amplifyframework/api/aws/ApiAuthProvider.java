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

import androidx.annotation.NonNull;

import com.amplifyframework.api.aws.sigv4.ApiKeyAuthProvider;
import com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.OidcAuthProvider;

import com.amazonaws.auth.AWSCredentialsProvider;

import java.util.Objects;

/**
 * Wrapper class to contain Auth providers for
 * API multi-auth support.
 */
public final class ApiAuthProvider {
    private final ApiKeyAuthProvider apiKeyAuthProvider;
    private final AWSCredentialsProvider awsCredentialsProvider;
    private final CognitoUserPoolsAuthProvider cognitoUserPoolsAuthProvider;
    private final OidcAuthProvider oidcAuthProvider;

    private ApiAuthProvider(Builder builder) {
        this.apiKeyAuthProvider = builder.getApiKeyAuthProvider();
        this.awsCredentialsProvider = builder.getAWSCredentialsProvider();
        this.oidcAuthProvider = builder.getOidcAuthProvider();
        this.cognitoUserPoolsAuthProvider = builder.getCognitoUserPoolsAuthProvider();
    }

    /**
     * Gets the API key Auth provider.
     * @return an implementation of {@link ApiKeyAuthProvider}
     */
    public ApiKeyAuthProvider getApiKeyAuthProvider() {
        return this.apiKeyAuthProvider;
    }

    /**
     * Gets the AWS Credentials provider.
     * @return an implementation of {@link AWSCredentialsProvider}
     */
    public AWSCredentialsProvider getAWSCredentialsProvider() {
        return this.awsCredentialsProvider;
    }

    /**
     * Gets the OIDC Auth provider.
     * @return an implementation of {@link OidcAuthProvider}
     */
    public OidcAuthProvider getOidcAuthProvider() {
        return this.oidcAuthProvider;
    }

    /**
     * Gets the Cognito User Pools Auth provider.
     * @return an implementation of {@link CognitoUserPoolsAuthProvider}
     */
    public CognitoUserPoolsAuthProvider getCognitoUserPoolsAuthProvider() {
        return this.cognitoUserPoolsAuthProvider;
    }

    /**
     * Statically gets the builder for conveniently
     * configuring an immutable instance of {@link ApiAuthProvider}.
     * @return the builder object for {@link ApiAuthProvider}
     */
    public static ApiAuthProvider.Builder builder() {
        return new ApiAuthProvider.Builder();
    }

    /**
     * Statically gets the default builder for an instance
     * of {@link ApiAuthProvider} that is not configured.
     * @return default instance of {@link ApiAuthProvider}
     */
    public static ApiAuthProvider defaultProvider() {
        return builder().build();
    }

    /**
     * Static Builder class for conveniently constructing an
     * immutable instance of {@link ApiAuthProvider}.
     */
    public static final class Builder {
        private ApiKeyAuthProvider apiKeyAuthProvider;
        private AWSCredentialsProvider awsCredentialsProvider;
        private CognitoUserPoolsAuthProvider cognitoUserPoolsAuthProvider;
        private OidcAuthProvider oidcAuthProvider;

        /**
         * Assigns an API key Auth provider.
         * @param provider an instance of {@link ApiKeyAuthProvider}
         * @return this builder object for chaining
         */
        public ApiAuthProvider.Builder apiKeyAuthProvider(@NonNull ApiKeyAuthProvider provider) {
            ApiAuthProvider.Builder.this.apiKeyAuthProvider = Objects.requireNonNull(provider);
            return ApiAuthProvider.Builder.this;
        }

        /**
         * Assigns an AWS credentials provider.
         * @param provider an instance of {@link AWSCredentialsProvider}
         * @return this builder object for chaining
         */
        public ApiAuthProvider.Builder awsCredentialsProvider(@NonNull AWSCredentialsProvider provider) {
            ApiAuthProvider.Builder.this.awsCredentialsProvider = Objects.requireNonNull(provider);
            return ApiAuthProvider.Builder.this;
        }

        /**
         * Assigns a Cognito User Pools provider.
         * @param provider an instance of {@link CognitoUserPoolsAuthProvider}
         * @return this builder object for chaining
         */
        public ApiAuthProvider.Builder cognitoUserPoolsAuthProvider(@NonNull CognitoUserPoolsAuthProvider provider) {
            ApiAuthProvider.Builder.this.cognitoUserPoolsAuthProvider = Objects.requireNonNull(provider);
            return ApiAuthProvider.Builder.this;
        }

        /**
         * Assigns an OpenID Connect Auth provider.
         * @param provider an instance of {@link OidcAuthProvider}
         * @return this builder object for chaining
         */
        public ApiAuthProvider.Builder oidcAuthProvider(@NonNull OidcAuthProvider provider) {
            ApiAuthProvider.Builder.this.oidcAuthProvider = Objects.requireNonNull(provider);
            return ApiAuthProvider.Builder.this;
        }

        /**
         * Creates an immutable instance of {@link ApiAuthProvider}
         * configured to this builder instance.
         * @return The configured {@link ApiAuthProvider} instance
         */
        public ApiAuthProvider build() {
            return new ApiAuthProvider(ApiAuthProvider.Builder.this);
        }

        ApiKeyAuthProvider getApiKeyAuthProvider() {
            return ApiAuthProvider.Builder.this.apiKeyAuthProvider;
        }

        AWSCredentialsProvider getAWSCredentialsProvider() {
            return ApiAuthProvider.Builder.this.awsCredentialsProvider;
        }

        CognitoUserPoolsAuthProvider getCognitoUserPoolsAuthProvider() {
            return ApiAuthProvider.Builder.this.cognitoUserPoolsAuthProvider;
        }

        OidcAuthProvider getOidcAuthProvider() {
            return ApiAuthProvider.Builder.this.oidcAuthProvider;
        }
    }
}
