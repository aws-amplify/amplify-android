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
    }
}
