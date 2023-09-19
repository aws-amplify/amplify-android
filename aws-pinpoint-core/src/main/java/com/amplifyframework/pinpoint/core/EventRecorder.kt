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
package com.amplifyframework.pinpoint.core

import android.content.Context
import android.database.Cursor
import android.net.Uri
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import aws.sdk.kotlin.services.pinpoint.model.ChannelType
import aws.sdk.kotlin.services.pinpoint.model.EndpointDemographic
import aws.sdk.kotlin.services.pinpoint.model.EndpointItemResponse
import aws.sdk.kotlin.services.pinpoint.model.EndpointLocation
import aws.sdk.kotlin.services.pinpoint.model.EndpointUser
import aws.sdk.kotlin.services.pinpoint.model.Event
import aws.sdk.kotlin.services.pinpoint.model.EventItemResponse
import aws.sdk.kotlin.services.pinpoint.model.EventsBatch
import aws.sdk.kotlin.services.pinpoint.model.EventsRequest
import aws.sdk.kotlin.services.pinpoint.model.PublicEndpoint
import aws.sdk.kotlin.services.pinpoint.model.PutEventsRequest
import aws.sdk.kotlin.services.pinpoint.model.Session
import com.amplifyframework.analytics.AnalyticsEvent
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.logging.Logger
import com.amplifyframework.pinpoint.core.database.EventTable
import com.amplifyframework.pinpoint.core.database.PinpointDatabase
import com.amplifyframework.pinpoint.core.endpointProfile.EndpointProfile
import com.amplifyframework.pinpoint.core.models.PinpointEvent
import com.amplifyframework.pinpoint.core.util.millisToIsoDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val AWS_PINPOINT_ANALYTICS_LOG_NAMESPACE = "amplify:aws-pinpoint-analytics:%s"

