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

package com.amplifyframework.auth.cognito;

import com.amplifyframework.auth.AuthProvider;

/**
 * Cognito extension of Auth Provider to include Cognito-specific preset providers.
 */
public final class AWSCognitoAuthProvider extends AuthProvider {
    private static final String DEVELOPER = "cognito-identity.amazonaws.com";

    private AWSCognitoAuthProvider(String providerKey) {
        super(providerKey);
    }

    /**
     * Factory method for getting a pre-configured AWS Developer provider.
     * @return a pre-configured AWS Developer provider
     */
    public static AuthProvider developer() {
        return new AWSCognitoAuthProvider(DEVELOPER);
    }
}
