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

package com.amplifyframework.auth.cognito.util;

import com.amplifyframework.auth.AuthProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Helpful util class to convert AuthProvider values to Cognito Identity Provider values.
 */
public final class AuthProviderConverter {
    private static final Map<AuthProvider, String> CONVERT_AUTH_PROVIDER;

    /**
     * Dis-allows instantiation of this class.
     */
    private AuthProviderConverter() { }

    static {
        Map<AuthProvider, String> convertAuthProviderInit = new HashMap<>();
        convertAuthProviderInit.put(AuthProvider.amazon(), "LoginWithAmazon");
        convertAuthProviderInit.put(AuthProvider.facebook(), "Facebook");
        convertAuthProviderInit.put(AuthProvider.google(), "Google");
        convertAuthProviderInit.put(AuthProvider.apple(), "SignInWithApple");
        CONVERT_AUTH_PROVIDER = Collections.unmodifiableMap(convertAuthProviderInit);
    }

    /**
     * Take an AuthProvider value and convert it to the string expected for a Cognito Identity Provider.
     * NOTE: If the AuthProvider supplied is not in the conversion map, this method will simply return the
     *       String version of the AuthProvider key to support custom values.
     * @param fromProvider the Auth Provider value to convert from
     * @return The Cognito Identity Provider String for the specified provider.
     */
    public static String getIdentityProvider(AuthProvider fromProvider) {
        String convertedVal = CONVERT_AUTH_PROVIDER.get(fromProvider);

        if (convertedVal != null) {
            return convertedVal;
        } else {
            return fromProvider.getProviderKey();
        }
    }
}
