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
 * Configuration options for specifying cognito user pool.
 */
data class UserPoolConfiguration internal constructor(val builder: Builder) {
    val region: String? = builder.region
    val poolId: String? = builder.poolId
    val appClient: String? = builder.appClientId
    val appClientSecret: String? = builder.appClientSecret

    /**
     * Amazon Cognito user pool: cognito-idp.<region>.amazonaws.com/<YOUR_USER_POOL_ID>,
     * for example, cognito-idp.us-east-1.amazonaws.com/us-east-1_123456789.
     */
    val identityProviderName = "cognito-idp.$region.amazonaws.com/$poolId"

    companion object {
        private const val DEFAULT_REGION = "us-east-1"

        /**
         * Returns a builder object for user pool configuration.
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
        internal fun fromJson(configJson: JSONObject): Builder {
            return Builder(configJson)
        }

        inline operator fun invoke(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    /**
     * Builder class for constructing [UserPoolConfiguration].
     */
    class Builder constructor(
        configJson: JSONObject? = null
    ) {
        var region: String? = DEFAULT_REGION
        var poolId: String? = null
        var appClientId: String? = null
        var appClientSecret: String? = null

        init {
            configJson?.run {
                region = optString(Config.REGION.key).takeUnless { it.isNullOrEmpty() }
                poolId = optString(Config.POOL_ID.key).takeUnless { it.isNullOrEmpty() }
                appClientId = optString(Config.APP_CLIENT_ID.key).takeUnless { it.isNullOrEmpty() }
                appClientSecret = optString(Config.APP_CLIENT_SECRET.key).takeUnless { it.isNullOrEmpty() }
            }
        }

        fun region(region: String) = apply { this.region = region }
        fun poolId(poolId: String) = apply { this.poolId = poolId }
        fun appClientId(appClientId: String) = apply { this.appClientId = appClientId }
        fun appClientSecret(appClientSecret: String) = apply { this.appClientSecret = appClientSecret }
        fun build() = UserPoolConfiguration(this)
    }

    private enum class Config(val key: String) {
        /**
         * Amazon Cognito Service endpoint region.
         */
        REGION("Region"),

        /**
         * Contains user pool identifier.
         */
        POOL_ID("PoolId"),

        /**
         * Contains user pool app client identifier.
         */
        APP_CLIENT_ID("AppClientId"),

        /**
         * Contains user pool app client secret.
         */
        APP_CLIENT_SECRET("AppClientSecret"),
    }
}

