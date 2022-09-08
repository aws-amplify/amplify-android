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

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthException.SignedOutException
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.cognito.helpers.SessionHelper
import com.amplifyframework.auth.result.AuthSessionResult
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
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
    val identityId: AuthSessionResult<String>,

    /**
     * The credentials which come from Identity Pool.
     * @return the credentials which come from Identity Pool
     */
    val awsCredentials: AuthSessionResult<Credentials>,

    /**
     * The id which comes from User Pools.
     * @return the id which comes from User Pools
     */
    val userSub: AuthSessionResult<String>,

    /**
     * The tokens which come from User Pools (access, id, refresh tokens).
     * @return the tokens which come from User Pools (access, id, refresh tokens)
     */
    val userPoolTokens: AuthSessionResult<AWSCognitoUserPoolTokens>
) : AuthSession(isSignedIn) {
    companion object {
        fun getCredentials(awsCredentials: AWSCredentials): AuthSessionResult<Credentials> {
            return if (awsCredentials.accessKeyId != null && awsCredentials.secretAccessKey != null) {
                val expiresIn = awsCredentials.expiration ?: 0
                val credentials = Credentials(
                    accessKeyId = awsCredentials.accessKeyId,
                    secretAccessKey = awsCredentials.secretAccessKey,
                    sessionToken = awsCredentials.sessionToken,
                    expiration = Instant.fromEpochSeconds(expiresIn)
                )
                AuthSessionResult.success(credentials)
            } else AuthSessionResult.failure(SignedOutException(AuthException.GuestAccess.GUEST_ACCESS_DISABLED))
        }

        fun getIdentityId(identityId: String?): AuthSessionResult<String> {
            return if (identityId != null) AuthSessionResult.success(identityId)
            else AuthSessionResult.failure(SignedOutException(AuthException.GuestAccess.GUEST_ACCESS_DISABLED))
        }

        fun getUserSub(userPoolTokens: CognitoUserPoolTokens?): AuthSessionResult<String> {
            return try {
                AuthSessionResult.success(userPoolTokens?.accessToken?.let(SessionHelper::getUserSub))
            } catch (e: Exception) {
                AuthSessionResult.failure(AuthException.UnknownException(e))
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
        is AmplifyCredential.UserPool -> SessionHelper.isValid(tokens)
        is AmplifyCredential.UserAndIdentityPool ->
            SessionHelper.isValid(tokens) &&
                SessionHelper.isValidSession(credentials)
        is AmplifyCredential.IdentityPool -> SessionHelper.isValidSession(credentials)
        else -> false
    }
}

fun AmplifyCredential.getCognitoSession(): AWSCognitoAuthSession {
    return when (this) {
        is AmplifyCredential.UserPool -> AWSCognitoAuthSession(
            true,
            identityId = AuthSessionResult.failure(
                AuthException(
                    "Could not retrieve Identity ID",
                    "Cognito Identity not configured. Please check amplifyconfiguration.json file."
                )
            ),
            awsCredentials = AuthSessionResult.failure(
                AuthException(
                    "Could not fetch AWS Cognito credentials",
                    "Cognito Identity not configured. Please check amplifyconfiguration.json file."
                )
            ),
            userSub = AWSCognitoAuthSession.getUserSub(tokens),
            userPoolTokens = AuthSessionResult.success(AWSCognitoAuthSession.getUserPoolTokens(tokens))
        )
        is AmplifyCredential.IdentityPool -> AWSCognitoAuthSession(
            false,
            identityId = AWSCognitoAuthSession.getIdentityId(identityId),
            awsCredentials = AWSCognitoAuthSession.getCredentials(credentials),
            userSub = AuthSessionResult.failure(SignedOutException()),
            userPoolTokens = AuthSessionResult.failure(SignedOutException())
        )
        is AmplifyCredential.UserAndIdentityPool -> AWSCognitoAuthSession(
            true,
            identityId = AuthSessionResult.success(identityId),
            awsCredentials = AWSCognitoAuthSession.getCredentials(credentials),//??
            userSub = AWSCognitoAuthSession.getUserSub(tokens),
            userPoolTokens = AuthSessionResult.success(AWSCognitoAuthSession.getUserPoolTokens(tokens))
        )
        else -> AWSCognitoAuthSession(
            false,
            identityId = AuthSessionResult.failure(SignedOutException(AuthException.GuestAccess.GUEST_ACCESS_POSSIBLE)),
            awsCredentials = AuthSessionResult.failure(
                SignedOutException(AuthException.GuestAccess.GUEST_ACCESS_POSSIBLE)
            ),
            userSub = AuthSessionResult.failure(SignedOutException()),
            userPoolTokens = AuthSessionResult.failure(SignedOutException())
        )
    }
}
