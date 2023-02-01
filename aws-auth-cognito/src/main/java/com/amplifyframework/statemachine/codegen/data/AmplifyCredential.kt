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

import com.amplifyframework.auth.CognitoCredentials
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed class AmplifyCredential {

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

    @Serializable
    @SerialName("deviceMetadata")
    data class DeviceData(override val deviceMetadata: DeviceMetadata) :
        AmplifyCredential(), DeviceMetaDataTypeCredential

    @Serializable
    @SerialName("asfDevice")
    data class ASFDevice(val id: String?) : AmplifyCredential()
}

/**
 * Contains identity provider info to federate a provider into identity pool
 * @param token identity provider token (Cognito or 3rd party)
 * @param providerName identity provider name
 */
@Serializable
internal data class FederatedToken(val token: String, val providerName: String) {
    override fun toString(): String {
        return "FederatedToken(" +
            "token = ${token.substring(0..4)}***, " +
            "providerName = $providerName" +
            ")"
    }
}

/**
 * Contains cognito user pool JWT tokens
 * @param idToken User Pool id token
 * @param accessToken User Pool access token
 * @param refreshToken User Pool refresh token
 * @param expiration Auth result expiration but not token expiration
 */
@Serializable
data class CognitoUserPoolTokens(
    override val idToken: String?,
    override val accessToken: String?,
    override val refreshToken: String?,
    override val expiration: Long?
) : com.amplifyframework.auth.CognitoUserPoolTokens {
    override fun toString(): String {
        return "CognitoUserPoolTokens(" +
            "idToken = ${idToken?.substring(0..4)}***, " +
            "accessToken = ${accessToken?.substring(0..4)}***, " +
            "refreshToken = ${refreshToken?.substring(0..4)}***" +
            ")"
    }

    override fun equals(other: Any?): Boolean {
        return if (super.equals(other)) {
            true
        } else if (other == null || javaClass != other.javaClass || other !is CognitoUserPoolTokens) {
            false
        } else {
            idToken == other.idToken && accessToken == other.accessToken && refreshToken == other.refreshToken
        }
    }
}

/**
 * Contains AWS credentials that allows access to AWS resources
 * @param accessKeyId access key id
 * @param secretAccessKey secret access key
 * @param sessionToken temporary session token
 * @param expiration session token expiration
 */
@Serializable
class AWSCredentials(
    override val accessKeyId: String?,
    override val secretAccessKey: String?,
    override val sessionToken: String?,
    override val expiration: Long?,
) : CognitoCredentials {
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

internal sealed class CredentialType {
    object Amplify : CredentialType()
    data class Device(val username: String) : CredentialType()
    object ASF : CredentialType()
}
