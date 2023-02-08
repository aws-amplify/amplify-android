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

import androidx.annotation.NonNull;

import com.amplifyframework.api.aws.sigv4.ApiKeyAuthProvider;
import com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.FunctionAuthProvider;
import com.amplifyframework.api.aws.sigv4.OidcAuthProvider;
import com.amplifyframework.auth.CognitoCredentialsProvider;

import java.util.Objects;

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider;

/**
 * Wrapper class to contain Auth providers for
 * API multi-auth support.
 */
public final class ApiAuthProviders {
    private final ApiKeyAuthProvider apiKeyAuthProvider;
    private final CredentialsProvider awsCredentialsProvider;
    private final CognitoUserPoolsAuthProvider cognitoUserPoolsAuthProvider;
    private final OidcAuthProvider oidcAuthProvider;
    private final FunctionAuthProvider functionAuthProvider;

    private ApiAuthProviders(Builder builder) {
        this.apiKeyAuthProvider = builder.getApiKeyAuthProvider();
        this.awsCredentialsProvider = builder.getAWSCredentialsProvider();
        this.oidcAuthProvider = builder.getOidcAuthProvider();
        this.cognitoUserPoolsAuthProvider = builder.getCognitoUserPoolsAuthProvider();
        this.functionAuthProvider = builder.getFunctionAuthProvider();
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
     * @return an implementation of {@link CredentialsProvider}
     */
    public CredentialsProvider getAWSCredentialsProvider() {
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
     * Gets the custom auth provider.
     * @return an implementation of {@link FunctionAuthProvider}
     */
    public FunctionAuthProvider getFunctionAuthProvider() {
        return this.functionAuthProvider;
    }

    /**
     * Statically gets the builder for conveniently
     * configuring an immutable instance of {@link ApiAuthProviders}.
     * @return the builder object for {@link ApiAuthProviders}
     */
    public static ApiAuthProviders.Builder builder() {
        return new ApiAuthProviders.Builder();
    }

    /**
     * Statically gets the default builder for an instance
     * of {@link ApiAuthProviders} that is not configured.
     * @return default instance of {@link ApiAuthProviders}
     */
    public static ApiAuthProviders noProviderOverrides() {
        return builder().build();
    }

    /**
     * Static Builder class for conveniently constructing an
     * immutable instance of {@link ApiAuthProviders}.
     */
    public static final class Builder {
        private ApiKeyAuthProvider apiKeyAuthProvider;
        private CredentialsProvider awsCredentialsProvider = new CognitoCredentialsProvider();
        private CognitoUserPoolsAuthProvider cognitoUserPoolsAuthProvider;
        private OidcAuthProvider oidcAuthProvider;
        private FunctionAuthProvider functionAuthProvider;

        /**
         * Assigns an API key Auth provider.
         * @param provider an instance of {@link ApiKeyAuthProvider}
         * @return this builder object for chaining
         */
        public ApiAuthProviders.Builder apiKeyAuthProvider(@NonNull ApiKeyAuthProvider provider) {
            ApiAuthProviders.Builder.this.apiKeyAuthProvider = Objects.requireNonNull(provider);
            return ApiAuthProviders.Builder.this;
        }

        /**
         * Assigns an AWS credentials provider.
         * @param provider an instance of {@link CredentialsProvider}
         * @return this builder object for chaining
         */
        public ApiAuthProviders.Builder awsCredentialsProvider(@NonNull CredentialsProvider provider) {
            ApiAuthProviders.Builder.this.awsCredentialsProvider = Objects.requireNonNull(provider);
            return ApiAuthProviders.Builder.this;
        }

        /**
         * Assigns a Cognito User Pools provider.
         * @param provider an instance of {@link CognitoUserPoolsAuthProvider}
         * @return this builder object for chaining
         */
        public ApiAuthProviders.Builder cognitoUserPoolsAuthProvider(@NonNull CognitoUserPoolsAuthProvider provider) {
            ApiAuthProviders.Builder.this.cognitoUserPoolsAuthProvider = Objects.requireNonNull(provider);
            return ApiAuthProviders.Builder.this;
        }

        /**
         * Assigns an OpenID Connect Auth provider.
         * @param provider an instance of {@link OidcAuthProvider}
         * @return this builder object for chaining
         */
        public ApiAuthProviders.Builder oidcAuthProvider(@NonNull OidcAuthProvider provider) {
            ApiAuthProviders.Builder.this.oidcAuthProvider = Objects.requireNonNull(provider);
            return ApiAuthProviders.Builder.this;
        }

        /**
         * Assigns a serverless function (e.g. AWS Lambda) auth provider.
         * @param provider an instance of {@link FunctionAuthProvider}
         * @return this builder object for chaining
         */
        public ApiAuthProviders.Builder functionAuthProvider(@NonNull FunctionAuthProvider provider) {
            ApiAuthProviders.Builder.this.functionAuthProvider = Objects.requireNonNull(provider);
            return ApiAuthProviders.Builder.this;
        }

        /**
         * Creates an immutable instance of {@link ApiAuthProviders}
         * configured to this builder instance.
         * @return The configured {@link ApiAuthProviders} instance
         */
        public ApiAuthProviders build() {
            return new ApiAuthProviders(ApiAuthProviders.Builder.this);
        }

        ApiKeyAuthProvider getApiKeyAuthProvider() {
            return ApiAuthProviders.Builder.this.apiKeyAuthProvider;
        }

        CredentialsProvider getAWSCredentialsProvider() {
            return ApiAuthProviders.Builder.this.awsCredentialsProvider;
        }

        CognitoUserPoolsAuthProvider getCognitoUserPoolsAuthProvider() {
            return ApiAuthProviders.Builder.this.cognitoUserPoolsAuthProvider;
        }

        OidcAuthProvider getOidcAuthProvider() {
            return ApiAuthProviders.Builder.this.oidcAuthProvider;
        }

        FunctionAuthProvider getFunctionAuthProvider() {
            return ApiAuthProviders.Builder.this.functionAuthProvider;
        }
    }
}
