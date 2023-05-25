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

import com.amplifyframework.analytics.AnalyticsBooleanProperty
import com.amplifyframework.analytics.AnalyticsCategoryBehavior
import com.amplifyframework.analytics.AnalyticsDoubleProperty
import com.amplifyframework.analytics.AnalyticsEventBehavior
import com.amplifyframework.analytics.AnalyticsIntegerProperty
import com.amplifyframework.analytics.AnalyticsProperties
import com.amplifyframework.analytics.AnalyticsStringProperty
import com.amplifyframework.analytics.UserProfile
import com.amplifyframework.pinpoint.core.AnalyticsClient
import com.amplifyframework.pinpoint.core.TargetingClient

internal class AWSPinpointAnalyticsPluginBehavior(
    private val analyticsClient: AnalyticsClient,
    private val targetingClient: TargetingClient,
) : AnalyticsCategoryBehavior {

    override fun identifyUser(userId: String, profile: UserProfile?) = targetingClient.identifyUser(userId, profile)

    override fun disable() = analyticsClient.disableEventSubmitter()

    override fun enable() = analyticsClient.enableEventSubmitter()

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
                is AnalyticsStringProperty, is AnalyticsBooleanProperty -> {
                    attributes[key] = analyticsProperty.value.toString()
                }
                is AnalyticsIntegerProperty, is AnalyticsDoubleProperty -> {
                    metrics[key] = analyticsProperty.value.toString().toDouble()
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
        properties.forEach {
            val key = it.key
            when (val property = it.value) {
                is AnalyticsStringProperty, is AnalyticsBooleanProperty -> {
                    analyticsClient.addGlobalAttribute(key, property.value.toString())
                }
                is AnalyticsIntegerProperty, is AnalyticsDoubleProperty -> {
                    analyticsClient.addGlobalMetric(key, property.value.toString().toDouble())
                }
            }
        }
    }

    override fun unregisterGlobalProperties(vararg propertyNames: String?) {
        propertyNames.forEach { propertyName ->
            propertyName?.let {
                analyticsClient.removeGlobalAttribute(it)
                analyticsClient.removeGlobalMetric(it)
            }
        }
    }

    override fun flushEvents() = analyticsClient.flushEvents()
}
