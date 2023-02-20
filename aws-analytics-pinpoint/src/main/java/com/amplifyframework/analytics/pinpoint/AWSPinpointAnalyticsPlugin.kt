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

import android.app.Application
import android.content.Context
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import com.amplifyframework.analytics.AnalyticsEventBehavior
import com.amplifyframework.analytics.AnalyticsPlugin
import com.amplifyframework.analytics.AnalyticsProperties
import com.amplifyframework.analytics.UserProfile
import com.amplifyframework.auth.CognitoCredentialsProvider
import org.json.JSONObject

/**
 * The plugin implementation for Amazon Pinpoint in Analytics category.
 */
internal const val AWS_PINPOINT_ANALYTICS_LOG_NAMESPACE = "amplify:aws-pinpoint-analytics:%s"

class AWSPinpointAnalyticsPlugin : AnalyticsPlugin<Any>() {

    private val pluginKey = "awsPinpointAnalyticsPlugin"
    private val analyticsConfigKey = "pinpointAnalytics"
    private lateinit var awsPinpointAnalyticsPluginBehavior: AWSPinpointAnalyticsPluginBehavior
    private lateinit var pinpointManager: PinpointManager

    override fun identifyUser(userId: String, profile: UserProfile?) {
        awsPinpointAnalyticsPluginBehavior.identifyUser(userId, profile)
    }

    override fun disable() {
        awsPinpointAnalyticsPluginBehavior.disable()
    }

    override fun enable() {
        awsPinpointAnalyticsPluginBehavior.enable()
    }

    override fun recordEvent(eventName: String) {
        awsPinpointAnalyticsPluginBehavior.recordEvent(eventName)
    }

    override fun recordEvent(analyticsEvent: AnalyticsEventBehavior) {
        awsPinpointAnalyticsPluginBehavior.recordEvent(analyticsEvent)
    }

    override fun registerGlobalProperties(properties: AnalyticsProperties) {
        awsPinpointAnalyticsPluginBehavior.registerGlobalProperties(properties)
    }

    override fun unregisterGlobalProperties(vararg propertyNames: String?) {
        awsPinpointAnalyticsPluginBehavior.unregisterGlobalProperties(*propertyNames)
    }

    override fun flushEvents() {
        awsPinpointAnalyticsPluginBehavior.flushEvents()
    }

    override fun getPluginKey(): String {
        return pluginKey
    }

    override fun configure(pluginConfiguration: JSONObject?, context: Context) {
        requireNotNull(pluginConfiguration)
        val configBuilder = AWSPinpointAnalyticsPluginConfiguration.builder()
        val pinpointAnalyticsConfigJson = pluginConfiguration.getJSONObject(analyticsConfigKey)
        configBuilder.withAppId(
            pinpointAnalyticsConfigJson.getString(PinpointConfigurationKey.APP_ID.configurationKey)
        )
        configBuilder.withRegion(
            pinpointAnalyticsConfigJson.getString(PinpointConfigurationKey.REGION.configurationKey)
        )
        if (pinpointAnalyticsConfigJson.has(PinpointConfigurationKey.AUTO_FLUSH_INTERVAL.configurationKey)) {
            configBuilder.withAutoFlushEventsInterval(
                pinpointAnalyticsConfigJson.getLong(PinpointConfigurationKey.AUTO_FLUSH_INTERVAL.configurationKey)
            )
        }
        if (pinpointAnalyticsConfigJson.has(PinpointConfigurationKey.TRACK_APP_LIFECYCLE_EVENTS.configurationKey)) {
            configBuilder.withTrackAppLifecycleEvents(
                pinpointAnalyticsConfigJson.getBoolean(
                    PinpointConfigurationKey.TRACK_APP_LIFECYCLE_EVENTS.configurationKey
                )
            )
        }
        val awsAnalyticsConfig = configBuilder.build()
        pinpointManager = PinpointManager(
            context,
            awsAnalyticsConfig,
            CognitoCredentialsProvider()
        )
        val autoEventSubmitter = AutoEventSubmitter(
            pinpointManager.analyticsClient,
            awsAnalyticsConfig.autoFlushEventsInterval
        )
        val autoSessionTracker = AutoSessionTracker(pinpointManager.analyticsClient, pinpointManager.sessionClient)
        awsPinpointAnalyticsPluginBehavior = AWSPinpointAnalyticsPluginBehavior(
            context,
            pinpointManager.analyticsClient,
            pinpointManager.targetingClient,
            autoEventSubmitter,
            autoSessionTracker
        )
        autoSessionTracker.startSessionTracking(context.applicationContext as Application)
        autoEventSubmitter.start()
    }

    override fun getEscapeHatch(): PinpointClient {
        return pinpointManager.pinpointClient
    }

    override fun getVersion(): String {
        return BuildConfig.VERSION_NAME
    }
}

private enum class PinpointConfigurationKey(
    /**
     * The key this property is listed under in the config JSON.
     */
    val configurationKey: String
) {
    /**
     * The Pinpoint Application Id.
     */
    APP_ID("appId"),

    /**
     * the AWS Regions for the Pinpoint service.
     */
    REGION("region"),

    /**
     * Time interval after which the events are automatically submitted to pinpoint.
     */
    AUTO_FLUSH_INTERVAL("autoFlushEventsInterval"),

    /**
     * Whether to track app lifecycle events automatically.
     */
    TRACK_APP_LIFECYCLE_EVENTS("trackAppLifecycleEvents");
}
