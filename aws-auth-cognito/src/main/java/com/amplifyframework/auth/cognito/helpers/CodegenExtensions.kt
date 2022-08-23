package com.amplifyframework.auth.cognito.helpers

import com.amplifyframework.auth.AuthProvider

val AuthProvider.userPoolProviderName: String
    get() {
        return when (this) {
            AuthProvider.amazon() -> "LoginWithAmazon"
            AuthProvider.facebook() -> "Facebook"
            AuthProvider.google() -> "Google"
            AuthProvider.apple() -> "SignInWithApple"
            else -> providerKey
        }
    }
