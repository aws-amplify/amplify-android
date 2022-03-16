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
                UserPoolData("userPoolPoolId", "userPoolRegion", "userPoolAppClientId", "AppClientSecret")
            )
        )
    }

    internal fun getAuthConfiguration() : AuthConfiguration {
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
internal data class CredentialsProvider(@SerializedName("CognitoIdentity") val cognitoIdentity: CognitoIdentity)

@Serializable
internal data class CognitoUserPool(@SerializedName("Default") val userPool: UserPoolData)

@Serializable
internal data class CognitoIdentity(@SerializedName("Default") val identityData: CognitoIdentityData)

@Serializable
internal data class UserPoolData(val PoolId: String, val Region: String, val AppClientId: String, val AppClientSecret: String)

@Serializable
internal data class CognitoIdentityData(val PoolId: String, val Region: String)