@InternalAmplifyApi
class EventRecorder(
    val context: Context,
    private val pinpointClient: PinpointClient,
    private val pinpointDatabase: PinpointDatabase,
    private val targetingClient: TargetingClient,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val logger: Logger =
        Amplify.Logging.logger(
            CategoryType.ANALYTICS,
            AWS_PINPOINT_ANALYTICS_LOG_NAMESPACE.format(EventRecorder::class.java.simpleName)
        )
) {
    private var isSyncInProgress = false
    private val defaultMaxSubmissionAllowed = 3
    private val defaultMaxSubmissionSize = 1024 * 100
    private val serviceDefinedMaxEventsPerBatch: Int = 100
    private val badRequestCode = 400
    internal suspend fun recordEvent(pinpointEvent: PinpointEvent): Uri? {
        return withContext(coroutineDispatcher) {
            val result = runCatching {
                pinpointDatabase.saveEvent(pinpointEvent)
            }
            when {
                result.isSuccess -> result.getOrNull()
                else -> {
                    logger.error("Failed to record event ${result.exceptionOrNull()}")
                    null
                }
            }
        }
    }

    @Synchronized
    internal suspend fun submitEvents(): List<AnalyticsEvent> {
        return withContext(coroutineDispatcher) {
            val result = runCatching {
                if (!isSyncInProgress) {
                    isSyncInProgress = true
                    processEvents()
                } else {
                    logger.info("Sync is already in progress, skipping")
                    emptyList()
                }
            }
            when {
                result.isSuccess -> {
                    isSyncInProgress = false
                    result.getOrNull() ?: emptyList()
                }
                else -> {
                    isSyncInProgress = false
                    logger.error("Failed to submit events ${result.exceptionOrNull()}")
                    emptyList()
                }
            }
        }
    }

    private suspend fun processEvents(): List<AnalyticsEvent> {
        val syncedAnalyticsEvents = mutableListOf<AnalyticsEvent>()
        val syncedPinpointEvents = mutableListOf<PinpointEvent>()
        pinpointDatabase.queryAllEvents().use { cursor ->
            var currentSubmissions = 0
            val maxSubmissionsAllowed = defaultMaxSubmissionAllowed
            do {
                if (!cursor.moveToFirst()) {
                    return emptyList()
                }

                val pinpointEvents = getNextBatchOfEvents(cursor)
                val eventToColumnIdMap = mutableMapOf<String, Int>()
                pinpointEvents.forEach { (key, value) ->
                    eventToColumnIdMap[value.eventId] = key
                }
                val submittedEvent = submitEventsAndProcessResponse(
                    pinpointEvents,
                    targetingClient.currentEndpoint()
                )
                syncedPinpointEvents.addAll(submittedEvent)
                submittedEvent.forEach {
                    eventToColumnIdMap[it.eventId]?.let { columnId ->
                        // TODO: Replace with bulk delete
                        if (pinpointDatabase.deleteEventById(columnId) > 0) {
                            logger.info("Successfully submitted ${it.eventId}, deleted event from local database")
                        }
                    }
                }
                currentSubmissions++
            } while (currentSubmissions < maxSubmissionsAllowed && !cursor.isClosed && cursor.moveToNext())
        }
        syncedPinpointEvents.forEach { pinpointEvent ->
            syncedAnalyticsEvents.add(convertPinpointEventToAnalyticsEvent(pinpointEvent))
        }
        return syncedAnalyticsEvents
    }

    private suspend fun convertPinpointEventToAnalyticsEvent(pinpointEvent: PinpointEvent): AnalyticsEvent {
        val builder = AnalyticsEvent.builder().name(pinpointEvent.eventType)
        pinpointEvent.attributes.forEach { (t, u) ->
            builder.addProperty(t, u)
        }
        pinpointEvent.metrics.forEach { (t, u) ->
            builder.addProperty(t, u)
        }
        return builder.build()
    }

    private suspend fun submitEventsAndProcessResponse(
        events: Map<Int, PinpointEvent>,
        endpointProfile: EndpointProfile
    ): List<PinpointEvent> {
        val putEventRequest = createPutEventsRequest(events, endpointProfile)
        val response = pinpointClient.putEvents(putEventRequest)
        val eventIdsToBeDeleted = mutableListOf<PinpointEvent>()
        response.eventsResponse?.results?.let { result ->
            processEndpointResponse(result[endpointProfile.endpointId]?.endpointItemResponse)
            eventIdsToBeDeleted.addAll(
                processEventResponse(
                    result[endpointProfile.endpointId]?.eventsItemResponse,
                    events.values.toList()
                )
            )
        }
        return eventIdsToBeDeleted
    }

    private fun processEventResponse(
        eventItemResponseMap: Map<String, EventItemResponse>?,
        events: List<PinpointEvent>
    ): List<PinpointEvent> {
        val eventIdToDelete = mutableListOf<PinpointEvent>()
        eventItemResponseMap?.let {
            events.forEach { pinpointEvent ->
                val pinpointEventResponse = it[pinpointEvent.eventId]
                pinpointEventResponse?.message?.let { message ->
                    if (message.equals("Accepted", ignoreCase = true)) {
                        logger.info("Successfully submitted event with eventId ${pinpointEvent.eventId}")
                        eventIdToDelete.add(pinpointEvent)
                    } else {
                        if (isRetryableError(pinpointEventResponse.statusCode)) {
                            logger.error(
                                "Failed to deliver event with ${pinpointEvent.eventId}," +
                                    " will be re-delivered later"
                            )
                        } else {
                            logger.error("Failed to deliver event with ${pinpointEvent.eventId}, response: $message")
                            eventIdToDelete.add(pinpointEvent)
                        }
                    }
                }
            }
        }
        return eventIdToDelete
    }

    private fun isRetryableError(code: Int?): Boolean {
        return code in 500..599
    }

    private fun processEndpointResponse(endpointResponse: EndpointItemResponse?) {
        endpointResponse?.let {
            if (it.statusCode == 202) {
                logger.info("EndpointProfile updated successfully.")
            } else {
                logger.error("AmazonServiceException occurred during endpoint update: ${it.message}")
            }
        } ?: logger.error("EndPointItemResponse is null")
    }

    private fun createPutEventsRequest(
        pinpointEvents: Map<Int, PinpointEvent>,
        currentEndpointProfile: EndpointProfile
    ): PutEventsRequest {
        val eventsBatch = EventsBatch {
            endpoint = buildEndpointPayload(currentEndpointProfile)
            events = getEventsMap(pinpointEvents)
        }
        return PutEventsRequest {
            applicationId = currentEndpointProfile.applicationId
            eventsRequest = EventsRequest {
                batchItem = mapOf(currentEndpointProfile.endpointId to eventsBatch)
            }
        }
    }

    private fun getEventsMap(events: Map<Int, PinpointEvent>): Map<String, Event> {
        val result = mutableMapOf<String, Event>()
        events.values.forEach { pinpointEvent ->
            val pinpointSession = Session {
                id = pinpointEvent.pinpointSession.sessionId
                startTimestamp = pinpointEvent.pinpointSession.sessionStart.millisToIsoDate()
                stopTimestamp = pinpointEvent.pinpointSession.sessionEnd?.let {
                    pinpointEvent.pinpointSession.sessionEnd.millisToIsoDate()
                }
                pinpointEvent.pinpointSession.sessionDuration?.toInt()?.let { duration = it }
            }
            val event = Event {
                appPackageName = pinpointEvent.androidAppDetails.packageName
                appTitle = pinpointEvent.androidAppDetails.appTitle
                appVersionCode = pinpointEvent.androidAppDetails.versionCode
                attributes = pinpointEvent.attributes
                clientSdkVersion = pinpointEvent.sdkInfo.version
                eventType = pinpointEvent.eventType
                sdkName = pinpointEvent.sdkInfo.name
                session = pinpointSession
                timestamp = pinpointEvent.eventTimestamp.millisToIsoDate()
            }
            result[pinpointEvent.eventId] = event
        }
        return result
    }

    private fun buildEndpointPayload(endpointProfile: EndpointProfile): PublicEndpoint {
        val endpointDemographic = EndpointDemographic {
            appVersion = endpointProfile.demographic.appVersion
            locale = endpointProfile.demographic.locale.toString()
            timezone = endpointProfile.demographic.timezone
            make = endpointProfile.demographic.make
            model = endpointProfile.demographic.model
            platform = endpointProfile.demographic.platform
            platformVersion = endpointProfile.demographic.platformVersion
        }
        val endpointLocation = EndpointLocation {
            endpointProfile.location.latitude?.let { latitude = it }
            endpointProfile.location.longitude?.let { longitude = it }
            postalCode = endpointProfile.location.postalCode
            city = endpointProfile.location.city
            region = endpointProfile.location.region
            country = endpointProfile.location.country
        }
        val endpointUser = EndpointUser {
            userId = endpointProfile.user.userId
            userAttributes = endpointProfile.user.userAttributes
        }

        return PublicEndpoint {
            location = endpointLocation
            demographic = endpointDemographic
            effectiveDate = endpointProfile.effectiveDate.millisToIsoDate()

            if (endpointProfile.address != "" && endpointProfile.channelType == ChannelType.Gcm) {
                optOut = "NONE" // no opt out, send notifications
                address = endpointProfile.address
                channelType = endpointProfile.channelType
            }

            attributes = endpointProfile.allAttributes
            metrics = endpointProfile.allMetrics
            user = endpointUser
        }
    }

    private fun getNextBatchOfEvents(cursor: Cursor): Map<Int, PinpointEvent> {
        val result = mutableMapOf<Int, PinpointEvent>()
        var currentRequestSize = 0
        val maxRequestSize = defaultMaxSubmissionSize
        cursor.use { it ->
            do {
                val rowId = it.getInt(EventTable.COLUMNINDEX.ID.index)
                val rowSize = it.getInt(EventTable.COLUMNINDEX.SIZE.index)
                val eventJsonString = it.getString(EventTable.COLUMNINDEX.JSON.index)
                val pinpointEvent = PinpointEvent.fromJsonString(eventJsonString)
                result.putIfAbsent(rowId, pinpointEvent)
                currentRequestSize += eventJsonString.length
            } while (
                currentRequestSize <= maxRequestSize &&
                result.size < serviceDefinedMaxEventsPerBatch &&
                it.moveToNext()
            )
        }
        return result
    }
}
