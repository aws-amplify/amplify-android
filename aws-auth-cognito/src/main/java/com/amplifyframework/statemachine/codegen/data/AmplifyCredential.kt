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

    interface UserPoolData {
        val signedInData: SignedInData
    }

    interface IdentityPoolData {
        val identityId: String
        val credentials: AWSCredentials
    }

    @Serializable
    @SerialName("empty")
    object Empty : AmplifyCredential()

    @Serializable
    @SerialName("userPool")
    data class UserPool(override val signedInData: SignedInData) : AmplifyCredential(), UserPoolData

    @Serializable
    @SerialName("identityPool")
    data class IdentityPool(override val identityId: String, override val credentials: AWSCredentials) :
        AmplifyCredential(), IdentityPoolData

    //    @Serializable
//    @SerialName("identityPoolFederated")
    data class IdentityPoolFederated(
        val federatedToken: FederatedToken,
        val identityId: String,
        val credentials: AWSCredentials
    ) : AmplifyCredential()

    @Serializable
    @SerialName("userAndIdentityPool")
    data class UserAndIdentityPool(
        override val signedInData: SignedInData,
        override val identityId: String,
        override val credentials: AWSCredentials
    ) : AmplifyCredential(), UserPoolData, IdentityPoolData

    fun update(
        identityId: String? = null,
        awsCredentials: AWSCredentials? = null
    ): AmplifyCredential {
        return when {
            identityId != null -> when (this) {
                is UserAndIdentityPool -> copy(identityId = identityId)
                is UserPool -> UserAndIdentityPool(signedInData, identityId, AWSCredentials.empty)
                is IdentityPool -> copy(identityId = identityId)
                else -> IdentityPool(identityId = identityId, AWSCredentials.empty)
            }
            awsCredentials != null -> when (this) {
                is UserAndIdentityPool -> copy(credentials = awsCredentials)
                is IdentityPool -> copy(credentials = awsCredentials)
                else -> Empty
            }
            else -> this
        }
    }
}

// TODO: token abstraction
// sealed class Token{
//    data class CognitoUserPoolTokens(
//        val idToken: String?,
//        val accessToken: String?,
//        val refreshToken: String?,
//        val expiration: Long?,
//    )
//    data class FederatedToken(val token: String, val provider: AuthProvider) : Token()
// }

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

    companion object {
        val empty = AWSCredentials(null, null, null, 0)
    }

    override fun toString(): String {
        return "AWSCredentials(" +
            "accessKeyId = ${accessKeyId?.substring(0..4)}***, " +
            "secretAccessKey = ${secretAccessKey?.substring(0..4)}***, " +
            "sessionToken = ${sessionToken?.substring(0..4)}***, " +
            "expiration = $expiration" +
            ")"
    }
}
