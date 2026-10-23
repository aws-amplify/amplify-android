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

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.Context
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.eventenrichment.clientid.SharedPreferencesClientIdProvider
import com.amplifyframework.eventenrichment.metadata.AppMetadata
import com.amplifyframework.eventenrichment.metadata.SdkMetadata
import com.amplifyframework.eventenrichment.session.SessionState
import com.amplifyframework.foundation.result.get
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class EventEnrichmentClientAutoResolveTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val sdkMetadata = SdkMetadata(name = "amplify-android", version = "2.0.0")

    private fun options(autoSessionTracking: Boolean = false) = EventEnrichmentClientOptions {
        this.autoSessionTracking = autoSessionTracking
    }

    @Test
    fun `auto-resolves real device metadata rather than shipping empty device info`() {
        val client = EventEnrichmentClient(
            context = context,
            appId = "my-app",
            sdkMetadata = sdkMetadata,
            options = options()
        )

        val event = client.record("x").get()
        event.device.platform shouldBe "Android"
    }

    @Test
    fun `auto-resolves the client id from the shared SharedPreferences key`() {
        val client = EventEnrichmentClient(
            context = context,
            appId = "my-app",
            sdkMetadata = sdkMetadata,
            options = options()
        )

        val event = client.record("x").get()
        val stored = context
            .getSharedPreferences(SharedPreferencesClientIdProvider.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(SharedPreferencesClientIdProvider.CLIENT_ID_STORAGE_KEY, null)

        event.clientId shouldBe stored
    }

    @Test
    fun `throws when appMetadata appId does not match the appId`() {
        shouldThrow<IllegalArgumentException> {
            EventEnrichmentClient(
                context = context,
                appId = "my-app",
                sdkMetadata = sdkMetadata,
                options = options(),
                appMetadata = AppMetadata(appId = "different-app")
            )
        }
    }

    @Test
    fun `accepts a matching appMetadata`() {
        val client = EventEnrichmentClient(
            context = context,
            appId = "my-app",
            sdkMetadata = sdkMetadata,
            options = options(),
            appMetadata = AppMetadata(appId = "my-app", title = "My App")
        )

        val event = client.record("x").get()
        event.app.title shouldBe "My App"
    }

    @Test
    fun `launch session start carries the initial user id and global fields`() {
        val received = CountDownLatch(1)
        var start: EnrichedEvent? = null
        val client = EventEnrichmentClient(
            context = context,
            appId = "my-app",
            sdkMetadata = sdkMetadata,
            options = EventEnrichmentClientOptions {
                initialUserId = "user-1"
                initialGlobalAttributes = mapOf("theme" to "dark")
                initialGlobalMetrics = mapOf("battery" to 0.5)
            },
            sink = EventSink { event ->
                if (start == null) start = event
                received.countDown()
            }
        )

        received.await(5, TimeUnit.SECONDS) shouldBe true
        val event = start!!
        event.eventType shouldBe "_session.start"
        event.userId shouldBe "user-1"
        event.attributes shouldBe mapOf("theme" to "dark")
        event.metrics shouldBe mapOf("battery" to 0.5)
        client.close()
    }

    @Test
    fun `launch session starts paused when the process has no visible activity`() {
        launchStateFor(RunningAppProcessInfo.IMPORTANCE_CACHED) shouldBe SessionState.PAUSED
    }

    @Test
    fun `launch session starts paused when only a foreground service is running`() {
        launchStateFor(RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE) shouldBe SessionState.PAUSED
    }

    @Test
    fun `launch session starts active when an activity is in the foreground`() {
        launchStateFor(RunningAppProcessInfo.IMPORTANCE_FOREGROUND) shouldBe SessionState.ACTIVE
    }

    @Test
    fun `launch session starts active when an activity is visible`() {
        launchStateFor(RunningAppProcessInfo.IMPORTANCE_VISIBLE) shouldBe SessionState.ACTIVE
    }

    private fun launchStateFor(importance: Int): SessionState {
        val process = RunningAppProcessInfo().apply {
            pid = Process.myPid()
            this.importance = importance
        }
        shadowOf(context.getSystemService(ActivityManager::class.java)).setProcesses(listOf(process))
        val client = EventEnrichmentClient(
            context = context,
            appId = "my-app",
            sdkMetadata = sdkMetadata,
            options = options(autoSessionTracking = true)
        )
        val state = client.sessionManager.state
        client.close()
        return state
    }
}
