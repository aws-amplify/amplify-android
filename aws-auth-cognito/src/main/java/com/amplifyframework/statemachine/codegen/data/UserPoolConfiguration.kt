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

import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.auth.AuthException
import org.json.JSONObject

/**
 * Configuration options for specifying cognito user pool.
 */
@InternalAmplifyApi
data class UserPoolConfiguration internal constructor(
    val region: String?,
    val endpoint: String?,
    val poolId: String?,
    val appClient: String?,
    val appClientSecret: String?,
    val pinpointAppId: String?
) {

    internal fun toGen1Json() = JSONObject().apply {
        region?.let { put(Config.REGION.key, it) }
        endpoint?.let { put(Config.ENDPOINT.key, it) }
        poolId?.let { put(Config.POOL_ID.key, it) }
        appClient?.let { put(Config.APP_CLIENT_ID.key, it) }
        appClientSecret?.let { put(Config.APP_CLIENT_SECRET.key, it) }
        pinpointAppId?.let { put(Config.PINPOINT_APP_ID.key, it) }
    }

    internal companion object {
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
        fun fromJson(configJson: JSONObject): Builder {
            return Builder(configJson)
        }

        inline operator fun invoke(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    /**
     * Builder class for constructing [UserPoolConfiguration].
     */
    internal class Builder constructor(
        configJson: JSONObject? = null
    ) {
        var region: String? = DEFAULT_REGION
        var endpoint: String? = null
        var poolId: String? = null
        var appClientId: String? = null
        var appClientSecret: String? = null
        var pinpointAppId: String? = null

        init {
            configJson?.run {
                region = optString(Config.REGION.key).takeUnless { it.isNullOrEmpty() }
                endpoint = validateEndpoint(optString(Config.ENDPOINT.key).takeUnless { it.isNullOrEmpty() })
                poolId = optString(Config.POOL_ID.key).takeUnless { it.isNullOrEmpty() }
                appClientId = optString(Config.APP_CLIENT_ID.key).takeUnless { it.isNullOrEmpty() }
                appClientSecret = optString(Config.APP_CLIENT_SECRET.key).takeUnless { it.isNullOrEmpty() }
                pinpointAppId = optString(Config.PINPOINT_APP_ID.key).takeUnless { it.isNullOrEmpty() }
            }
        }

        fun region(region: String) = apply { this.region = region }
        fun endpoint(endpoint: String) = apply { this.endpoint = validateEndpoint(endpoint) }
        fun poolId(poolId: String) = apply { this.poolId = poolId }
        fun appClientId(appClientId: String) = apply { this.appClientId = appClientId }
        fun appClientSecret(appClientSecret: String) = apply { this.appClientSecret = appClientSecret }
        fun pinpointAppId(pinpointAppId: String) = apply { this.pinpointAppId = pinpointAppId }
        fun build() = UserPoolConfiguration(
            region = region,
            endpoint = endpoint,
            poolId = poolId,
            appClient = appClientId,
            appClientSecret = appClientSecret,
            pinpointAppId = pinpointAppId
        )

        @Throws(AuthException::class)
        private fun validateEndpoint(endpoint: String?): String? {
            try {
                endpoint?.let {
                    // regex to match valid host url only with no scheme, no path, and no query
                    val regex = Regex(
                        "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9]" +
                            "[A-Za-z0-9\\-]*[A-Za-z0-9])\$"
                    )
                    if (!regex.matches(it)) {
                        throw Exception("Invalid endpoint")
                    }
                }
                return endpoint?.let {
                    "https://$endpoint"
                }
            } catch (e: Exception) {
                throw Exception(
                    "Invalid endpoint value $endpoint. Expected fully qualified hostname with no scheme, " +
                        "no path and no query"
                )
            }
        }
    }

    private enum class Config(val key: String) {
        /**
         * Amazon Cognito Service endpoint region.
         */
        REGION("Region"),

        /**
         * Contains user pool endpoint host.
         */
        ENDPOINT("Endpoint"),

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

        /**
         * Contains pinpoint app id to link auth events to Pinpoint.
         */
        PINPOINT_APP_ID("PinpointAppId")
    }
}
