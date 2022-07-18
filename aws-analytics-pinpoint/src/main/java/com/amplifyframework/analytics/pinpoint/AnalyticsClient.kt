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
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import com.amplifyframework.analytics.pinpoint.database.PinpointDatabase
import com.amplifyframework.analytics.pinpoint.models.AndroidAppDetails
import com.amplifyframework.analytics.pinpoint.models.AndroidDeviceDetails
import com.amplifyframework.analytics.pinpoint.models.PinpointEvent
import com.amplifyframework.analytics.pinpoint.models.PinpointSession
import com.amplifyframework.analytics.pinpoint.models.SDKInfo
import java.lang.IllegalStateException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class AnalyticsClient(
    val context: Context,
    private val pinpointClient: PinpointClient,
    private val sessionClient: SessionClient,
    private val pinpointDatabase: PinpointDatabase,
    private val androidAppDetails: AndroidAppDetails,
    private val androidDeviceDetails: AndroidDeviceDetails,
    private val sdkInfo: SDKInfo,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val eventRecorder: EventRecorder = EventRecorder(
        context,
        pinpointClient,
        pinpointDatabase,
        coroutineDispatcher
    )
) {

    private val coroutineScope = CoroutineScope(coroutineDispatcher)
    private val globalAttributes = ConcurrentHashMap<String, String>()
    private val globalMetrics = ConcurrentHashMap<String, Double>()
    private val eventTypeAttributes = ConcurrentHashMap<String, Map<String, String>>()
    private val eventTypeMetrics = ConcurrentHashMap<String, Map<String, Double>>()

    fun createEvent(
        eventType: String,
        attributes: MutableMap<String, String> = mutableMapOf(),
        metrics: MutableMap<String, Double> = mutableMapOf()
    ): PinpointEvent {
        val session = sessionClient.session ?: throw IllegalStateException("session is null")
        return createEvent(
            eventType,
            session.sessionId,
            session.startTime,
            session.stopTime,
            session.sessionDuration,
            attributes,
            metrics
        )
    }

    fun createEvent(
        eventType: String,
        sessionId: String,
        sessionStart: Long,
        sessionEnd: Long? = null,
        sessionDuration: Long = 0L,
        attributes: MutableMap<String, String> = mutableMapOf(),
        metrics: MutableMap<String, Double> = mutableMapOf()
    ): PinpointEvent {
        globalAttributes.forEach { (key, value) ->
            attributes[key] = value
        }
        eventTypeAttributes[eventType]?.forEach { (key, value) ->
            attributes[key] = value
        }
        globalMetrics.forEach { (key, value) ->
            metrics[key] = value
        }
        eventTypeMetrics[eventType]?.forEach { (key, value) ->
            metrics[key] = value
        }
        return PinpointEvent(
            eventType = eventType,
            attributes = attributes,
            metrics = metrics,
            sdkInfo = sdkInfo,
            pinpointSession = PinpointSession(sessionId, sessionStart, sessionEnd, sessionDuration),
            eventTimestamp = System.currentTimeMillis(),
            uniqueId = "", // TODO: Get Unique from shared preferences
            androidAppDetails = androidAppDetails,
            androidDeviceDetails = androidDeviceDetails
        )
    }

    fun recordEvent(event: PinpointEvent) {
        coroutineScope.launch {
            eventRecorder.recordEvent(event)
        }
    }

    fun submitEvents() {
        coroutineScope.launch {
            eventRecorder.submitEvents()
        }
    }

    fun addGlobalAttribute(attributeName: String, attributeValue: String) {
    }

    fun addGlobalAttribute(eventType: String, attributeName: String, attributeValue: String) {
    }

    fun addGlobalMetric(metricName: String, metricValue: String) {
    }

    fun addGlobalMetric(eventType: String, metricName: String, metricValue: String) {
    }

    fun removeGlobalAttribute(attributeName: String) {
    }

    fun removeGlobalAttribute(eventType: String, attributeName: String) {
    }

    fun removeGlobalMetric(metricName: String) {
    }

    fun removeGlobalMetric(eventType: String, metricName: String) {
    }
}
