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
import org.json.JSONObject

/**
 * Configuration options for specifying cognito identity pool.
 */
@InternalAmplifyApi
data class IdentityPoolConfiguration internal constructor(
    val region: String?,
    val poolId: String?
) {
    internal fun toGen1Json() = JSONObject().apply {
        region?.let { put(Config.REGION.key, it) }
        poolId?.let { put(Config.POOL_ID.key, it) }
    }

    internal companion object {
        private const val DEFAULT_REGION = "us-east-1"

        /**
         * Returns a builder object for identity pool configuration.
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
     * Builder class for constructing [IdentityPoolConfiguration].
     */
    internal class Builder(
        configJson: JSONObject? = null
    ) {
        var region: String? = DEFAULT_REGION
        var poolId: String? = null

        init {
            configJson?.run {
                region = optString(Config.REGION.key).takeUnless { it.isNullOrEmpty() }
                poolId = optString(Config.POOL_ID.key).takeUnless { it.isNullOrEmpty() }
            }
        }

        fun region(region: String) = apply { this.region = region }
        fun poolId(poolId: String) = apply { this.poolId = poolId }
        fun build() = IdentityPoolConfiguration(
            region = region,
            poolId = poolId
        )
    }

    private enum class Config(val key: String) {
        /**
         * Amazon Cognito Service endpoint region.
         */
        REGION("Region"),

        /**
         * Contains identity pool identifier.
         */
        POOL_ID("PoolId")
    }
}
