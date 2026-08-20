/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.eventenrichment

import com.amplifyframework.eventenrichment.metadata.AppMetadata
import com.amplifyframework.eventenrichment.metadata.DeviceMetadata
import com.amplifyframework.eventenrichment.metadata.SdkMetadata
import com.amplifyframework.eventenrichment.session.Session
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class EnrichedEventTest {

    private fun event(
        eventType: String = "button_clicked",
        eventTimestamp: Long = 1_700_000_000_000L,
        session: Session = Session(
            id = "my-app12-abcdef12-20260706-195159839",
            startTimestamp = "2026-07-06T19:51:59.839Z"
        ),
        attributes: Map<String, String> = emptyMap(),
        metrics: Map<String, Double> = emptyMap(),
        device: DeviceMetadata = DeviceMetadata(),
        app: AppMetadata = AppMetadata(appId = "my-app"),
        sdk: SdkMetadata = SdkMetadata(name = "amplify-android", version = "2.0.0"),
        clientId: String = "device-uuid",
        userId: String? = null
    ) = EnrichedEvent(
        eventId = "event-uuid",
        eventType = eventType,
        eventTimestamp = eventTimestamp,
        session = session,
        attributes = attributes,
        metrics = metrics,
        device = device,
        app = app,
        sdk = sdk,
        clientId = clientId,
        userId = userId
    )

    @Test
    fun `toJson emits the full 3_1 envelope with all fields`() {
        val json = event(
            attributes = mapOf("screen" to "home"),
            metrics = mapOf("load_time" to 2.5),
            device = DeviceMetadata(
                platform = "Android",
                platformVersion = "14",
                manufacturer = "Google",
                model = "Pixel",
                locale = "en_US"
            ),
            app = AppMetadata(
                appId = "my-app",
                packageName = "com.example.app",
                versionName = "1.0",
                versionCode = "42",
                title = "Example"
            ),
            userId = "user-1"
        ).toJson()

        json shouldEqualJson """
            {
              "event_type": "button_clicked",
              "event_timestamp": 1700000000000,
              "arrival_timestamp": 1700000000000,
              "event_version": "3.1",
              "application": {
                "app_id": "my-app",
                "package_name": "com.example.app",
                "version_name": "1.0",
                "version_code": "42",
                "title": "Example",
                "sdk": { "name": "amplify-android", "version": "2.0.0" }
              },
              "client": { "client_id": "device-uuid", "user_id": "user-1" },
              "device": {
                "platform": { "name": "Android", "version": "14" },
                "make": "Google",
                "model": "Pixel",
                "locale": { "code": "en_US" }
              },
              "session": {
                "id": "my-app12-abcdef12-20260706-195159839",
                "start_timestamp": "2026-07-06T19:51:59.839Z"
              },
              "attributes": { "screen": "home" },
              "metrics": { "load_time": 2.5 }
            }
        """.trimIndent()
    }

    @Test
    fun `arrival_timestamp mirrors event_timestamp`() {
        val obj = Json.parseToJsonElement(event(eventTimestamp = 123456789L).toJson()).jsonObject
        obj["event_timestamp"] shouldBe JsonPrimitive(123456789L)
        obj["arrival_timestamp"] shouldBe JsonPrimitive(123456789L)
    }

    @Test
    fun `device is always present even when empty`() {
        val obj = Json.parseToJsonElement(event(device = DeviceMetadata()).toJson()).jsonObject
        obj.containsKey("device") shouldBe true
        obj.getValue("device").jsonObject.size shouldBe 0
    }

    @Test
    fun `platform sub-object is omitted when platform fields are null`() {
        val obj = Json.parseToJsonElement(
            event(device = DeviceMetadata(manufacturer = "Google", model = "Pixel")).toJson()
        ).jsonObject
        val device = obj.getValue("device").jsonObject
        device.containsKey("platform") shouldBe false
        device.getValue("make").jsonPrimitive.content shouldBe "Google"
        device.getValue("model").jsonPrimitive.content shouldBe "Pixel"
    }

    @Test
    fun `platform sub-object is present when only platform name is set`() {
        val obj = Json.parseToJsonElement(
            event(device = DeviceMetadata(platform = "Android")).toJson()
        ).jsonObject
        val platform = obj.getValue("device").jsonObject.getValue("platform").jsonObject
        platform.getValue("name").jsonPrimitive.content shouldBe "Android"
        platform.containsKey("version") shouldBe false
    }

    @Test
    fun `locale sub-object is omitted when locale is null`() {
        val obj = Json.parseToJsonElement(
            event(device = DeviceMetadata(platform = "Android")).toJson()
        ).jsonObject
        obj.getValue("device").jsonObject.containsKey("locale") shouldBe false
    }

    @Test
    fun `optional application fields are omitted when null`() {
        val obj = Json.parseToJsonElement(event(app = AppMetadata(appId = "my-app")).toJson()).jsonObject
        val application = obj.getValue("application").jsonObject
        application.getValue("app_id").jsonPrimitive.content shouldBe "my-app"
        application.containsKey("package_name") shouldBe false
        application.containsKey("version_name") shouldBe false
        application.containsKey("version_code") shouldBe false
        application.containsKey("title") shouldBe false
        application.containsKey("sdk") shouldBe true
    }

    @Test
    fun `user_id is omitted when null`() {
        val obj = Json.parseToJsonElement(event(userId = null).toJson()).jsonObject
        val client = obj.getValue("client").jsonObject
        client.getValue("client_id").jsonPrimitive.content shouldBe "device-uuid"
        client.containsKey("user_id") shouldBe false
    }

    @Test
    fun `empty attributes and metrics maps are omitted`() {
        val obj = Json.parseToJsonElement(event().toJson()).jsonObject
        obj.containsKey("attributes") shouldBe false
        obj.containsKey("metrics") shouldBe false
    }

    @Test
    fun `session stop_timestamp and duration are present when set`() {
        val obj = Json.parseToJsonElement(
            event(
                session = Session(
                    id = "sid",
                    startTimestamp = "2026-07-06T19:51:59.839Z",
                    stopTimestamp = "2026-07-06T19:52:09.839Z",
                    duration = 10_000L
                )
            ).toJson()
        ).jsonObject
        val session = obj.getValue("session").jsonObject
        session.getValue("stop_timestamp").jsonPrimitive.content shouldBe "2026-07-06T19:52:09.839Z"
        session.getValue("duration") shouldBe JsonPrimitive(10_000L)
    }

    @Test
    fun `session stop_timestamp and duration are omitted while active`() {
        val obj = Json.parseToJsonElement(event().toJson()).jsonObject
        val session = obj.getValue("session").jsonObject
        session.containsKey("stop_timestamp") shouldBe false
        session.containsKey("duration") shouldBe false
    }

    @Test
    fun `no caps are applied to attribute or metric count`() {
        val attributes = (0 until 1_000).associate { "attr_$it" to "value_$it" }
        val metrics = (0 until 1_000).associate { "metric_$it" to it.toDouble() }

        val obj = Json.parseToJsonElement(event(attributes = attributes, metrics = metrics).toJson()).jsonObject
        obj.getValue("attributes").jsonObject.size shouldBe 1_000
        obj.getValue("metrics").jsonObject.size shouldBe 1_000
    }

    @Test
    fun `no caps are applied to attribute value length`() {
        val longValue = "x".repeat(50_000)
        val obj = Json.parseToJsonElement(event(attributes = mapOf("big" to longValue)).toJson()).jsonObject
        obj.getValue("attributes").jsonObject.getValue("big").jsonPrimitive.content shouldBe longValue
    }

    @Test
    fun `event_version is 3_1`() {
        val obj: JsonObject = Json.parseToJsonElement(event().toJson()).jsonObject
        obj.getValue("event_version").jsonPrimitive.content shouldBe "3.1"
    }
}
