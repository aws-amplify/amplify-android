/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.apollo.appsync.util

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import java.util.Date
import org.junit.Ignore
import org.junit.Test

class Iso8601TimestampTest {
    @Test
    @Ignore("Not passing on GitHub Runner")
    fun `returns expected timestamp format`() {
        mockkObject(Iso8601Timestamp) {
            // July 17 2024, 12:00:00
            every { Iso8601Timestamp.currentDate() } returns Date(1721228400000L)

            val result = Iso8601Timestamp.now()
            result shouldBe "20240717T120000Z"
        }
    }
}
