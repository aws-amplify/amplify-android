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
import com.amplifyframework.analytics.AnalyticsPropertyBehavior
import com.amplifyframework.analytics.AnalyticsStringProperty
import com.amplifyframework.analytics.UserProfile
import com.amplifyframework.analytics.pinpoint.models.AWSPinpointUserProfile
import com.amplifyframework.analytics.pinpoint.targeting.TargetingClient
import com.amplifyframework.analytics.pinpoint.targeting.endpointProfile.EndpointProfileUser

internal class AWSPinpointAnalyticsPluginBehavior(
    private val context: Context,
    val analyticsClient: AnalyticsClient,
    private val targetingClient: TargetingClient,
    private val autoEventSubmitter: AutoEventSubmitter,
    private val autoSessionTracker: AutoSessionTracker
) : AnalyticsCategoryBehavior {

    companion object {
        private const val USER_NAME_KEY = "name"
        private const val USER_PLAN_KEY = "plan"
        private const val USER_EMAIL_KEY = "email"
    }

    override fun identifyUser(userId: String, profile: UserProfile?) {
        val endpointUser = EndpointProfileUser().apply {
            setUserId(userId)
            if (profile is AWSPinpointUserProfile) {
                profile.userAttributes?.let {
                    it.forEach { entry ->
                        when (val attribute = entry.value) {
                            is AnalyticsPropertyBehavior -> {
                                addUserAttribute(entry.key, listOf(attribute.value.toString()))
                            }
                        }
                    }
                }
            }
        }
        val endpointProfile = targetingClient.currentEndpoint().apply {
            addAttribute(USER_NAME_KEY, profile?.name?.let { listOf(it) } ?: emptyList())
            addAttribute(USER_EMAIL_KEY, profile?.email?.let { listOf(it) } ?: emptyList())
            addAttribute(USER_PLAN_KEY, profile?.plan?.let { listOf(it) } ?: emptyList())
            user = endpointUser
        }
        targetingClient.updateEndpointProfile(endpointProfile)
    }

    override fun disable() {
        autoEventSubmitter.stop()
        autoSessionTracker.stopSessionTracking(context.applicationContext as Application)
    }

    override fun enable() {
        autoEventSubmitter.start()
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

    override fun flushEvents() {
        analyticsClient.flushEvents()
    }
}
