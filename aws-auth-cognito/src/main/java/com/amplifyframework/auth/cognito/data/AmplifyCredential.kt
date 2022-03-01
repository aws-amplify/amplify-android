package com.amplifyframework.auth.cognito.data

import kotlinx.serialization.Serializable

@Serializable
data class AmplifyCredential(
    val cognitoUserPoolTokens: CognitoUserPoolTokens?,
    val identityId: String?,
    val awsCredentials: AWSCredentials?,
)

@Serializable
data class CognitoUserPoolTokens(
    val idToken: String?,
    val accessToken: String?,
    val refreshToken: String?,
    val tokenExpiration: String?,
)

@Serializable
data class AWSCredentials(
    val accessKeyId: String?,
    val secretAccessKey: String?,
    val sessionToken: String?,
    val expiration: String?,
)