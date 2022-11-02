package com.amplifyframework.auth.cognito.helpers

import com.amplifyframework.auth.AWSCredentials
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.cognito.AWSCognitoUserPoolTokens
import com.amplifyframework.auth.result.AuthSessionResult

/**
 * Although this has public access, it is intended for internal use and should not be used directly by host
 * applications. The behavior of this may change without warning.
 */
object FlutterFactory {

    fun createAWSCognitoUserPoolTokens(
        accessToken: String?,
        idToken: String?,
        refreshToken: String?
    ) = AWSCognitoUserPoolTokens(accessToken, idToken, refreshToken)

    fun createAWSCognitoAuthSession(
        isSignedIn: Boolean,
        identityIdResult: AuthSessionResult<String>,
        awsCredentialsResult: AuthSessionResult<AWSCredentials>,
        userSubResult: AuthSessionResult<String>,
        userPoolTokensResult: AuthSessionResult<AWSCognitoUserPoolTokens>
    ) = AWSCognitoAuthSession(
        isSignedIn,
        identityIdResult,
        awsCredentialsResult,
        userSubResult,
        userPoolTokensResult
    )
}
