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

package com.amplifyframework.pushnotifications.pinpoint

import com.amplifyframework.notifications.pushnotifications.NotificationContentProvider
import com.amplifyframework.notifications.pushnotifications.NotificationPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class EventSourceTypeTest {
    // set up campaign payload
    private val campaignIdKey = PushNotificationsConstants.PINPOINT_CAMPAIGN_CAMPAIGN_ID
    private val campaignActivityIdKey = PushNotificationsConstants.PINPOINT_CAMPAIGN_CAMPAIGN_ACTIVITY_ID
    private val campaignTreatmentIdKey = "${PushNotificationsConstants.CAMPAIGN_PREFIX}treatment_id"

    private val campaignData = mapOf(
        campaignIdKey to "test_campaign_id",
        campaignActivityIdKey to "test_campaign_activity_id",
        campaignTreatmentIdKey to "test_treatment_id"
    )

    private val campaignPayload = NotificationPayload(NotificationContentProvider.FCM(campaignData))

    // set up journey payload
    private val journeyIdKey = PushNotificationsConstants.JOURNEY_ID
    private val journeyActivityIdKey = PushNotificationsConstants.JOURNEY_ACTIVITY_ID
    private val journeyRunIdKey = "journey_run_id"
    private val journeyAttributes = "{\"${PushNotificationsConstants.JOURNEY}\":" +
        "{\"$journeyActivityIdKey\":\"test_journey_activity_id\",\"$journeyRunIdKey\":\"test_journey_run_id\"," +
        "\"$journeyIdKey\":\"test_journey_id\"}}"

    private val journeyData = mapOf("pinpoint" to journeyAttributes)
    private val journeyPayload = NotificationPayload(NotificationContentProvider.FCM(journeyData))

    @Test
    fun testCampaignEventTypeBackgroundReceived() {
        val eventSource = EventSourceType.getEventSourceType(campaignPayload)
        assertEquals(eventSource.getEventTypeReceived(false), "_campaign.received_background")
    }

    @Test
    fun testCampaignEventTypeForegroundReceived() {
        val eventSource = EventSourceType.getEventSourceType(campaignPayload)
        assertEquals(eventSource.getEventTypeReceived(true), "_campaign.received_foreground")
    }

    @Test
    fun testCampaignEventAttributes() {
        val eventSource = EventSourceType.getEventSourceType(campaignPayload)
        val attributes = eventSource.attributeParser.parseAttributes(campaignPayload)

        assertEquals(eventSource.eventSourceIdAttributeKey, PushNotificationsConstants.CAMPAIGN_ID)
        assertEquals(eventSource.eventSourceActivityAttributeKey, PushNotificationsConstants.CAMPAIGN_ACTIVITY_ID)

        assertEquals(attributes["campaign_id"], campaignData[campaignIdKey])
        assertEquals(attributes["campaign_activity_id"], campaignData[campaignActivityIdKey])
        assertEquals(attributes["treatment_id"], campaignData[campaignTreatmentIdKey])
    }

    @Test
    fun testJourneyEventTypeBackgroundReceived() {
        val eventSource = EventSourceType.getEventSourceType(journeyPayload)
        assertEquals(eventSource.getEventTypeReceived(false), "_journey.received_background")
    }

    @Test
    fun testJourneyEventTypeForegroundReceived() {
        val eventSource = EventSourceType.getEventSourceType(journeyPayload)
        assertEquals(eventSource.getEventTypeReceived(true), "_journey.received_foreground")
    }

    @Test
    fun testJourneyEventAttributes() {
        val eventSource = EventSourceType.getEventSourceType(journeyPayload)
        val attributes = eventSource.attributeParser.parseAttributes(journeyPayload)

        assertEquals(eventSource.eventSourceIdAttributeKey, journeyIdKey)
        assertEquals(eventSource.eventSourceActivityAttributeKey, journeyActivityIdKey)

        assertEquals(attributes[journeyIdKey], "test_journey_id")
        assertEquals(attributes[journeyActivityIdKey], "test_journey_activity_id")
        assertEquals(attributes[journeyRunIdKey], "test_journey_run_id")
    }
}
