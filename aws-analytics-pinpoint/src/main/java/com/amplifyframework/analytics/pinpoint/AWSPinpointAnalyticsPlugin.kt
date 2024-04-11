/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import com.amplifyframework.analytics.AnalyticsEventBehavior
import com.amplifyframework.analytics.AnalyticsPlugin
import com.amplifyframework.analytics.AnalyticsProperties
import com.amplifyframework.analytics.UserProfile
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.auth.CognitoCredentialsProvider
import com.amplifyframework.core.configuration.AmplifyOutputsData
import org.json.JSONObject

/**
 * The plugin implementation for Amazon Pinpoint in Analytics category.
 */
class AWSPinpointAnalyticsPlugin @JvmOverloads constructor(
    private val options: AWSPinpointAnalyticsPluginOptions? = null
) : AnalyticsPlugin<PinpointClient>() {

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

        // Use the programmatic options if they were supplied, otherwise read additional options from the
        // amplifyconfiguration file
        if (options != null) {
            configBuilder.withAutoFlushEventsInterval(options.autoFlushEventsInterval)
        } else if (pinpointAnalyticsConfigJson.has(PinpointConfigurationKey.AUTO_FLUSH_INTERVAL.configurationKey)) {
            configBuilder.withAutoFlushEventsInterval(
                pinpointAnalyticsConfigJson.getLong(PinpointConfigurationKey.AUTO_FLUSH_INTERVAL.configurationKey)
            )
        }
        val awsAnalyticsConfig = configBuilder.build()
        configure(awsAnalyticsConfig, context)
    }

    @InternalAmplifyApi
    override fun configure(configuration: AmplifyOutputsData, context: Context) {
        val options = this.options ?: AWSPinpointAnalyticsPluginOptions.defaults()
        val analyticsConfig = AWSPinpointAnalyticsPluginConfiguration.from(configuration, options)
        configure(analyticsConfig, context)
    }

    private fun configure(configuration: AWSPinpointAnalyticsPluginConfiguration, context: Context) {
        pinpointManager = PinpointManager(
            context,
            configuration,
            CognitoCredentialsProvider()
        )

        awsPinpointAnalyticsPluginBehavior = AWSPinpointAnalyticsPluginBehavior(
            pinpointManager.analyticsClient,
            pinpointManager.targetingClient
        )
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
    AUTO_FLUSH_INTERVAL("autoFlushEventsInterval")
}
