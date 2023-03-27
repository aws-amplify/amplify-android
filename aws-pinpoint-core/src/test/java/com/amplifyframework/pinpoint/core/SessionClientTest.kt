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

import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.pinpoint.core.util.getUniqueId
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verifyOrder
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionClientTest {

    private val analyticsClientMock = mockk<AnalyticsClient>(relaxed = true)
    private val targetingClientMock = mockk<TargetingClient>(relaxed = true)
    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private lateinit var sessionClient: SessionClient

    @Before
    fun setup() {
        mockkStatic("com.amplifyframework.pinpoint.core.util.SharedPreferencesUtilKt")
        every { sharedPreferences.getUniqueId() }.answers { "UNIQUE_ID" }

        sessionClient = SessionClient(
            ApplicationProvider.getApplicationContext(),
            targetingClientMock,
            sharedPreferences.getUniqueId(),
            analyticsClientMock
        )
    }

    @Test
    fun `test session start is recorded`() {
        sessionClient.startSession()
        Assert.assertNotNull(sessionClient.session)
        Assert.assertTrue(!sessionClient.session!!.isPaused())
        verifyOrder {
            analyticsClientMock.createEvent("_session.start", any(), any(), any(), any(), any(), any(), any(), any())
            analyticsClientMock.recordEvent(any())
        }
        Assert.assertEquals(SessionClient.SessionState.ACTIVE, sessionClient.getSessionState())
    }

    @Test
    fun `test session stop is recorded`() {
        sessionClient.startSession()
        sessionClient.stopSession()
        Assert.assertNull(sessionClient.session)
        verifyOrder {
            analyticsClientMock.createEvent("_session.start", any(), any(), any(), any(), any(), any(), any(), any())
            analyticsClientMock.recordEvent(any())
            analyticsClientMock.createEvent("_session.stop", any(), any(), any(), any(), any(), any(), any(), any())
            analyticsClientMock.recordEvent(any())
        }
        Assert.assertNull(sessionClient.session)
        Assert.assertEquals(SessionClient.SessionState.INACTIVE, sessionClient.getSessionState())
    }
}
