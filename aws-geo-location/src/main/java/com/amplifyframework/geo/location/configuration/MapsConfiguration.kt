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

import com.amplifyframework.geo.models.MapStyle
import org.json.JSONObject

/**
 * Configuration options for specifying available map resources.
 */
data class MapsConfiguration internal constructor(
    val items: Collection<MapStyle>,
    val default: MapStyle
) {
    companion object {
        /**
         * Returns a builder object for maps configuration.
         * @return fresh configuration builder instance.
         */
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
    }

    /**
     * Builder class for constructing [MapsConfiguration].
     */
    data class Builder internal constructor(
        private val configJson: JSONObject? = null
    ) {
        private var items: Collection<MapStyle> = emptySet()
        private var default: MapStyle? = null

        init {
            configJson?.run {
                val mapItems = getJSONObject(Config.ITEMS.key)
                mapItems.let {
                    val maps = HashSet<MapStyle>()
                    for (mapName in it.keys()) {
                        val style = it.getJSONObject(mapName)
                            .getString(Config.STYLE.key)
                        maps.add(MapStyle(mapName, style))
                    }
                    items = maps
                }

                val defaultMap = optString(Config.DEFAULT.key)
                if (!defaultMap.isNullOrEmpty()) {
                    default = items.firstOrNull { it.mapName == defaultMap }
                }
            }
        }

        fun items(items: Collection<MapStyle>) = apply { this.items = items }
        fun default(default: MapStyle) = apply { this.default = default }
        fun build() = MapsConfiguration(items, default ?: items.first())
    }

    private enum class Config(val key: String) {
        /**
         * Contains a collection of maps and their corresponding style.
         */
        ITEMS("items"),

        /**
         * Contains name of the default map.
         */
        DEFAULT("default"),

        /**
         * Contains style name for a given map.
         */
        STYLE("style")
    }
}