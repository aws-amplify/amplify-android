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

import aws.sdk.kotlin.runtime.auth.credentials.Credentials
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthException.SignedOutException
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.cognito.helpers.SessionHelper
import com.amplifyframework.auth.result.AuthSessionResult
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens

/**
 * Cognito extension of AuthSession containing AWS Cognito specific tokens.
 */
data class AWSCognitoAuthSession(
    /**
     * Are you currently in a signed in state (an AuthN indicator to be technical)
     */
    val signedIn: Boolean,

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
) : AuthSession(signedIn) {
    companion object {
        fun fromAmplifyCredential(credentials: AmplifyCredential?): AWSCognitoAuthSession {
            // TODO: fix empty credentials
            val awsCredentials = Credentials(
                accessKeyId = credentials?.awsCredentials?.accessKeyId ?: "",
                secretAccessKey = credentials?.awsCredentials?.secretAccessKey ?: "",
                sessionToken = credentials?.awsCredentials?.sessionToken,
                expiration = Instant.fromEpochSeconds(credentials?.awsCredentials?.expiration ?: 0)
            )

            val isSignedIn = credentials?.cognitoUserPoolTokens?.accessToken != null
            val userSubResult: AuthSessionResult<String>
            val userPoolTokensResult: AuthSessionResult<AWSCognitoUserPoolTokens>
            val identityIdResult: AuthSessionResult<String>
            val awsCredentialsResult: AuthSessionResult<Credentials>
            if (isSignedIn) {
                val userPoolTokens = AWSCognitoUserPoolTokens(
                    accessToken = credentials?.cognitoUserPoolTokens?.accessToken ?: "",
                    idToken = credentials?.cognitoUserPoolTokens?.idToken ?: "",
                    refreshToken = credentials?.cognitoUserPoolTokens?.refreshToken ?: ""
                )
                userSubResult = getUserSub(credentials?.cognitoUserPoolTokens)
                userPoolTokensResult = AuthSessionResult.success(userPoolTokens)
                if (credentials?.awsCredentials != null) {
                    identityIdResult = getIdentityId(credentials.identityId)
                    awsCredentialsResult = AuthSessionResult.success(awsCredentials)
                } else {
                    val error = AuthException("Could not fetch AWS Cognito credentials", "This is a bug")
                    identityIdResult = AuthSessionResult.failure(error)
                    awsCredentialsResult = AuthSessionResult.failure(error)
                }
            } else {
                userSubResult = AuthSessionResult.failure(SignedOutException())
                userPoolTokensResult = AuthSessionResult.failure(SignedOutException())
                if (credentials?.awsCredentials != null) {
                    identityIdResult = getIdentityId(credentials.identityId)
                    awsCredentialsResult = AuthSessionResult.success(awsCredentials)
                } else {
                    identityIdResult =
                        AuthSessionResult.failure(SignedOutException(AuthException.GuestAccess.GUEST_ACCESS_POSSIBLE))
                    awsCredentialsResult =
                        AuthSessionResult.failure(SignedOutException(AuthException.GuestAccess.GUEST_ACCESS_POSSIBLE))
                }
            }

            return AWSCognitoAuthSession(
                signedIn = isSignedIn,
                identityId = identityIdResult,
                awsCredentials = awsCredentialsResult,
                userSub = userSubResult,
                userPoolTokens = userPoolTokensResult
            )
        }

        private fun getIdentityId(identityId: String?): AuthSessionResult<String> {
            return if (identityId != null) AuthSessionResult.success(identityId) else AuthSessionResult.failure(
                AuthException(
                    "Failed to retrieve Identity ID",
                    "This should never happen. See the attached exception for more details."
                )
            )
        }

        private fun getUserSub(userPoolTokens: CognitoUserPoolTokens?): AuthSessionResult<String> {
            return try {
                AuthSessionResult.success(userPoolTokens?.accessToken?.let(SessionHelper::getUserSub))
            } catch (e: Exception) {
                AuthSessionResult.failure(AuthException.UnknownException(e))
            }
        }

        fun getUsername(userPoolTokens: CognitoUserPoolTokens): String? {
            return userPoolTokens.accessToken?.let(SessionHelper::getUsername)
        }
    }
}
