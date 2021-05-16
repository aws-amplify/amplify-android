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
    OWNER(Provider.userPools),

    /**
     * Group authorization specifies whether a group can access or operate against an object.  To use GROUPS, the API
     * must have Cognito User Pool configured.
     */
    GROUPS(Provider.userPools),

    /**
     * The private authorization specifies that everyone will be allowed to access the API with a valid JWT token from
     * the configured Cognito User Pool. To use PRIVATE, the API must have Cognito User Pool configured.
     */
    PRIVATE(Provider.userPools),

    /**
     * The public authorization specifies that everyone will be allowed to access the API, behind the scenes the API
     * will be protected with an API Key. To use PUBLIC, the API must have API Key configured.
     */
    PUBLIC(Provider.apiKey);

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
        userPools,

        /**
         * The userPools provider of the @auth rule directive.
         */
        oidc,

        /**
         * The userPools provider of the @auth rule directive.
         */
        iam,

        /**
         * The userPools provider of the @auth rule directive.
         */
        apiKey;
    }
}
