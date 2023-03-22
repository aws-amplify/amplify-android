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

package com.amplifyframework.pinpoint.core.endpointProfile

import com.amplifyframework.pinpoint.core.constructEndpointProfile
import com.amplifyframework.pinpoint.core.effectiveDate
import com.amplifyframework.pinpoint.core.util.millisToIsoDate
import junit.framework.TestCase

class EndpointProfileTest : TestCase() {
    private lateinit var endpointProfile: EndpointProfile

    override fun setUp() {
        endpointProfile = constructEndpointProfile()
    }

    fun `test serialization`() {
        val expected =
            "{\"Address\":\"\",\"ApplicationId\":\"app id\",\"EndpointId\":\"unique-id\"," +
                "\"Location\":\"{\\\"country\\\":\\\"en_US\\\"}\",\"Demographic\":\"{\\\"appVersion\\\":" +
                "\\\"1.0.0\\\",\\\"make\\\":\\\"TEST MANUFACTURER\\\",\\\"locale\\\":\\\"en_US\\\"}\"," +
                "\"EffectiveDate\":\"${effectiveDate.millisToIsoDate()}\",\"User\":\"{}\"}"
        assertEquals(expected, endpointProfile.toString())
    }

    fun `test metrics`() {
        val metric1Name = "metric1" + "_".repeat(100)
        endpointProfile.addMetric(metric1Name, 1.0)
        endpointProfile.addMetric("metric2", 2.0)
        endpointProfile.addMetric("metric3", -3.0)
        endpointProfile = endpointProfile.withMetric("metric4", 4.0)

        assertTrue(endpointProfile.hasMetric("metric3"))
        assertEquals(-3.0, endpointProfile.getMetric("metric3"))

        val metric1NameTrimmed = metric1Name.take(50)
        assertTrue(endpointProfile.hasMetric(metric1NameTrimmed))
        endpointProfile.addMetric(metric1NameTrimmed, null)
        assertFalse(endpointProfile.hasMetric(metric1NameTrimmed))
        assertNull(endpointProfile.getMetric(metric1NameTrimmed))

        assertEquals(3, endpointProfile.allMetrics.size)
        assertEquals(4.0, endpointProfile.allMetrics["metric4"])

        val expected =
            "{\"Address\":\"\",\"ApplicationId\":\"app id\",\"EndpointId\":\"unique-id\"," +
                "\"Location\":\"{\\\"country\\\":\\\"en_US\\\"}\",\"Demographic\":\"{\\\"appVersion\\\":" +
                "\\\"1.0.0\\\",\\\"make\\\":\\\"TEST MANUFACTURER\\\",\\\"locale\\\":\\\"en_US\\\"}\"," +
                "\"EffectiveDate\":\"${effectiveDate.millisToIsoDate()}\"," +
                "\"Metrics\":{\"metric2\":2.0,\"metric3\":-3.0,\"metric4\":4.0},\"User\":\"{}\"}"
        assertEquals(expected, endpointProfile.toString())

        for (i in 1..100) {
            endpointProfile.addMetric(i.toString(), 0.0)
        }
        assertEquals(20, endpointProfile.allMetrics.size)
    }

    fun `test attributes`() {
        val attribute1Name = "attribute1" + "_".repeat(100)
        endpointProfile.addAttribute(attribute1Name, listOf("a", "b", "c"))
        endpointProfile.addAttribute("attribute2", listOf("d", "e", "f"))
        endpointProfile.addAttribute("attribute3", listOf(""))
        endpointProfile = endpointProfile.withAttribute("attribute4", listOf("g", "h", "i"))

        assertTrue(endpointProfile.hasAttribute("attribute3"))
        assertEquals(listOf(""), endpointProfile.getAttribute("attribute3"))

        val attribute1NameTrimmed = attribute1Name.take(50)
        assertTrue(endpointProfile.hasAttribute(attribute1NameTrimmed))
        endpointProfile.addAttribute(attribute1NameTrimmed, null)
        assertFalse(endpointProfile.hasAttribute(attribute1NameTrimmed))
        assertNull(endpointProfile.getAttribute(attribute1NameTrimmed))

        assertEquals(3, endpointProfile.allAttributes.size)
        assertEquals(listOf("g", "h", "i"), endpointProfile.allAttributes["attribute4"])

        println(endpointProfile.toString())
        val expected =
            "{\"Address\":\"\",\"ApplicationId\":\"app id\",\"EndpointId\":\"unique-id\"," +
                "\"Location\":\"{\\\"country\\\":\\\"en_US\\\"}\",\"Demographic\":\"{\\\"appVersion\\\":" +
                "\\\"1.0.0\\\",\\\"make\\\":\\\"TEST MANUFACTURER\\\",\\\"locale\\\":\\\"en_US\\\"}\"," +
                "\"EffectiveDate\":\"${effectiveDate.millisToIsoDate()}\"," +
                "\"Attributes\":{\"attribute4\":[\"g\",\"h\",\"i\"],\"attribute3\":[\"\"]," +
                "\"attribute2\":[\"d\",\"e\",\"f\"]},\"User\":\"{}\"}"
        assertEquals(expected, endpointProfile.toString())

        for (i in 1..100) {
            endpointProfile.addAttribute(i.toString(), listOf(""))
        }
        assertEquals(20, endpointProfile.allAttributes.size)
    }
}
