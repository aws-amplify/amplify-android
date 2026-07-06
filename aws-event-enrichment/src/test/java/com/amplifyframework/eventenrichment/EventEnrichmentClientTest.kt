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

import com.amplifyframework.eventenrichment.exception.EventEnrichmentClosedException
import com.amplifyframework.eventenrichment.metadata.AppMetadata
import com.amplifyframework.eventenrichment.metadata.DeviceMetadata
import com.amplifyframework.eventenrichment.metadata.SdkMetadata
import com.amplifyframework.eventenrichment.session.SessionManager
import com.amplifyframework.eventenrichment.session.SessionState
import com.amplifyframework.foundation.result.Result
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class EventEnrichmentClientTest {

    private class CapturingSink : EventSink {
        val events = mutableListOf<EnrichedEvent>()
        override fun send(event: EnrichedEvent) {
            events += event
        }
    }

    private val sink = CapturingSink()
    private var clock = Instant.parse("2026-07-06T19:51:59.839Z")
    private var idSequence = 0

    private fun sessionManager(appId: String = "my-app") = SessionManager(
        appId = appId,
        sessionTimeout = 5.seconds,
        now = { clock },
        generateId = { "session${idSequence++}0-1111-2222-3333-444455556666" }
    )

    private fun client(
        appMetadata: AppMetadata = AppMetadata(appId = "my-app"),
        deviceMetadata: DeviceMetadata = DeviceMetadata(platform = "Android"),
        autoSessionTracking: Boolean = true,
        manager: SessionManager = sessionManager()
    ) = EventEnrichmentClient(
        appMetadata = appMetadata,
        deviceMetadata = deviceMetadata,
        sdkMetadata = SdkMetadata(name = "amplify-android", version = "2.0.0"),
        clientId = "device-uuid",
        sink = sink,
        sessionManager = manager,
        autoSessionTracking = autoSessionTracking,
        application = null,
        clock = { clock },
        generateEventId = { "event${idSequence++}" }
    )

    @Test
    fun `record returns the enriched event and forwards it to the sink`() {
        val client = client()
        val result = client.record("button_clicked")

        result.shouldBeInstanceOf<Result.Success<EnrichedEvent>>()
        result.data.eventType shouldBe "button_clicked"
        result.data.clientId shouldBe "device-uuid"
        sink.events shouldHaveSize 1
        sink.events.first().eventType shouldBe "button_clicked"
    }

    @Test
    fun `record on a closed client returns a closed failure`() {
        val client = client()
        client.close()

        val result = client.record("late_event")
        result.shouldBeInstanceOf<Result.Failure<*>>()
        result.error.shouldBeInstanceOf<EventEnrichmentClosedException>()
        // No event is emitted after close.
        sink.events.none { it.eventType == "late_event" } shouldBe true
    }

    @Test
    fun `record starts a fresh session after a stop instead of reusing the stopped one`() {
        val client = client(autoSessionTracking = true)

        val first = (client.record("first") as Result.Success).data
        val firstSessionId = first.session.id

        client.stopSession()
        clock = clock.plusMillis(1_000)

        val second = (client.record("second") as Result.Success).data

        // A new session is started, and the stopped session is not stamped
        // onto the new event.
        second.session.id shouldNotBe firstSessionId
        second.session.stopTimestamp shouldBe null
        second.session.duration shouldBe null
    }

    @Test
    fun `autoSessionTracking false defers session start until first record`() {
        val manager = sessionManager()
        val client = client(autoSessionTracking = false, manager = manager)

        manager.state shouldBe SessionState.STOPPED
        manager.session shouldBe null

        client.record("first")

        manager.state shouldBe SessionState.ACTIVE
        manager.session.shouldNotBeNull()
    }

    @Test
    fun `global attributes and metrics are stamped on every event`() {
        val client = client()
        client.addGlobalAttribute("app_theme", "dark")
        client.addGlobalMetric("battery", 0.8)

        val event = (client.record("open") as Result.Success).data
        event.attributes["app_theme"] shouldBe "dark"
        event.metrics["battery"] shouldBe 0.8
    }

    @Test
    fun `per-event fields override global fields`() {
        val client = client()
        client.addGlobalAttribute("screen", "home")

        val event = (client.record("nav", attributes = mapOf("screen" to "settings")) as Result.Success).data
        event.attributes["screen"] shouldBe "settings"
    }

    @Test
    fun `removeGlobalAttribute and removeGlobalMetric drop the field`() {
        val client = client()
        client.addGlobalAttribute("a", "1")
        client.addGlobalMetric("m", 1.0)
        client.removeGlobalAttribute("a")
        client.removeGlobalMetric("m")

        val event = (client.record("x") as Result.Success).data
        event.attributes.containsKey("a") shouldBe false
        event.metrics.containsKey("m") shouldBe false
    }

    @Test
    fun `setUserId stamps the user id on subsequent events`() {
        val client = client()
        client.setUserId("user-42")

        val event = (client.record("x") as Result.Success).data
        event.userId shouldBe "user-42"
    }

    @Test
    fun `record applies no caps to attribute and metric counts`() {
        val client = client()
        val attributes = (0 until 500).associate { "a$it" to "v$it" }
        val metrics = (0 until 500).associate { "m$it" to it.toDouble() }

        val event = (client.record("x", attributes = attributes, metrics = metrics) as Result.Success).data
        event.attributes.size shouldBe 500
        event.metrics.size shouldBe 500
    }

    @Test
    fun `close marks the client closed and clears the session`() {
        val manager = sessionManager()
        val client = client(manager = manager)
        client.record("x")

        client.close()

        client.isClosed shouldBe true
        manager.state shouldBe SessionState.STOPPED
        manager.session shouldBe null
    }
}
