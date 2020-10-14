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
import androidx.annotation.Nullable;

/**
 * The type of provider for an @auth directive rule.
 * @see <a href="https://docs.amplify.aws/cli/graphql-transformer/directives#auth">
 *     GraphQL Transformer @auth directive documentation.</a>
 */
public enum AuthProvider {
    /**
     * Uses API key to authenticate.
     * The following strategies are permitted:
     * <ul>
     *     <li>{@link AuthStrategy#PUBLIC}</li>
     * </ul>
     */
    API_KEY("apiKey"),

    /**
     * Uses AWS IAM to authenticate.
     * The following strategies are permitted:
     * <ul>
     *     <li>{@link AuthStrategy#PUBLIC}</li>
     *     <li>{@link AuthStrategy#PRIVATE}</li>
     * </ul>
     */
    IAM("iam"),

    /**
     * Uses OpenID Connect to authenticate.
     * The following strategies are permitted:
     * <ul>
     *     <li>{@link AuthStrategy#OWNER}</li>
     *     <li>{@link AuthStrategy#GROUPS}</li>
     * </ul>
     */
    OIDC("oidc"),

    /**
     * Uses Cognito User Pools to authenticate.
     * The following strategies are permitted:
     * <ul>
     *     <li>{@link AuthStrategy#OWNER}</li>
     *     <li>{@link AuthStrategy#GROUPS}</li>
     *     <li>{@link AuthStrategy#PRIVATE}</li>
     * </ul>
     */
    USER_POOLS("userPools");

    private final String name;

    AuthProvider(String name) {
        this.name = name;
    }

    /**
     * Returns the name of provider.
     * @return the name of provider
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Returns a matching {@link AuthProvider} enum instance from given string.
     * If there is no match, then defaults to {@link AuthProvider#USER_POOLS}.
     * @param name the name of provider
     * @return corresponding auth provider enum
     */
    @NonNull
    public AuthProvider fromName(@Nullable String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return USER_POOLS;
        }
    }
}
