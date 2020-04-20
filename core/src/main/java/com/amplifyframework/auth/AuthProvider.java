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

package com.amplifyframework.auth;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

public class AuthProvider {
    private static final String AMAZON = "www.amazon.com";
    private static final String FACEBOOK = "graph.facebook.com";
    private static final String GOOGLE = "accounts.google.com";
    private static final String TWITTER = "api.twitter.com";

    private final String providerKey;

    protected AuthProvider(String providerKey) {
        this.providerKey = providerKey;
    }

    /**
     * Factory method for getting a pre-configured Amazon provider.
     * @return a pre-configured Amazon provider
     */
    public static AuthProvider amazon() {
        return new AuthProvider(AMAZON);
    }

    /**
     * Factory method for getting a pre-configured Facebook provider.
     * @return a pre-configured Facebook provider
     */
    public static AuthProvider facebook() {
        return new AuthProvider(FACEBOOK);
    }

    /**
     * Factory method for getting a pre-configured Google provider.
     * @return a pre-configured Google provider
     */
    public static AuthProvider google() {
        return new AuthProvider(GOOGLE);
    }

    /**
     * Factory method for getting a pre-configured Twitter provider.
     * @return a pre-configured Twitter provider
     */
    public static AuthProvider twitter() {
        return new AuthProvider(TWITTER);
    }

    /**
     * Factory method for creating your own custom provider.
     * @param providerKey The name of the custom auth provider
     * @return a custom provider
     */
    public static AuthProvider custom(String providerKey) {
        return new AuthProvider(providerKey);
    }

    /**
     * Returns the String key for the provider.
     * @return the String key for the provider
     */
    @NonNull
    public String getProviderKey() {
        return providerKey;
    }

    /**
     * When overriding, be sure to include providerKey in the hash.
     * @return Hash code of this object
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getProviderKey()
        );
    }

    /**
     * When overriding, be sure to include providerKey in the comparison.
     * @return True if the two objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            AuthProvider authProvider = (AuthProvider) obj;
            return ObjectsCompat.equals(getProviderKey(), authProvider.getProviderKey());
        }
    }

    /**
     * When overriding, be sure to include providerKey in the output string.
     * @return A string representation of the object
     */
    @Override
    public String toString() {
        return "AuthProvider{" +
                "providerKey=" + providerKey +
                '}';
    }
}
