/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import androidx.annotation.IntRange
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.exceptions.ConfigurationException
import com.amplifyframework.core.configuration.AmplifyOutputsData
import com.amplifyframework.statemachine.codegen.data.IdentityPoolConfiguration
import com.amplifyframework.statemachine.codegen.data.OauthConfiguration
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import org.json.JSONArray
import org.json.JSONObject

@InternalAmplifyApi
enum class UsernameAttribute {
    Username,
    Email,
    PhoneNumber
}

@InternalAmplifyApi
enum class VerificationMechanism {
    Email,
    PhoneNumber
}

@InternalAmplifyApi
data class PasswordProtectionSettings(
    @IntRange(from = 6, to = 99) val length: Int,
    val requiresNumber: Boolean,
    val requiresSpecial: Boolean,
    val requiresUpper: Boolean,
    val requiresLower: Boolean
)

/**
 * Configuration options for [AWSCognitoAuthPlugin].
 */
@InternalAmplifyApi
data class AuthConfiguration internal constructor(
    val userPool: UserPoolConfiguration?,
    val identityPool: IdentityPoolConfiguration?,
    val oauth: OauthConfiguration?,
    val authFlowType: AuthFlowType,
    val signUpAttributes: List<AuthUserAttributeKey>,
    val usernameAttributes: List<UsernameAttribute>,
    val verificationMechanisms: List<VerificationMechanism>,
    val passwordProtectionSettings: PasswordProtectionSettings?
) {

    internal companion object {
        /**
         * Returns an AuthConfiguration instance from JSON
         * @return populated AuthConfiguration instance.
         */
        fun fromJson(
            pluginJson: JSONObject,
            configName: String = "Default"
        ): AuthConfiguration {
            val authConfig = pluginJson.optJSONObject("Auth")?.optJSONObject(configName)

            val signUpAttributes = authConfig?.optJSONArray("signupAttributes")?.map {
                AuthUserAttributeKey.custom(getString(it).lowercase())
            } ?: emptyList()

            val usernameAttributes = authConfig?.optJSONArray("usernameAttributes")?.map {
                when (getString(it)) {
                    "EMAIL" -> UsernameAttribute.Email
                    "PHONE_NUMBER" -> UsernameAttribute.PhoneNumber
                    else -> UsernameAttribute.Username
                }
            } ?: emptyList()

            val verificationMechanisms = authConfig?.optJSONArray("verificationMechanisms")?.map {
                when (getString(it)) {
                    "EMAIL" -> VerificationMechanism.Email
                    else -> VerificationMechanism.PhoneNumber
                }
            } ?: emptyList()

            return AuthConfiguration(
                userPool = pluginJson.optJSONObject("CognitoUserPool")?.getJSONObject(configName)?.let {
                    UserPoolConfiguration.fromJson(it).build()
                },
                identityPool = pluginJson.optJSONObject("CredentialsProvider")
                    ?.getJSONObject("CognitoIdentity")
                    ?.getJSONObject(configName)?.let {
                        IdentityPoolConfiguration.fromJson(it).build()
                    },
                oauth = authConfig?.optJSONObject("OAuth")?.let { OauthConfiguration.fromJson(it) },
                authFlowType = getAuthenticationFlowType(authConfig?.optString("authenticationFlowType")),
                signUpAttributes = signUpAttributes,
                usernameAttributes = usernameAttributes,
                verificationMechanisms = verificationMechanisms,
                passwordProtectionSettings = getPasswordProtectionSettings(authConfig)
            )
        }

        fun from(amplifyOutputs: AmplifyOutputsData): AuthConfiguration {
            val auth = amplifyOutputs.auth ?: throw ConfigurationException(
                "Missing Auth configuration",
                "Ensure the auth category is properly configured"
            )

            val oauth = auth.oauth?.let {
                OauthConfiguration(
                    appClient = auth.userPoolClientId,
                    appSecret = null, // Not supported in Gen2
                    domain = it.domain,
                    scopes = it.scopes.toSet(),
                    // Note: Gen2 config gives an array for these values, while Gen1 is just a String. In Gen1
                    // if you specify multiple URIs the CLI will join them to a comma-delimited string in the json.
                    // We are matching that behaviour here for Gen2.
                    signInRedirectURI = it.redirectSignInUri.joinToString(","),
                    signOutRedirectURI = it.redirectSignOutUri.joinToString(",")
                )
            }

            val identityPool = auth.identityPoolId?.let {
                IdentityPoolConfiguration(region = auth.awsRegion, poolId = it)
            }

            return AuthConfiguration(
                userPool = UserPoolConfiguration(
                    region = auth.awsRegion,
                    endpoint = null, // Not supported in Gen2
                    poolId = auth.userPoolId,
                    appClient = auth.userPoolClientId,
                    appClientSecret = null, // Not supported in Gen2
                    pinpointAppId = null // Not supported in Gen2
                ),
                identityPool = identityPool,
                oauth = oauth,
                authFlowType = auth.authenticationFlowType.toConfigType(),
                signUpAttributes = auth.standardRequiredAttributes,
                usernameAttributes = auth.usernameAttributes.map { it.toConfigType() },
                verificationMechanisms = auth.userVerificationTypes.map { it.toConfigType() },
                passwordProtectionSettings = auth.passwordPolicy?.toConfigType()
            )
        }

        private fun getAuthenticationFlowType(authType: String?): AuthFlowType {
            return if (!authType.isNullOrEmpty() && AuthFlowType.values().any { it.name == authType }) {
                AuthFlowType.valueOf(authType)
            } else {
                AuthFlowType.USER_SRP_AUTH
            }
        }

        private fun getPasswordProtectionSettings(authConfig: JSONObject?): PasswordProtectionSettings? {
            val passwordSettings = authConfig?.optJSONObject("passwordProtectionSettings") ?: return null
            val passwordLength = passwordSettings.optInt("passwordPolicyMinLength")
            val passwordRequirements = passwordSettings.optJSONArray("passwordPolicyCharacters")?.map {
                getString(it)
            } ?: emptyList()
            return PasswordProtectionSettings(
                length = passwordLength,
                requiresNumber = passwordRequirements.contains("REQUIRES_NUMBERS"),
                requiresSpecial = passwordRequirements.contains("REQUIRES_SYMBOLS"),
                requiresLower = passwordRequirements.contains("REQUIRES_LOWER"),
                requiresUpper = passwordRequirements.contains("REQUIRES_UPPER")
            )
        }

        private inline fun <T> JSONArray.map(func: JSONArray.(Int) -> T) = List(length()) {
            func(it)
        }

        private fun AmplifyOutputsData.Auth.AuthenticationFlowType.toConfigType() = when (this) {
            AmplifyOutputsData.Auth.AuthenticationFlowType.USER_SRP_AUTH -> AuthFlowType.USER_SRP_AUTH
            AmplifyOutputsData.Auth.AuthenticationFlowType.CUSTOM_AUTH -> AuthFlowType.CUSTOM_AUTH
        }

        private fun AmplifyOutputsData.Auth.UsernameAttributes.toConfigType() = when (this) {
            AmplifyOutputsData.Auth.UsernameAttributes.EMAIL -> UsernameAttribute.Email
            AmplifyOutputsData.Auth.UsernameAttributes.PHONE -> UsernameAttribute.PhoneNumber
            AmplifyOutputsData.Auth.UsernameAttributes.USERNAME -> UsernameAttribute.Username
        }

        private fun AmplifyOutputsData.Auth.UserVerificationTypes.toConfigType() = when (this) {
            AmplifyOutputsData.Auth.UserVerificationTypes.EMAIL -> VerificationMechanism.Email
            AmplifyOutputsData.Auth.UserVerificationTypes.PHONE -> VerificationMechanism.PhoneNumber
        }

        private fun AmplifyOutputsData.Auth.PasswordPolicy.toConfigType() = PasswordProtectionSettings(
            length = minLength ?: 6,
            requiresNumber = requireNumbers ?: false,
            requiresSpecial = requireSymbols ?: false,
            requiresUpper = requireUppercase ?: false,
            requiresLower = requireLowercase ?: false
        )
    }
}
