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

import org.json.JSONObject

/**
 * Configuration options for [AWSCognitoAuthPlugin].
 */
data class AuthConfiguration internal constructor(
    val userPool: UserPoolConfiguration?,
    val identityPool: IdentityPoolConfiguration?,
    val oauth: OauthConfiguration?
) {

    companion object {
        /**
         * Returns an AuthConfiguration instance from JSON
         * @return populated AuthConfiguration instance.
         */
        internal fun fromJson(
            pluginJson: JSONObject,
            configName: String = "Default"
        ): AuthConfiguration {
            return AuthConfiguration(
                userPool = pluginJson.optJSONObject("CognitoUserPool")?.getJSONObject(configName)?.let {
                    UserPoolConfiguration.fromJson(it).build()
                },
                identityPool = pluginJson.optJSONObject("CredentialsProvider")
                    ?.getJSONObject("CognitoIdentity")
                    ?.getJSONObject(configName)?.let {
                        IdentityPoolConfiguration.fromJson(it).build()
                    },
                oauth = pluginJson.optJSONObject("Auth")
                    ?.optJSONObject(configName)
                    ?.optJSONObject("OAuth")?.let {
                        OauthConfiguration.fromJson(it)
                    }
            )
        }
    }
}
