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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.eventenrichment.clientid.SharedPreferencesClientIdProvider
import com.amplifyframework.eventenrichment.metadata.AppMetadata
import com.amplifyframework.eventenrichment.metadata.SdkMetadata
import com.amplifyframework.foundation.result.Result
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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

        val event = (client.record("x") as Result.Success).data
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

        val event = (client.record("x") as Result.Success).data
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

        val event = (client.record("x") as Result.Success).data
        event.app.title shouldBe "My App"
    }
}
