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

package com.amplifyframework.auth.cognito.testutils

import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import org.json.JSONObject

object AuthConfigurationProvider {
    internal fun getAuthConfigurationObject(): Configuration {
        return Configuration(
            CredentialsProvider(
                CognitoIdentity(
                    CognitoIdentityData("identityPoolId", "cognitoIdRegion")
                )
            ),
            CognitoUserPool(
                UserPoolData(
                    "userPoolPoolId",
                    "userPoolRegion",
                    "userPoolAppClientId",
                    "AppClientSecret"
                )
            )
        )
    }

    internal fun getAuthConfiguration(): AuthConfiguration {
        return AuthConfiguration.fromJson(
            JSONObject(Gson().toJson(getAuthConfigurationObject()))
        ).build()
    }
}

// TODO refactor this to use UserPool IdentityPool Configuration

@Serializable
internal data class Configuration(
    @SerializedName("CredentialsProvider") val credentials: CredentialsProvider,
    @SerializedName("CognitoUserPool") val userPool: CognitoUserPool,
)

@Serializable
internal data class CredentialsProvider(
    @SerializedName("CognitoIdentity") val cognitoIdentity: CognitoIdentity
)

@Serializable
internal data class CognitoUserPool(@SerializedName("Default") val userPool: UserPoolData)

@Serializable
internal data class CognitoIdentity(
    @SerializedName("Default") val identityData: CognitoIdentityData
)

@Serializable
internal data class UserPoolData(
    val PoolId: String,
    val Region: String,
    val AppClientId: String,
    val AppClientSecret: String
)

@Serializable
internal data class CognitoIdentityData(val PoolId: String, val Region: String)
