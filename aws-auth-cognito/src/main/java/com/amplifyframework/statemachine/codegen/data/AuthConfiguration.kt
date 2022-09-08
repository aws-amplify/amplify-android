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
data class AuthConfiguration internal constructor(val builder: Builder) {
    val userPool: UserPoolConfiguration? = builder.userPool
    val identityPool: IdentityPoolConfiguration? = builder.identityPool

    companion object {
        /**
         * Returns a builder object for auth plugin configuration.
         * @return fresh configuration builder instance.
         */
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }

        /**
         * Returns a builder object populated from JSON.
         * @return populated builder instance.
         */
        internal fun fromJson(
            pluginJson: JSONObject,
            configName: String = Config.DEFAULT.key
        ): Builder {
            return Builder(pluginJson, configName)
        }

        inline operator fun invoke(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    /**
     * Builder class for constructing [AuthConfiguration].
     */
    class Builder constructor(
        configJson: JSONObject? = null,
        configName: String = Config.DEFAULT.key
    ) {
        var userPool: UserPoolConfiguration? = null
        var identityPool: IdentityPoolConfiguration? = null

        init {
            configJson?.run {
                optJSONObject(Config.COGNITO_USER_POOL.key)?.getJSONObject(configName)?.let {
                    userPool = UserPoolConfiguration.fromJson(it).build()
                }

                optJSONObject(Config.CREDENTIALS_PROVIDER.key)?.getJSONObject(
                    Config.COGNITO_IDENTITY.key
                )?.getJSONObject(configName)?.let {
                    identityPool = IdentityPoolConfiguration.fromJson(it).build()
                }
            }
        }

        fun userPool(userPool: UserPoolConfiguration) = apply { this.userPool = userPool }
        fun identityPool(identityPool: IdentityPoolConfiguration) = apply { this.identityPool = identityPool }
        fun build() = AuthConfiguration(this)
    }

    private enum class Config(val key: String) {
        /**
         * Contains configuration for User Pool.
         */
        COGNITO_USER_POOL("CognitoUserPool"),

        CREDENTIALS_PROVIDER("CredentialsProvider"),

        /**
         * Contains configuration for Identity Pool.
         */
        COGNITO_IDENTITY("CognitoIdentity"),

        DEFAULT("Default"),
    }
}
