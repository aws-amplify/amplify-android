package com.amplifyframework.auth

interface CognitoCredentials {
    val accessKeyId: String?
    val secretAccessKey: String?
    val sessionToken: String?
    val expiration: Long?
}
