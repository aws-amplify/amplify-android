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
package com.amplifyframework.geo.location

import android.content.Context
import com.amazonaws.services.geo.AmazonLocationClient
import com.amplifyframework.AmplifyException
import com.amplifyframework.geo.GeoCategoryPlugin
import com.amplifyframework.geo.GeoException
import com.amplifyframework.geo.location.configuration.GeoConfiguration
import org.json.JSONObject

/**
 * A plugin for the Geo category to interact with Amazon Location Service.
 */
class AWSLocationGeoPlugin(
    private val userConfiguration: GeoConfiguration? = null // for programmatically overriding amplifyconfiguration.json
): GeoCategoryPlugin<AmazonLocationClient?>() {
    companion object {
        private const val GEO_PLUGIN_KEY = "awsLocationGeoPlugin"
    }

    private lateinit var pluginConfiguration: GeoConfiguration

    override fun getPluginKey(): String {
        return GEO_PLUGIN_KEY
    }

    @Throws(AmplifyException::class)
    override fun configure(pluginConfiguration: JSONObject, context: Context) {
        try {
            this.pluginConfiguration = userConfiguration ?: GeoConfiguration.fromJson(pluginConfiguration).build()
        } catch (error: Exception) {
            throw GeoException("Failed to configure AWSLocationGeoPlugin.", error,
                "Make sure your amplifyconfiguration.json is valid.")
        }
    }

    override fun getEscapeHatch(): AmazonLocationClient? {
        return null
    }

    override fun getVersion(): String {
        return BuildConfig.VERSION_NAME
    }
}