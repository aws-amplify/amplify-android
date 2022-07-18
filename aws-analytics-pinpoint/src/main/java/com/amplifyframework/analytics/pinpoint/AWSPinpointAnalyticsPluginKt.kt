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
package com.amplifyframework.analytics.pinpoint

import android.content.Context
import com.amplifyframework.analytics.AnalyticsEventBehavior
import com.amplifyframework.analytics.AnalyticsPlugin
import com.amplifyframework.analytics.AnalyticsProperties
import com.amplifyframework.analytics.UserProfile
import org.json.JSONObject

/**
 * The plugin implementation for Amazon Pinpoint in Analytics category.
 */
class AWSPinpointAnalyticsPluginKt : AnalyticsPlugin<Any>() {

    private val pluginKey = "awsPinpointAnalyticsPlugin"
    private val analyticsConfigKey = "pinpointAnalytics"
    private lateinit var awsPinpointAnalyticsPluginBehavior: AWSPinpointAnalyticsPluginBehavior

    override fun identifyUser(userId: String, profile: UserProfile?) {
        TODO("Not yet implemented")
    }

    override fun disable() {
        TODO("Not yet implemented")
    }

    override fun enable() {
        TODO("Not yet implemented")
    }

    override fun recordEvent(eventName: String) {
        awsPinpointAnalyticsPluginBehavior.recordEvent(eventName)
    }

    override fun recordEvent(analyticsEvent: AnalyticsEventBehavior) {
        awsPinpointAnalyticsPluginBehavior.recordEvent(analyticsEvent)
    }

    override fun registerGlobalProperties(properties: AnalyticsProperties) {
        TODO("Not yet implemented")
    }

    override fun unregisterGlobalProperties(vararg propertyNames: String?) {
        TODO("Not yet implemented")
    }

    override fun flushEvents() {
        TODO("Not yet implemented")
    }

    override fun getPluginKey(): String {
        return pluginKey
    }

    override fun configure(pluginConfiguration: JSONObject?, context: Context) {
        requireNotNull(pluginConfiguration)
        val configBuilder = AWSPinpointAnalyticsPluginConfiguration.builder()
        val pinpointAnalyticsConfigJson = pluginConfiguration.getJSONObject(analyticsConfigKey)
        configBuilder.withAppId(
            pinpointAnalyticsConfigJson.getString(ConfigKey.APP_ID.configurationKey)
        )
        configBuilder.withRegion(
            pinpointAnalyticsConfigJson.getString(ConfigKey.REGION.configurationKey)
        )
        if (pinpointAnalyticsConfigJson.has(ConfigKey.AUTO_FLUSH_INTERVAL.configurationKey)) {
            configBuilder.withAutoFlushEventsInterval(
                pinpointAnalyticsConfigJson.getLong(ConfigKey.AUTO_FLUSH_INTERVAL.configurationKey)
            )
        }
        if (pinpointAnalyticsConfigJson.has(ConfigKey.TRACK_APP_LIFECYCLE_EVENTS.configurationKey)) {
            configBuilder.withTrackAppLifecycleEvents(
                pinpointAnalyticsConfigJson.getBoolean(ConfigKey.TRACK_APP_LIFECYCLE_EVENTS.configurationKey)
            )
        }
        val awsAnalyticsConfig = configBuilder.build()
        val pinpointManager = PinpointManager(
            context,
            awsAnalyticsConfig,
            null // TODO: Provide valid credential provider
        )
        awsPinpointAnalyticsPluginBehavior = AWSPinpointAnalyticsPluginBehavior(pinpointManager.analyticsClient)
    }

    override fun getEscapeHatch(): Any? {
        TODO("Not yet implemented")
    }

    override fun getVersion(): String {
        TODO("Not yet implemented")
    }
}

typealias ConfigKey = AWSPinpointAnalyticsPlugin.PinpointConfigurationKey
