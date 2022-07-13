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
package com.amplifyframework.analytics.pinpoint.models

import junit.framework.TestCase

class PinpointEventTest : TestCase() {

    fun testToJsonObject() {
        val expectedOutput =
            "{\"eventId\":\"c175d759-3a90-44be-ab51-888ce43ed527\",\"eventType\":\"EVENT_TYPE\"," +
                "\"attributes\":{\"attribute1\":\"value1\",\"attribute2\":\"value2\"}," +
                "\"metrics\":{\"metric1\":1.0,\"metric2\":2.0},\"sdkInfo\":{\"name\":\"amplify-test\"," +
                "\"version\":\"1.0\"},\"pinpointSession\":{\"sessionId\":\"SESSION_ID\"," +
                "\"sessionStart\":1657035956917},\"eventTimestamp\":1657035956917,\"uniqueId\":\"UNIQUE_ID\"," +
                "\"androidAppDetails\":{\"appId\":\"appId\",\"appTitle\":\"appTitle\"," +
                "\"packageName\":\"packageName\",\"versionCode\":\"versionCode\",\"versionName\":\"versionName\"}," +
                "\"androidDeviceDetails\":{\"platform\":\"ANDROID\",\"locale\":\"en_US\"}}"
        val pinpointEvent = PinpointEvent(
            eventId = "c175d759-3a90-44be-ab51-888ce43ed527",
            eventType = "EVENT_TYPE",
            attributes = mapOf("attribute1" to "value1", "attribute2" to "value2"),
            metrics = mapOf("metric1" to 1.0, "metric2" to 2.0),
            sdkInfo = SDKInfo("amplify-test", "1.0"),
            pinpointSession = PinpointSession("SESSION_ID", 1657035956917L),
            eventTimestamp = 1657035956917L,
            uniqueId = "UNIQUE_ID",
            androidAppDetails = AndroidAppDetails("appId", "appTitle", "packageName", "versionCode", "versionName"),
            androidDeviceDetails = AndroidDeviceDetails()
        )
        assertEquals(expectedOutput, pinpointEvent.toJsonString())
    }
}
