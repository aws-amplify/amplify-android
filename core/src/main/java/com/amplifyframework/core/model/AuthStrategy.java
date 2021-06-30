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
     * Owner authorization specifies whether a user can access or operate against an object.  To use OWNER, the API
     * must have Cognito User Pool configured.
     */
    OWNER(Provider.USER_POOLS),

    /**
     * Group authorization specifies whether a group can access or operate against an object.  To use GROUPS, the API
     * must have Cognito User Pool configured.
     */
    GROUPS(Provider.USER_POOLS),

    /**
     * The private authorization specifies that everyone will be allowed to access the API with a valid JWT token from
     * the configured Cognito User Pool. To use PRIVATE, the API must have Cognito User Pool configured.
     */
    PRIVATE(Provider.USER_POOLS),

    /**
     * The public authorization specifies that everyone will be allowed to access the API, behind the scenes the API
     * will be protected with an API Key. To use PUBLIC, the API must have API Key configured.
     */
    PUBLIC(Provider.API_KEY);

    private final Provider defaultAuthProvider;

    AuthStrategy(Provider defaultAuthProvider) {
        this.defaultAuthProvider = defaultAuthProvider;
    }

    /**
     * Returns the default provider for the strategy.
     * @return The default provider for the strategy.
     */
    public Provider getDefaultAuthProvider() {
        return defaultAuthProvider;
    }

    /**
     * Represents the the value of the provider field of the @auth directive.
     */
    public enum Provider {
        /**
         * The userPools provider of the @auth rule directive.
         */
        USER_POOLS("userPools"),

        /**
         * The userPools provider of the @auth rule directive.
         */
        OIDC("oidc"),

        /**
         * The userPools provider of the @auth rule directive.
         */
        IAM("iam"),

        /**
         * The userPools provider of the @auth rule directive.
         */
        API_KEY("apiKey");

        private final String authDirectiveProviderName;

        Provider(String authDirectiveProviderName) {
            this.authDirectiveProviderName = authDirectiveProviderName;
        }

        /**
         * Returns the provider name used in the @auth rule directive to represent the current enum item.
         * @return The name associated with the enum item.
         */
        public String getAuthDirectiveProviderName() {
            return this.authDirectiveProviderName;
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
