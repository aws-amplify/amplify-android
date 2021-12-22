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

package com.amplifyframework.core.model;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 *  The type of strategy for an @auth directive rule.
 * @see <a href="https://docs.amplify.aws/cli/graphql-transformer/directives#auth">GraphQL Transformer @auth directive
 *  * documentation.</a>
 */
public enum AuthStrategy {
    /**
     * Custom authorization restricts access based on anything, as defined by the customer, such as via an AWS Lambda
     * serverless function.  To use CUSTOM, the API must have the AWS_LAMBDA auth type configured.
     */
    CUSTOM(Provider.FUNCTION, 1),

    /**
     * Owner authorization specifies whether a user can access or operate against an object.  To use OWNER, the API
     * must have Cognito User Pool configured.
     */
    OWNER(Provider.USER_POOLS, 2),

    /**
     * Group authorization specifies whether a group can access or operate against an object.  To use GROUPS, the API
     * must have Cognito User Pool configured.
     */
    GROUPS(Provider.USER_POOLS, 3),

    /**
     * The private authorization specifies that everyone will be allowed to access the API with a valid JWT token from
     * the configured Cognito User Pool. To use PRIVATE, the API must have Cognito User Pool configured.
     */
    PRIVATE(Provider.USER_POOLS, 4),

    /**
     * The public authorization specifies that everyone will be allowed to access the API, behind the scenes the API
     * will be protected with an API Key. To use PUBLIC, the API must have API Key configured.
     */
    PUBLIC(Provider.API_KEY, 5);

    private final Provider defaultAuthProvider;
    private final int priority;

    AuthStrategy(Provider defaultAuthProvider, int priority) {
        this.defaultAuthProvider = defaultAuthProvider;
        this.priority = priority;
    }

    /**
     * Returns the default provider for the strategy.
     * @return The default provider for the strategy.
     */
    public Provider getDefaultAuthProvider() {
        return defaultAuthProvider;
    }

    /**
     * Returns an integer that represents the rank of a given
     * strategy among its peers. (OWNER=1, GROUP=2, PRIVATE=3, PUBLIC=4)
     * @return The priority value of the strategy.
     */
    public int getPriority() {
        return this.priority;
    }

    /**
     * Represents the value of the provider field of the @auth directive.
     */
    public enum Provider {
        /**
         * The function provider of the @auth rule directive.
         */
        FUNCTION("function", 1),

        /**
         * The userPools provider of the @auth rule directive.
         */
        USER_POOLS("userPools", 2),

        /**
         * The OIDC provider of the @auth rule directive.
         */
        OIDC("oidc", 3),

        /**
         * The IAM provider of the @auth rule directive.
         */
        IAM("iam", 4),

        /**
         * The apiKey provider of the @auth rule directive.
         */
        API_KEY("apiKey", 5);

        private final String authDirectiveProviderName;
        private final int priority;

        Provider(String authDirectiveProviderName,
                 int priority) {
            this.authDirectiveProviderName = authDirectiveProviderName;
            this.priority = priority;
        }

        /**
         * Returns the provider name used in the @auth rule directive to represent the current enum item.
         * @return The name associated with the enum item.
         */
        public String getAuthDirectiveProviderName() {
            return this.authDirectiveProviderName;
        }

        /**
         * Returns an integer that represents the rank of a given provider among its peers.  They are ordered from
         * "most specific" to "least specific".
         *   1: FUNCTION
         *   2: USER_POOLS
         *   3: OIDC
         *   4: IAM
         *   5: API_KEY
         * @return The priority value of the strategy.
         */
        public int getPriority() {
            return priority;
        }

        /**
         * Retrieve the enum value for the given name.
         * @param authDirectiveProviderName String value of the provider attribute of the @auth directive.
         * @return The associated {@link Provider} item associated with the name.
         * @throws IllegalArgumentException If the value provided in the parameter does not
         * have a matching element in the enum.
         */
        public static Provider from(@NonNull String authDirectiveProviderName) {
            Objects.requireNonNull(authDirectiveProviderName);
            for (Provider provider : Provider.values()) {
                if (authDirectiveProviderName.equals(provider.authDirectiveProviderName)) {
                    return provider;
                }
            }
            throw new IllegalArgumentException("Unable to find a matching " +
                                                   "auth strategy for " + authDirectiveProviderName);
        }
    }
}
