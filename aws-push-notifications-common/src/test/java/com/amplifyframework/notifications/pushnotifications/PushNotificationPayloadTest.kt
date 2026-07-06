/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.notifications.pushnotifications

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

/**
 * Unit tests for [PushNotificationPayload], the backend-agnostic payload parser.
 */
class PushNotificationPayloadTest {

    @Test
    fun `parses a plain FCM payload with no backend-specific keys`() {
        val payload = PushNotificationPayload.fromData(
            mapOf(
                PushNotificationsConstants.TITLE to "Hello",
                PushNotificationsConstants.BODY to "World",
                PushNotificationsConstants.IMAGE_URL to "https://example.com/image.png"
            )
        )

        payload.shouldNotBeNull()
        payload.title shouldBe "Hello"
        payload.body shouldBe "World"
        payload.imageUrl shouldBe "https://example.com/image.png"
        payload.canBeDisplayed.shouldBeTrue()
    }

    @Test
    fun `falls back to message key for body`() {
        val payload = PushNotificationPayload.fromData(
            mapOf(PushNotificationsConstants.MESSAGE to "From message key")
        )

        payload.shouldNotBeNull()
        payload.body shouldBe "From message key"
    }

    @Test
    fun `prefers body over message when both present`() {
        val payload = PushNotificationPayload.fromData(
            mapOf(
                PushNotificationsConstants.BODY to "preferred",
                PushNotificationsConstants.MESSAGE to "fallback"
            )
        )

        payload.shouldNotBeNull()
        payload.body shouldBe "preferred"
    }

    @Test
    fun `extracts deeplink and openApp actions`() {
        val payload = PushNotificationPayload.fromData(
            mapOf(
                PushNotificationsConstants.OPEN_APP to "true",
                PushNotificationsConstants.DEEPLINK to "myapp://home"
            )
        )

        payload.shouldNotBeNull()
        payload.action shouldContain (PushNotificationsConstants.OPEN_APP to "true")
        payload.action shouldContain (PushNotificationsConstants.DEEPLINK to "myapp://home")
    }

    @Test
    fun `forces https scheme on url action`() {
        val payload = PushNotificationPayload.fromData(
            mapOf(PushNotificationsConstants.URL to "http://example.com/page")
        )

        payload.shouldNotBeNull()
        payload.action shouldContain (PushNotificationsConstants.URL to "https://example.com/page")
    }

    @Test
    fun `does not drop a non-pinpoint payload`() {
        // Regression guard: the legacy Pinpoint parser returned null for payloads without pinpoint.* keys,
        // silently dropping valid FCM pushes. The backend-agnostic parser must not do that.
        val payload = PushNotificationPayload.fromData(
            mapOf(
                "title" to "Third party push",
                "body" to "Sent without any Pinpoint keys",
                "customKey" to "customValue"
            )
        )

        payload.shouldNotBeNull()
        payload.title shouldBe "Third party push"
        payload.body shouldBe "Sent without any Pinpoint keys"
        payload.canBeDisplayed.shouldBeTrue()
    }

    @Test
    fun `returns null only when there is no data`() {
        PushNotificationPayload.fromData(emptyMap()) shouldBe null
    }

    @Test
    fun `applies default channel id when none provided`() {
        val payload = PushNotificationPayload.fromData(
            mapOf(PushNotificationsConstants.TITLE to "Hello")
        )

        payload.shouldNotBeNull()
        payload.channelId shouldBe PushNotificationsConstants.DEFAULT_NOTIFICATION_CHANNEL_ID
    }

    @Test
    fun `honors channel id from payload data`() {
        val payload = PushNotificationPayload.fromData(
            mapOf(
                PushNotificationsConstants.TITLE to "Hello",
                PushNotificationsConstants.CHANNEL_ID to "custom.channel"
            )
        )

        payload.shouldNotBeNull()
        payload.channelId shouldBe "custom.channel"
    }

    @Test
    fun `treats silentPush 1 as silent and not displayable`() {
        val payload = PushNotificationPayload.fromData(
            mapOf(PushNotificationsConstants.SILENT_PUSH to "1")
        )

        payload.shouldNotBeNull()
        payload.silentPush.shouldBeTrue()
        payload.canBeDisplayed.shouldBeFalse()
    }

    @Test
    fun `treats silentPush true as silent`() {
        val payload = PushNotificationPayload.fromData(
            mapOf(PushNotificationsConstants.SILENT_PUSH to "true")
        )

        payload.shouldNotBeNull()
        payload.silentPush.shouldBeTrue()
    }

    @Test
    fun `content-only payload without title or body is not displayable`() {
        val payload = PushNotificationPayload.fromData(
            mapOf("customKey" to "customValue")
        )

        payload.shouldNotBeNull()
        payload.canBeDisplayed.shouldBeFalse()
    }
}
