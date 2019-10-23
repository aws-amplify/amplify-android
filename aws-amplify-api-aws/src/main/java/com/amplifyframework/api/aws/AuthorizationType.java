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

/**
 * The types of authorization one can use while talking to an Amazon
 * AppSync GraphQL backend.
 * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/security.html">AppSync Security</a>
 */
@SuppressWarnings("LineLength") // For long URLs in Javadocs
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
    AMAZON_COGNITO_USER_POOLS;

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
}

