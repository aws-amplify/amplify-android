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

import com.amplifyframework.analytics.AnalyticsBooleanProperty
import com.amplifyframework.analytics.AnalyticsCategoryBehavior
import com.amplifyframework.analytics.AnalyticsDoubleProperty
import com.amplifyframework.analytics.AnalyticsEventBehavior
import com.amplifyframework.analytics.AnalyticsIntegerProperty
import com.amplifyframework.analytics.AnalyticsProperties
import com.amplifyframework.analytics.AnalyticsStringProperty
import com.amplifyframework.analytics.UserProfile
import java.lang.IllegalArgumentException

internal class AWSPinpointAnalyticsPluginBehavior(
    val analyticsClient: AnalyticsClient
) : AnalyticsCategoryBehavior {

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
        val pinpointEvent = analyticsClient.createEvent(eventName)
        analyticsClient.recordEvent(pinpointEvent)
    }

    override fun recordEvent(analyticsEvent: AnalyticsEventBehavior) {
        val attributes = mutableMapOf<String, String>()
        val metrics = mutableMapOf<String, Double>()
        analyticsEvent.properties.forEach { property ->
            val key = property.key
            val analyticsProperty = property.value
            when (analyticsProperty.value) {
                is AnalyticsStringProperty -> {
                    attributes[key] = (analyticsProperty.value as AnalyticsStringProperty).value
                }
                is AnalyticsBooleanProperty -> {
                    attributes[key] = (analyticsProperty.value as AnalyticsBooleanProperty).value.toString()
                }
                is AnalyticsIntegerProperty -> {
                    metrics[key] = (analyticsProperty.value as AnalyticsIntegerProperty).value.toDouble()
                }
                is AnalyticsDoubleProperty -> {
                    metrics[key] = (analyticsProperty.value as AnalyticsDoubleProperty).value
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
        TODO("Not yet implemented")
    }
}
