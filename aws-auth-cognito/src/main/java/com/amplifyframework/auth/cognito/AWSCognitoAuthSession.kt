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
 */
data class AWSCognitoAuthSession(
    /**
     * Are you currently in a signed in state (an AuthN indicator to be technical)
     */
    @get:JvmName("getSignedIn")
    val isSignedIn: Boolean,

    /**
     * The id which comes from Identity Pools.
     * @return the id which comes from Identity Pools
     */
    val identityIdResult: AuthSessionResult<String>,

    /**
     * The credentials which come from Identity Pool.
     * @return the credentials which come from Identity Pool
     */
    val awsCredentialsResult: AuthSessionResult<AWSCredentials>,

    /**
     * The id which comes from User Pools.
     * @return the id which comes from User Pools
     */
    val userSubResult: AuthSessionResult<String>,

    /**
     * The tokens which come from User Pools (access, id, refresh tokens).
     * @return the tokens which come from User Pools (access, id, refresh tokens)
     */
    val userPoolTokensResult: AuthSessionResult<AWSCognitoUserPoolTokens>
) : AuthSession(isSignedIn) {
    companion object {
        fun getCredentials(awsCredentials: CognitoCredentials): AuthSessionResult<AWSCredentials> =
            with(awsCredentials) {
                AWSCredentials.Factory.createAWSCredentials(accessKeyId, secretAccessKey, sessionToken, expiration)
            }?.let {
                AuthSessionResult.success(it)
            } ?: AuthSessionResult.failure(
                SignedOutException(SignedOutException.RECOVERY_SUGGESTION_GUEST_ACCESS_DISABLED)
            )

        fun getIdentityId(identityId: String?): AuthSessionResult<String> {
            return if (identityId != null) AuthSessionResult.success(identityId)
            else AuthSessionResult.failure(
                SignedOutException(SignedOutException.RECOVERY_SUGGESTION_GUEST_ACCESS_DISABLED)
            )
        }

        fun getUserSub(userPoolTokens: CognitoUserPoolTokens?): AuthSessionResult<String> {
            return try {
                AuthSessionResult.success(userPoolTokens?.accessToken?.let(SessionHelper::getUserSub))
            } catch (e: Exception) {
                AuthSessionResult.failure(UnknownException(cause = e))
            }
        }

        fun getUserPoolTokens(cognitoUserPoolTokens: CognitoUserPoolTokens) = AWSCognitoUserPoolTokens(
            accessToken = cognitoUserPoolTokens.accessToken,
            idToken = cognitoUserPoolTokens.idToken,
            refreshToken = cognitoUserPoolTokens.refreshToken
        )
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

fun AmplifyCredential.getCognitoSession(): AWSCognitoAuthSession {
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
            userSubResult = AWSCognitoAuthSession.getUserSub(signedInData.cognitoUserPoolTokens),
            userPoolTokensResult = AuthSessionResult.success(
                AWSCognitoAuthSession.getUserPoolTokens(signedInData.cognitoUserPoolTokens)
            )
        )
        is AmplifyCredential.UserAndIdentityPool -> AWSCognitoAuthSession(
            true,
            identityIdResult = AuthSessionResult.success(identityId),
            awsCredentialsResult = AWSCognitoAuthSession.getCredentials(credentials),
            userSubResult = AWSCognitoAuthSession.getUserSub(signedInData.cognitoUserPoolTokens),
            userPoolTokensResult = AuthSessionResult.success(
                AWSCognitoAuthSession.getUserPoolTokens(signedInData.cognitoUserPoolTokens)
            )
        )
        is AmplifyCredential.IdentityPoolTypeCredential -> AWSCognitoAuthSession(
            false,
            identityIdResult = AWSCognitoAuthSession.getIdentityId(identityId),
            awsCredentialsResult = AWSCognitoAuthSession.getCredentials(credentials),
            userSubResult = AuthSessionResult.failure(SignedOutException()),
            userPoolTokensResult = AuthSessionResult.failure(SignedOutException())
        )
        else -> AWSCognitoAuthSession(
            false,
            identityIdResult = AuthSessionResult.failure(
                SignedOutException(
                    recoverySuggestion = SignedOutException.RECOVERY_SUGGESTION_GUEST_ACCESS_POSSIBLE
                )
            ),
            awsCredentialsResult = AuthSessionResult.failure(
                SignedOutException(
                    recoverySuggestion = SignedOutException.RECOVERY_SUGGESTION_GUEST_ACCESS_POSSIBLE
                )
            ),
            userSubResult = AuthSessionResult.failure(SignedOutException()),
            userPoolTokensResult = AuthSessionResult.failure(SignedOutException())
        )
    }
}
