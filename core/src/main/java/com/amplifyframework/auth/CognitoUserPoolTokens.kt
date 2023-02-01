package com.amplifyframework.auth

interface CognitoUserPoolTokens {
    val idToken: String?
    val accessToken: String?
    val refreshToken: String?
    val expiration: Long?
}
