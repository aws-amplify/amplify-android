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

package com.amplifyframework.auth.cognito

import com.amplifyframework.auth.AWSCredentials
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.cognito.helpers.SessionHelper
import com.amplifyframework.auth.exceptions.ConfigurationException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.result.AuthSessionResult
import com.amplifyframework.statemachine.codegen.data.AWSCredentials as CognitoCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens

/**
 * Cognito extension of AuthSession containing AWS Cognito specific tokens.
 *
 * @param isSignedIn Are you currently in a signed in state (an AuthN indicator to be technical)
 * @param identityIdResult The id which comes from Identity Pools.
 * @param awsCredentialsResult The credentials which come from Identity Pool.
 * @param userSubResult The id which comes from User Pools.
 * @param userPoolTokensResult The tokens which come from User Pools (access, id, refresh tokens).
 */
data class AWSCognitoAuthSession(
    @get:JvmName("getSignedIn")
    val isSignedIn: Boolean,
    val identityIdResult: AuthSessionResult<String>,
    val awsCredentialsResult: AuthSessionResult<AWSCredentials>,
    val userSubResult: AuthSessionResult<String>,
    val userPoolTokensResult: AuthSessionResult<AWSCognitoUserPoolTokens>
) : AuthSession(isSignedIn) {
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
                AuthSessionResult.success(userPoolTokens?.accessToken?.let(SessionHelper::getUserSub))
            } catch (e: Exception) {
                AuthSessionResult.failure(UnknownException(cause = e))
            }
        }

        fun getUserPoolTokensResult(
            cognitoUserPoolTokens: CognitoUserPoolTokens
        ): AuthSessionResult<AWSCognitoUserPoolTokens> {
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

fun AmplifyCredential.isValid(): Boolean {
    return when (this) {
        is AmplifyCredential.UserPool -> SessionHelper.isValidTokens(signedInData.cognitoUserPoolTokens)
        is AmplifyCredential.IdentityPool -> SessionHelper.isValidSession(credentials)
        is AmplifyCredential.UserAndIdentityPool ->
            SessionHelper.isValidTokens(signedInData.cognitoUserPoolTokens) && SessionHelper.isValidSession(credentials)
        else -> false
    }
}

fun AmplifyCredential.getCognitoSession(exception: AuthException = SignedOutException()): AWSCognitoAuthSession {
    return when (this) {
        is AmplifyCredential.UserPool -> AWSCognitoAuthSession(
            true,
            identityIdResult = AuthSessionResult.failure(
                ConfigurationException(
                    "Could not retrieve Identity ID",
                    "Cognito Identity not configured. Please check amplifyconfiguration.json file."
                )
            ),
            awsCredentialsResult = AuthSessionResult.failure(
                ConfigurationException(
                    "Could not fetch AWS Cognito credentials",
                    "Cognito Identity not configured. Please check amplifyconfiguration.json file."
                )
            ),
            userSubResult = AWSCognitoAuthSession.getUserSubResult(signedInData.cognitoUserPoolTokens),
            userPoolTokensResult = AWSCognitoAuthSession.getUserPoolTokensResult(signedInData.cognitoUserPoolTokens)
        )
        is AmplifyCredential.UserAndIdentityPool -> AWSCognitoAuthSession(
            true,
            identityIdResult = AWSCognitoAuthSession.getIdentityIdResult(identityId),
            awsCredentialsResult = AWSCognitoAuthSession.getCredentialsResult(credentials),
            userSubResult = AWSCognitoAuthSession.getUserSubResult(signedInData.cognitoUserPoolTokens),
            userPoolTokensResult = AWSCognitoAuthSession.getUserPoolTokensResult(signedInData.cognitoUserPoolTokens)
        )
        is AmplifyCredential.IdentityPoolTypeCredential -> AWSCognitoAuthSession(
            false,
            identityIdResult = AWSCognitoAuthSession.getIdentityIdResult(identityId),
            awsCredentialsResult = AWSCognitoAuthSession.getCredentialsResult(credentials),
            userSubResult = AuthSessionResult.failure(SignedOutException()),
            userPoolTokensResult = AuthSessionResult.failure(SignedOutException())
        )
        else -> AWSCognitoAuthSession(
            false,
            identityIdResult = AuthSessionResult.failure(exception),
            awsCredentialsResult = AuthSessionResult.failure(exception),
            userSubResult = AuthSessionResult.failure(exception),
            userPoolTokensResult = AuthSessionResult.failure(exception)
        )
    }
}
