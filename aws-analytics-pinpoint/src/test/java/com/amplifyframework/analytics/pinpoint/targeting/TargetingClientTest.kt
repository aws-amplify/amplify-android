/*
 *  Copyright 2016-2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package com.amplifyframework.analytics.pinpoint.targeting

import aws.sdk.kotlin.services.pinpoint.PinpointClient
import aws.sdk.kotlin.services.pinpoint.model.UpdateEndpointRequest
import aws.sdk.kotlin.services.pinpoint.model.UpdateEndpointResponse
import com.amplifyframework.analytics.pinpoint.targeting.endpointProfile.EndpointProfile
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TargetingClientTest {
    private lateinit var pinpointClient: PinpointClient
    private lateinit var targetingClient: TargetingClient

    @Before
    fun setUp() {
        pinpointClient = constructPinpointClient()
        targetingClient = constructTargetingClient()
    }

    @Test
    fun testCurrentEndpoint() {
        targetingClient.addAttribute("attribute", listOf("a", "b", "c"))
        targetingClient.addMetric("metric", 2.0)
        val endpoint = targetingClient.currentEndpoint()
        assertEquals(endpoint.getAttribute("attribute"), listOf("a", "b", "c"))
        assertEquals(endpoint.getMetric("metric"), 2.0)
    }

    @Test
    fun testUpdateEndpointProfile() = runTest {
        val updateEndpointResponse = UpdateEndpointResponse.invoke{}
        coEvery { pinpointClient.updateEndpoint(ofType(UpdateEndpointRequest::class)) }.returns(updateEndpointResponse)
        val updateEndpointRequest =
            UpdateEndpointRequest.invoke {}
        mockkObject(UpdateEndpointRequest.Companion)
        every { UpdateEndpointRequest.Companion.invoke(any()) }.returns(updateEndpointRequest)
        targetingClient.updateEndpointProfile()
        coVerify { pinpointClient.updateEndpoint(updateEndpointRequest) }
    }
}