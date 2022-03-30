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
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.result.AuthSessionResult
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential

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
        fun fromAmplifyCredential(credentials: AmplifyCredential): AWSCognitoAuthSession {
            val userPoolTokens = AWSCognitoUserPoolTokens(
                accessToken = credentials.cognitoUserPoolTokens?.accessToken ?: "",
                idToken = credentials.cognitoUserPoolTokens?.idToken ?: "",
                refreshToken = credentials.cognitoUserPoolTokens?.refreshToken ?: ""
            )
            val awsCredentials = Credentials(
                accessKeyId = credentials.awsCredentials?.accessKeyId ?: "",
                secretAccessKey = credentials.awsCredentials?.secretAccessKey ?: "",
                sessionToken = credentials.awsCredentials?.sessionToken,
                expiration = Instant.fromEpochSeconds(
                    credentials.awsCredentials?.expiration ?: 0
                )
            )
            return AWSCognitoAuthSession(
                signedIn = true,
                identityId = AuthSessionResult.success(credentials.identityId),
                awsCredentials = AuthSessionResult.success(awsCredentials),
                userSub = AuthSessionResult.success("usersub"),
                userPoolTokens = AuthSessionResult.success(userPoolTokens)
            )
        }
    }
}
