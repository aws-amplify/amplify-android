package com.amplifyframework.auth.cognito.helpers

import com.amplifyframework.auth.AWSCredentials
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.cognito.AWSCognitoUserPoolTokens
import com.amplifyframework.auth.result.AuthSessionResult

/**
 * Helper class that allows Amplify Flutter dev team to create necessary return types with internal constructors.
 * This class should not be consumed outside of the Amplify Flutter development team and should not be considered
 * a stable public API.
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