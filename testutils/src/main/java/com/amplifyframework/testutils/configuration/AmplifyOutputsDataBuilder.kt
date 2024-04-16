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

package com.amplifyframework.testutils.configuration

import com.amplifyframework.core.configuration.AmplifyOutputsData
import kotlinx.serialization.json.JsonObject

fun amplifyOutputsData(func: AmplifyOutputsDataBuilder.() -> Unit): AmplifyOutputsData =
    AmplifyOutputsDataBuilder().apply(func)

class AmplifyOutputsDataBuilder : AmplifyOutputsData {
    override var version = "1"
    override var analytics: AmplifyOutputsData.Analytics? = null
    override var auth: AmplifyOutputsData.Auth? = null
    override var data: AmplifyOutputsData.Data? = null
    override var geo: AmplifyOutputsData.Geo? = null
    override var notifications: AmplifyOutputsData.Notifications? = null
    override var storage: AmplifyOutputsData.Storage? = null
    override var custom: JsonObject? = null

    fun analytics(func: AnalyticsBuilder.() -> Unit) {
        analytics = AnalyticsBuilder().apply(func)
    }

    fun geo(func: GeoBuilder.() -> Unit) {
        geo = GeoBuilder().apply(func)
    }

    fun notifications(func: NotificationsBuilder.() -> Unit) {
        notifications = NotificationsBuilder().apply(func)
    }

    fun storage(func: StorageBuilder.() -> Unit) {
        storage = StorageBuilder().apply(func)
    }
}

class AnalyticsBuilder : AmplifyOutputsData.Analytics {
    override var amazonPinpoint: AmplifyOutputsData.Analytics.AmazonPinpoint? = null

    fun amazonPinpoint(func: AmazonPinpointBuilder.() -> Unit) {
        amazonPinpoint = AmazonPinpointBuilder().apply(func)
    }
}

class AmazonPinpointBuilder : AmplifyOutputsData.Analytics.AmazonPinpoint {
    override var awsRegion: String = "us-east-1"
    override var appId: String = "analytics-app-id"
}

class GeoBuilder : AmplifyOutputsData.Geo {
    override var awsRegion: String = "us-east-1"
    override var maps: AmplifyOutputsData.Geo.Maps? = null
    override var searchIndices: AmplifyOutputsData.Geo.SearchIndices? = null
    override var geofenceCollections: AmplifyOutputsData.Geo.GeofenceCollections? = null

    fun maps(func: GeoMapsBuilder.() -> Unit) {
        maps = GeoMapsBuilder().apply(func)
    }

    fun searchIndices(func: GeoSearchIndicesBuilder.() -> Unit) {
        searchIndices = GeoSearchIndicesBuilder().apply(func)
    }
}

class GeoMapsBuilder : AmplifyOutputsData.Geo.Maps {
    override val items: MutableMap<String, AmplifyOutputsData.AmazonLocationServiceConfig> = mutableMapOf()
    override var default: String = ""

    fun map(name: String, style: String) {
        items += name to LocationServiceConfig(style)
    }

    data class LocationServiceConfig(override val style: String) : AmplifyOutputsData.AmazonLocationServiceConfig
}

class GeoSearchIndicesBuilder : AmplifyOutputsData.Geo.SearchIndices {
    override val items: MutableSet<String> = mutableSetOf()
    override var default: String = ""
}

class NotificationsBuilder : AmplifyOutputsData.Notifications {
    override var awsRegion: String = "us-east-1"
    override var amazonPinpointAppId: String = "pinpoint-app-id"
    override val channels: MutableList<AmplifyOutputsData.AmazonPinpointChannels> = mutableListOf(
        AmplifyOutputsData.AmazonPinpointChannels.FCM
    )
}

class StorageBuilder : AmplifyOutputsData.Storage {
    override var awsRegion: String = "us-east-1"
    override var bucketName: String = "bucket-name"
}
