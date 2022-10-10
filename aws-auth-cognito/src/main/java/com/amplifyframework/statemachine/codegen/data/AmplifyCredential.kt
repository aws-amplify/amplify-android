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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class AmplifyCredential {

    interface UserPoolTypeCredential {
        val signedInData: SignedInData
    }

    interface IdentityPoolTypeCredential {
        val identityId: String
        val credentials: AWSCredentials
    }

    interface DeviceMetaDataTypeCredential {
        val deviceMetadata: DeviceMetadata
    }

    @Serializable
    @SerialName("empty")
    object Empty : AmplifyCredential()

    @Serializable
    @SerialName("userPool")
    data class UserPool(override val signedInData: SignedInData) : AmplifyCredential(), UserPoolTypeCredential

    @Serializable
    @SerialName("deviceMetadata")
    data class DeviceData(override val deviceMetadata: DeviceMetadata) :
        AmplifyCredential(), DeviceMetaDataTypeCredential

    @Serializable
    @SerialName("identityPool")
    data class IdentityPool(override val identityId: String, override val credentials: AWSCredentials) :
        AmplifyCredential(), IdentityPoolTypeCredential

    @Serializable
    @SerialName("identityPoolFederated")
    data class IdentityPoolFederated(
        val federatedToken: FederatedToken,
        override val identityId: String,
        override val credentials: AWSCredentials
    ) : AmplifyCredential(), IdentityPoolTypeCredential

    @Serializable
    @SerialName("userAndIdentityPool")
    data class UserAndIdentityPool(
        override val signedInData: SignedInData,
        override val identityId: String,
        override val credentials: AWSCredentials
    ) : AmplifyCredential(), UserPoolTypeCredential, IdentityPoolTypeCredential
}

// TODO: Token abstraction if needed
// @Serializable
// sealed class AuthTokens{
//    data class CognitoUserPoolTokens(
//        val idToken: String?,
//        val accessToken: String?,
//        val refreshToken: String?,
//        val expiration: Long?,
//    ) : AuthTokens()
//    data class FederatedToken(val token: String, val provider: AuthProvider) : AuthTokens()
// }

@Serializable
data class FederatedToken(val token: String, val providerName: String)

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

sealed class CredentialType {
    object Amplify : CredentialType()
    data class Device(val username: String) : CredentialType()
    object ASF : CredentialType()
}
