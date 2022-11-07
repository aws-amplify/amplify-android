/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.helpers

import com.amplifyframework.auth.AuthProvider

internal val AuthProvider.userPoolProviderName: String
    get() {
        return when (this) {
            AuthProvider.amazon() -> "LoginWithAmazon"
            AuthProvider.facebook() -> "Facebook"
            AuthProvider.google() -> "Google"
            AuthProvider.apple() -> "SignInWithApple"
            else -> providerKey
        }
    }

internal val AuthProvider.identityProviderName: String
    get() {
        return when (this) {
            AuthProvider.amazon() -> "www.amazon.com"
            AuthProvider.facebook() -> "graph.facebook.com"
            AuthProvider.google() -> "accounts.google.com"
            AuthProvider.apple() -> "appleid.apple.com"
            else -> providerKey
        }
    }
