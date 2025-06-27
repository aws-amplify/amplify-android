/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.sdk.appsync.core.util

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import java.util.Date
import java.util.TimeZone
import org.junit.Test

class Iso8601TimestampTest {

    @Test
    fun `returns expected timestamp format`() {
        mockkObject(Iso8601Timestamp) {
            // Wed Jun 25 2025 14:00:00.000 UTC
            val utcMillis = 1750860000000L

            // Adjust for system timezone to get equivalent UTC time
            // This allows us to compare the result string, regardless of the timezone of the system clock
            val systemOffset = TimeZone.getDefault().getOffset(utcMillis)
            val adjustedMillis = utcMillis - systemOffset

            every { Iso8601Timestamp.currentDate() } returns Date(adjustedMillis)

            val result = Iso8601Timestamp.now()
            result shouldBe "20250625T140000Z"
        }
    }
}
