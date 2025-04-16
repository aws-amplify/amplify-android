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
    private val userOptions: Options? = null
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

    override fun getPluginKey(): String = pluginKey

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
        if (userOptions != null) {
            configBuilder.withAutoFlushEventsInterval(userOptions.autoFlushEventsInterval)
                .withTrackAppLifecycleEvents(userOptions.trackLifecycleEvents)
        } else {
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
        }
        val awsAnalyticsConfig = configBuilder.build()
        configure(awsAnalyticsConfig, context)
    }

    @InternalAmplifyApi
    override fun configure(configuration: AmplifyOutputsData, context: Context) {
        val options = this.userOptions ?: Options.defaults()
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

    override fun getEscapeHatch(): PinpointClient = pinpointManager.pinpointClient

    override fun getVersion(): String = BuildConfig.VERSION_NAME

    /**
     * Options that can be specified to fine-tune the behavior of the Pinpoint Analytics Plugin.
     */
    data class Options internal constructor(
        /**
         * The interval between sends of queued analytics events, in milliseconds
         */
        val autoFlushEventsInterval: Long,

        /**
         * If true then the plugin will stop sessions when the app goes to the background
         */
        val trackLifecycleEvents: Boolean
    ) {
        companion object {
            /**
             * Create a new [Builder] instance
             */
            @JvmStatic
            fun builder() = Builder()

            /**
             * Create an [AWSPinpointAnalyticsPlugin.Options] instance
             */
            @JvmSynthetic
            operator fun invoke(func: Builder.() -> Unit) = Builder().apply(func).build()

            internal fun defaults() = builder().build()
        }

        /**
         * Builder API for constructing [AWSPinpointAnalyticsPlugin.Options] instances
         */
        class Builder internal constructor() {
            /**
             * Set the interval between sends of queued analytics events, in milliseconds
             */
            var autoFlushEventsInterval: Long = AWSPinpointAnalyticsPluginConfiguration.DEFAULT_AUTO_FLUSH_INTERVAL
                @JvmSynthetic set

            /**
             * Set whether or not the plugin will stop/start sessions when the app goes to the background/foreground.
             */
            var trackLifecycleEvents: Boolean = true
                @JvmSynthetic set

            /**
             * Set the interval between sends of queed analytics events, in milliseconds
             */
            fun autoFlushEventsInterval(value: Long) = apply { autoFlushEventsInterval = value }

            /**
             * Set whether or not the plugin will stop/start sessions when the app goes to the background/foreground.
             */
            fun trackLifecycleEvents(value: Boolean) = apply { trackLifecycleEvents = value }

            fun build() = Options(
                autoFlushEventsInterval = autoFlushEventsInterval,
                trackLifecycleEvents = trackLifecycleEvents
            )
        }
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
    TRACK_APP_LIFECYCLE_EVENTS("trackAppLifecycleEvents")
}
