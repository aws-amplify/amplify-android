/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.geo.location.configuration

import com.amplifyframework.geo.location.AWSLocationGeoPlugin
import org.json.JSONObject

/**
 * Configuration options for [AWSLocationGeoPlugin].
 */
data class GeoConfiguration internal constructor(
    val region: String,
    val maps: MapsConfiguration?
) {
    companion object {
        private const val DEFAULT_REGION = "us-east-1"

        /**
         * Returns a builder object for plugin configuration.
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
        internal fun fromJson(pluginJson: JSONObject): Builder {
            return Builder(pluginJson)
        }
    }

    /**
     * Builder class for constructing [GeoConfiguration].
     */
    data class Builder internal constructor(
        private val configJson: JSONObject? = null
    ) {
        private var region: String = DEFAULT_REGION
        private var maps: MapsConfiguration? = null

        init {
            configJson?.run {
                val regionString = optString(Config.REGION.key)
                if (!regionString.isNullOrEmpty()) {
                    region = regionString
                }

                optJSONObject(Config.MAPS.key)?.let {
                    maps = MapsConfiguration.fromJson(it).build()
                }
            }
        }

        fun region(region: String) = apply { this.region = region }
        fun maps(maps: MapsConfiguration) = apply { this.maps = maps }
        fun build() = GeoConfiguration(region, maps)
    }

    private enum class Config(val key: String) {
        /**
         * Contains configuration for Maps APIs.
         */
        MAPS("maps"),

        /**
         * Amazon Location Service endpoint region.
         */
        REGION("region")
    }
}