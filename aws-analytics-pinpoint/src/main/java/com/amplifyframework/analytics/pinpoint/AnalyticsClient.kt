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
import com.amplifyframework.analytics.AnalyticsChannelEventName
import com.amplifyframework.analytics.pinpoint.database.PinpointDatabase
import com.amplifyframework.analytics.pinpoint.internal.core.idresolver.SharedPrefsUniqueIdService
import com.amplifyframework.analytics.pinpoint.models.AndroidAppDetails
import com.amplifyframework.analytics.pinpoint.models.AndroidDeviceDetails
import com.amplifyframework.analytics.pinpoint.models.PinpointEvent
import com.amplifyframework.analytics.pinpoint.models.PinpointSession
import com.amplifyframework.analytics.pinpoint.models.SDKInfo
import com.amplifyframework.analytics.pinpoint.targeting.TargetingClient
import com.amplifyframework.core.Amplify
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class AnalyticsClient(
    val context: Context,
    pinpointClient: PinpointClient,
    private val sessionClient: SessionClient,
    targetingClient: TargetingClient,
    pinpointDatabase: PinpointDatabase,
    private val sharedPrefsUniqueIdService: SharedPrefsUniqueIdService,
    private val androidAppDetails: AndroidAppDetails,
    private val androidDeviceDetails: AndroidDeviceDetails,
    private val sdkInfo: SDKInfo,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val eventRecorder: EventRecorder = EventRecorder(
        context,
        pinpointClient,
        pinpointDatabase,
        targetingClient,
        coroutineDispatcher
    )
) {
    private val coroutineScope = CoroutineScope(coroutineDispatcher)
    private val globalAttributes = ConcurrentHashMap<String, String>()
    private val globalMetrics = ConcurrentHashMap<String, Double>()

    fun createEvent(
        eventType: String,
        attributes: MutableMap<String, String> = mutableMapOf(),
        metrics: MutableMap<String, Double> = mutableMapOf(),
        eventTimestamp: Long = System.currentTimeMillis(),
        eventId: String = UUID.randomUUID().toString()
    ): PinpointEvent {
        val session = sessionClient.session ?: throw IllegalStateException("session is null")
        return createEvent(
            eventType,
            session.sessionId,
            session.startTime,
            session.stopTime,
            session.sessionDuration,
            attributes,
            metrics,
            eventTimestamp,
            eventId
        )
    }

    fun createEvent(
        eventType: String,
        sessionId: String,
        sessionStart: Long,
        sessionEnd: Long? = null,
        sessionDuration: Long = 0L,
        attributes: MutableMap<String, String> = mutableMapOf(),
        metrics: MutableMap<String, Double> = mutableMapOf(),
        eventTimestamp: Long = System.currentTimeMillis(),
        eventId: String = UUID.randomUUID().toString()
    ): PinpointEvent {
        globalAttributes.forEach { (key, value) ->
            attributes[key] = value
        }
        globalMetrics.forEach { (key, value) ->
            metrics[key] = value
        }
        return PinpointEvent(
            eventId = eventId,
            eventType = eventType,
            attributes = attributes,
            metrics = metrics,
            sdkInfo = sdkInfo,
            pinpointSession = PinpointSession(sessionId, sessionStart, sessionEnd, sessionDuration),
            eventTimestamp = eventTimestamp,
            uniqueId = sharedPrefsUniqueIdService.getUniqueId(),
            androidAppDetails = androidAppDetails,
            androidDeviceDetails = androidDeviceDetails
        )
    }

    fun recordEvent(event: PinpointEvent) {
        coroutineScope.launch {
            eventRecorder.recordEvent(event)
        }
    }

    fun flushEvents() {
        coroutineScope.launch {
            val syncedEvents = eventRecorder.submitEvents()
            Amplify.Hub.publish(
                HubChannel.ANALYTICS,
                HubEvent.create(AnalyticsChannelEventName.FLUSH_EVENTS, syncedEvents)
            )
        }
    }

    fun addGlobalAttribute(attributeName: String, attributeValue: String) {
        globalAttributes[attributeName] = attributeValue
    }

    fun addGlobalMetric(metricName: String, metricValue: Double) {
        globalMetrics[metricName] = metricValue
    }

    fun removeGlobalAttribute(attributeName: String) {
        globalAttributes.remove(attributeName)
    }

    fun removeGlobalMetric(metricName: String) {
        globalMetrics.remove(metricName)
    }

    /*
    * adding for testing
    * */
    internal fun getGlobalAttributes() = globalAttributes

    /*
    * adding for testing
    * */
    internal fun getGlobalMetrics() = globalMetrics
}
