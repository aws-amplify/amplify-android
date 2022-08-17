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
import com.amplifyframework.analytics.AnalyticsBooleanProperty
import com.amplifyframework.analytics.AnalyticsCategoryBehavior
import com.amplifyframework.analytics.AnalyticsDoubleProperty
import com.amplifyframework.analytics.AnalyticsEventBehavior
import com.amplifyframework.analytics.AnalyticsIntegerProperty
import com.amplifyframework.analytics.AnalyticsProperties
import com.amplifyframework.analytics.AnalyticsStringProperty
import com.amplifyframework.analytics.UserProfile

internal class AWSPinpointAnalyticsPluginBehavior(
    private val context: Context,
    val analyticsClient: AnalyticsClient,
    private val autoEventSubmitter: AutoEventSubmitter,
    private val autoSessionTracker: AutoSessionTracker
) : AnalyticsCategoryBehavior {

    override fun identifyUser(userId: String, profile: UserProfile?) {
        TODO("Not yet implemented")
    }

    override fun disable() {
        autoEventSubmitter.stop()
        autoSessionTracker.stopSessionTracking(context.applicationContext as Application)
    }

    override fun enable() {
        autoEventSubmitter.start()
        // Start auto session tracking
        autoSessionTracker.startSessionTracking(context.applicationContext as Application)
    }

    override fun recordEvent(eventName: String) {
        val pinpointEvent = analyticsClient.createEvent(eventName)
        analyticsClient.recordEvent(pinpointEvent)
    }

    override fun recordEvent(analyticsEvent: AnalyticsEventBehavior) {
        val attributes = mutableMapOf<String, String>()
        val metrics = mutableMapOf<String, Double>()
        analyticsEvent.properties.forEach { property ->
            val key = property.key
            when (val analyticsProperty = property.value) {
                is AnalyticsStringProperty -> {
                    attributes[key] = analyticsProperty.value
                }
                is AnalyticsBooleanProperty -> {
                    attributes[key] = analyticsProperty.value.toString()
                }
                is AnalyticsIntegerProperty -> {
                    metrics[key] = analyticsProperty.value.toDouble()
                }
                is AnalyticsDoubleProperty -> {
                    metrics[key] = analyticsProperty.value
                }
                else -> {
                    throw IllegalArgumentException("Invalid property type")
                }
            }
        }
        val pinpointEvent = analyticsClient.createEvent(analyticsEvent.name, attributes, metrics)
        analyticsClient.recordEvent(pinpointEvent)
    }

    override fun registerGlobalProperties(properties: AnalyticsProperties) {
        TODO("Not yet implemented")
    }

    override fun unregisterGlobalProperties(vararg propertyNames: String?) {
        TODO("Not yet implemented")
    }

    override fun flushEvents() {
        analyticsClient.submitEvents()
    }
}
