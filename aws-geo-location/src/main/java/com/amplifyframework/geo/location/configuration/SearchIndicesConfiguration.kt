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

import org.json.JSONObject

/**
 * Configuration options for specifying available search indices.
 */
data class SearchIndicesConfiguration internal constructor(
    val items: Collection<String>,
    val default: String
) {
    companion object {
        /**
         * Returns a builder object for maps configuration.
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
    }

    /**
     * Builder class for constructing [SearchIndicesConfiguration].
     */
    data class Builder internal constructor(
        private val configJson: JSONObject? = null
    ) {
        private var items: Collection<String> = emptySet()
        private var default: String? = null

        init {
            configJson?.run {
                val searchIndexItems = getJSONArray(Config.ITEMS.key)
                searchIndexItems.let {
                    val searchIndices = HashSet<String>()
                    for (i in 0 until searchIndexItems.length()) {
                        searchIndices.add(searchIndexItems.getString(i))
                    }
                    items = searchIndices
                }
                val defaultSearchIndex = optString(Config.DEFAULT.key)
                if (!defaultSearchIndex.isNullOrEmpty()) {
                    default = items.firstOrNull()
                }
            }
        }

        fun items(items: Collection<String>) = apply { this.items = items }
        fun default(default: String) = apply { this.default = default }
        fun build() = SearchIndicesConfiguration(items, default ?: items.first())
    }

    private enum class Config(val key: String) {
        /**
         * Contains a collection of maps and their corresponding style.
         */
        ITEMS("items"),

        /**
         * Contains name of the default map.
         */
        DEFAULT("default")
    }
}