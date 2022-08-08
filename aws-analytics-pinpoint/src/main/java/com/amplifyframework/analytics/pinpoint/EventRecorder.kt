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
import android.database.Cursor
import android.net.Uri
import aws.sdk.kotlin.services.pinpoint.PinpointClient
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
import com.amplifyframework.analytics.pinpoint.database.EventTable
import com.amplifyframework.analytics.pinpoint.database.PinpointDatabase
import com.amplifyframework.analytics.pinpoint.internal.core.util.DateUtil
import com.amplifyframework.analytics.pinpoint.models.PinpointEvent
import com.amplifyframework.analytics.pinpoint.targeting.TargetingClient
import com.amplifyframework.analytics.pinpoint.targeting.endpointProfile.EndpointProfile
import com.amplifyframework.core.Amplify
import com.amplifyframework.logging.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class EventRecorder(
    val context: Context,
    private val pinpointClient: PinpointClient,
    private val pinpointDatabase: PinpointDatabase,
    private val targetingClient: TargetingClient,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val logger: Logger =
        Amplify.Logging.forNamespace(
            AWS_PINPOINT_ANALYTICS_LOG_NAMESPACE.format(EventRecorder::class.java.simpleName)
        )
) {
    private val defaultMaxSubmissionAllowed = 3
    private val defaultMaxSubmissionSize = 1024 * 100
    private val serviceDefinedMaxEventsPerBatch: Int = 100
    internal suspend fun recordEvent(pinpointEvent: PinpointEvent): Uri {
        return withContext(coroutineDispatcher) {
            pinpointDatabase.saveEvent(pinpointEvent)
        }
    }

    internal suspend fun submitEvents(): List<AnalyticsEvent> {
        return withContext(coroutineDispatcher) {
            val syncedPinpointEvents = processEvents()
            val syncedAnalyticsEvent = mutableListOf<AnalyticsEvent>()
            syncedPinpointEvents.forEach {
                syncedAnalyticsEvent.add(AnalyticsEvent.builder().name(it.eventType).build())
            }
            syncedAnalyticsEvent
        }
    }

    private suspend fun processEvents(): List<PinpointEvent> {
        val syncedPinpointEvents = mutableListOf<PinpointEvent>()
        pinpointDatabase.queryAllEvents().use { cursor ->
            var currentSubmissions = 0
            // TODO: fetch maxSubmissions from shared prefs
            val maxSubmissionsAllowed = defaultMaxSubmissionAllowed
            do {
                if (!cursor.moveToFirst())
                    return emptyList()

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
            } while (currentSubmissions < maxSubmissionsAllowed && cursor.moveToNext())
        }
        return syncedPinpointEvents
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
                        if (isRetryableError(message)) {
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

    private fun isRetryableError(responseCode: String): Boolean {
        return !(
            responseCode.equals("ValidationException", ignoreCase = true) ||
                responseCode.equals("SerializationException", ignoreCase = true) ||
                responseCode.equals("BadRequestException", ignoreCase = true)
            )
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
                startTimestamp = DateUtil.isoDateFromMillis(pinpointEvent.pinpointSession.sessionStart)
                stopTimestamp = pinpointEvent.pinpointSession.sessionEnd?.let {
                    DateUtil.isoDateFromMillis(pinpointEvent.pinpointSession.sessionStart)
                }
                duration = pinpointEvent.pinpointSession.sessionDuration?.toInt()
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
                timestamp = DateUtil.isoDateFromMillis(pinpointEvent.eventTimestamp)
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
            latitude = endpointProfile.location.latitude
            longitude = endpointProfile.location.longitude
            postalCode = endpointProfile.location.postalCode
            city = endpointProfile.location.city
            region = endpointProfile.location.region
            country = endpointProfile.location.country
        }
        val endpointUser = EndpointUser {
            userId = endpointProfile.user.getUserId()
            userAttributes = endpointProfile.user.getUserAttributes()
        }

        return PublicEndpoint {
            channelType = endpointProfile.channelType
            location = endpointLocation
            demographic = endpointDemographic
            effectiveDate = DateUtil.isoDateFromMillis(endpointProfile.effectiveDate)
            optOut = endpointProfile.optOut
            attributes = endpointProfile.allAttributes
            metrics = endpointProfile.allMetrics
            user = endpointUser
        }
    }

    private fun getNextBatchOfEvents(cursor: Cursor): Map<Int, PinpointEvent> {
        val result = mutableMapOf<Int, PinpointEvent>()
        var currentRequestSize = 0
        // TODO: fetch maxSubmissionSize from shared prefs
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
