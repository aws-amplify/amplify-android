package com.amplifyframework.auth

import com.amplifyframework.auth.cognito.helpers.JWTParser
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.result.AuthSessionResult

interface AWSCognitoAuthSession {
    val isSignedIn: Boolean
    val identityIdResult: AuthSessionResult<String>
    val awsCredentialsResult: AuthSessionResult<AWSCredentials>
    val userSubResult: AuthSessionResult<String>
    val userPoolTokensResult: AuthSessionResult<AWSCognitoUserPoolTokens>

    companion object {
        fun getCredentialsResult(awsCredentials: CognitoCredentials): AuthSessionResult<AWSCredentials> =
            with(awsCredentials) {
                AWSCredentials.createAWSCredentials(accessKeyId, secretAccessKey, sessionToken, expiration)
            }?.let {
                AuthSessionResult.success(it)
            } ?: AuthSessionResult.failure(UnknownException("Failed to fetch AWS credentials."))

        fun getIdentityIdResult(identityId: String): AuthSessionResult<String> {
            return if (identityId.isNotEmpty()) AuthSessionResult.success(identityId)
            else AuthSessionResult.failure(UnknownException("Failed to fetch identity id."))
        }

        fun getUserSubResult(userPoolTokens: CognitoUserPoolTokens?): AuthSessionResult<String> {
            return try {
                AuthSessionResult.success(userPoolTokens?.accessToken?.let { JWTParser.getClaim(it, "sub") })
            } catch (e: Exception) {
                AuthSessionResult.failure(UnknownException(cause = e))
            }
        }

        fun getUserPoolTokensResult(cognitoUserPoolTokens: CognitoUserPoolTokens):
            AuthSessionResult<AWSCognitoUserPoolTokens> {
            return AuthSessionResult.success(
                AWSCognitoUserPoolTokens(
                    accessToken = cognitoUserPoolTokens.accessToken,
                    idToken = cognitoUserPoolTokens.idToken,
                    refreshToken = cognitoUserPoolTokens.refreshToken
                )
            )
        }
    }
}
