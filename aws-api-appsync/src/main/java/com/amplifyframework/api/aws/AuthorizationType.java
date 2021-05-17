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

import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.annotations.AuthRule;
import com.amplifyframework.util.Empty;

/**
 * The types of authorization one can use while talking to an Amazon
 * AppSync GraphQL backend.
 * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/security.html">AppSync Security</a>
 */
@SuppressWarnings("LineLength") // Web links
public enum AuthorizationType {

    /**
     * A hardcoded key which can provide throttling for an
     * unauthenticated API.
     * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/security.html#api-key-authorization">API Key Authorization</a>
     */
    API_KEY,

    /**
     * Use an IAM access/secret key credential pair to authorize access
     * to an API.
     * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/security.html#aws-iam-authorization">IAM Authorization</a>
     * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/introduction.html">IAM Introduction</a>
     */
    AWS_IAM,

    /**
     * OpenID Connect is a simple identity layer on top of OAuth2.0.
     * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/security.html#openid-connect-authorization">OpenID Connect Authorization</a>
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html">OpenID Connect Specification</a>
     */
    OPENID_CONNECT,

    /**
     * Control access to date by putting users into different
     * permissions pools.
     * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/security.html#amazon-cognito-user-pools-authorization">Amazon Cognito User Pools</a>
     */
    AMAZON_COGNITO_USER_POOLS,

    /**
     * No authorization.
     */
    NONE;

    /**
     * Look up an AuthorizationType by its String name.
     * @param name String representation of an authorization type
     * @return The corresponding authorization type
     */
    public static AuthorizationType from(String name) {
        for (final AuthorizationType authorizationType : values()) {
            if (authorizationType.name().equals(name)) {
                return authorizationType;
            }
        }
        throw new IllegalArgumentException("No such authorization type: " + name);
    }

    /**
     * Look up an AuthorizationType by inspecting an AuthRule annotation.
     * @param authRuleAnnotation The annotation obtained from a model.
     * @return The AuthorizationType for the provider.
     * @throws IllegalArgumentException if AuthRule's provider does not match an authorization type.
     */
    public static AuthorizationType from(@NonNull AuthRule authRuleAnnotation) {
        String providerName = authRuleAnnotation.provider();
        if (Empty.check(providerName)) {
            providerName = authRuleAnnotation.allow().getDefaultAuthProvider().name();
        }
        AuthStrategy.Provider authRuleProvider = AuthStrategy.Provider.valueOf(providerName);
        return from(authRuleProvider);
    }

    /**
     * Look up an AuthorizationType for a given auth rule provider.
     * @param authRuleProvider The {@link AuthStrategy.Provider} from an @auth rule.
     * @return The corresponding {@link AuthorizationType}.
     * @throws IllegalArgumentException if AuthRule's provider does not match an authorization type
     */
    public static AuthorizationType from(@NonNull AuthStrategy.Provider authRuleProvider) {
        switch (authRuleProvider) {
            case USER_POOLS:
                return AMAZON_COGNITO_USER_POOLS;
            case OIDC:
                return OPENID_CONNECT;
            case IAM:
                return AWS_IAM;
            case API_KEY:
                return API_KEY;
            default:
                throw new IllegalArgumentException("No compatible authorization type " +
                                                       "for the requested provider:" + authRuleProvider.name());
        }
    }
}
