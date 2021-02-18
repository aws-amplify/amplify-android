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

import java.util.Arrays;
import java.util.List;

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
    OWNER(AuthRuleProvider.AMAZON_COGNITO_USER_POOLS,
        Arrays.asList(new AuthRuleProvider[] {
            AuthRuleProvider.AMAZON_COGNITO_USER_POOLS,
            AuthRuleProvider.OPENID_CONNECT
        })
    ),

    /**
     * Group authorization specifies whether a group can access or operate against an object.  To use GROUPS, the API
     * must have Cognito User Pool configured.
     */
    GROUPS(AuthRuleProvider.AMAZON_COGNITO_USER_POOLS,
       Arrays.asList(new AuthRuleProvider[] {
           AuthRuleProvider.AMAZON_COGNITO_USER_POOLS,
           AuthRuleProvider.OPENID_CONNECT
       })
    ),

    /**
     * The private authorization specifies that everyone will be allowed to access the API with a valid JWT token from
     * the configured Cognito User Pool. To use PRIVATE, the API must have Cognito User Pool configured.
     */
    PRIVATE(AuthRuleProvider.AMAZON_COGNITO_USER_POOLS,
        Arrays.asList(new AuthRuleProvider[] {
            AuthRuleProvider.AMAZON_COGNITO_USER_POOLS,
            AuthRuleProvider.AWS_IAM
        })
    ),

    /**
     * The public authorization specifies that everyone will be allowed to access the API, behind the scenes the API
     * will be protected with an API Key. To use PUBLIC, the API must have API Key configured.
     */
    PUBLIC(AuthRuleProvider.API_KEY,
       Arrays.asList(new AuthRuleProvider[] {
           AuthRuleProvider.API_KEY,
           AuthRuleProvider.AMAZON_COGNITO_USER_POOLS
       })
    );

    private AuthRuleProvider defaultProvider;
    private List<AuthRuleProvider> compatibleProviders;
    AuthStrategy(AuthRuleProvider defaultProvider,
                 List<AuthRuleProvider> compatibleProviders) {
        this.defaultProvider = defaultProvider;
        this.compatibleProviders = compatibleProviders;
    }

    /**
     * Returns the default provider for this strategy.
     * @return the default auth provider.
     */
    public AuthRuleProvider getDefaultProvider() {
        return defaultProvider;
    }

    /**
     * Gets a list of the auth providers that can be used for this strategy.
     * @return A list of compatible auth providers.
     */
    public List<AuthRuleProvider> getCompatibleProviders() {
        return compatibleProviders;
    }
}
