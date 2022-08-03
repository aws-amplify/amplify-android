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

package com.amplifyframework.statemachine.codegen.data

import com.amplifyframework.auth.AuthProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class AmplifyCredential {
    @Serializable @SerialName("empty") object Empty : AmplifyCredential()
    data class UserPool(val tokens: CognitoUserPoolTokens) : AmplifyCredential()
    data class IdentityPool(val identityId: String, val credentials: AWSCredentials) : AmplifyCredential()
    data class IdentityPoolFederated(
        val federatedToken: FederatedToken,
        val identityId: String,
        val credentials: AWSCredentials
    ) : AmplifyCredential()

    data class UserAndIdentityPool(
        val tokens: CognitoUserPoolTokens,
        val identityId: String,
        val credentials: AWSCredentials
    ) : AmplifyCredential()
}

data class FederatedToken(val token: String, val provider: AuthProvider)

@Serializable
data class CognitoUserPoolTokens(
    /**
     * User Pool id token
     */
    val idToken: String?,

    /**
     * User Pool access token
     */
    val accessToken: String?,

    /**
     * User Pool refresh token
     */
    val refreshToken: String?,

    /**
     * Auth result expiration but not token expiration
     */
    val expiration: Long?,
) {
    override fun toString(): String {
        return "CognitoUserPoolTokens(" +
                "idToken = ${idToken?.substring(0..4)}***, " +
                "accessToken = ${accessToken?.substring(0..4)}***, " +
                "refreshToken = ${refreshToken?.substring(0..4)}***" +
                ")"
    }
}

@Serializable
data class AWSCredentials(
    val accessKeyId: String?,
    val secretAccessKey: String?,
    val sessionToken: String?,
    val expiration: Long?,
) {
    override fun toString(): String {
        return "AWSCredentials(" +
                "accessKeyId = ${accessKeyId?.substring(0..4)}***, " +
                "secretAccessKey = ${secretAccessKey?.substring(0..4)}***, " +
                "sessionToken = ${sessionToken?.substring(0..4)}***, " +
                "expiration = $expiration" +
                ")"
    }
}
