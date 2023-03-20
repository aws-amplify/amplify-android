/*
 *  Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.pinpoint.core

import android.os.Build
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import aws.sdk.kotlin.services.pinpoint.model.EndpointRequest
import aws.sdk.kotlin.services.pinpoint.model.UpdateEndpointRequest
import aws.sdk.kotlin.services.pinpoint.model.UpdateEndpointResponse
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
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
        setup()
        targetingClient = constructTargetingClient()

        targetingClient.addAttribute("attribute", listOf("a1", "a2"))
        targetingClient.addMetric("metric", 1.0)

        val updateEndpointResponse = UpdateEndpointResponse.invoke {}
        coEvery { pinpointClient.updateEndpoint(ofType(UpdateEndpointRequest::class)) }.returns(updateEndpointResponse)
        targetingClient.updateEndpointProfile()

        coVerify {
            pinpointClient.updateEndpoint(
                coWithArg<UpdateEndpointRequest> {
                    assertNotNull(it.endpointRequest)
                    val request: EndpointRequest = it.endpointRequest!!
                    assertEquals("app id", it.applicationId)
                    assertEquals(listOf("a1", "a2"), request.attributes?.get("attribute") ?: listOf("wrong"))
                    assertEquals(1.0, request.metrics?.get("metric") ?: -1.0, 0.01)
                }
            )
        }
    }
}
