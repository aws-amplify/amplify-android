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

package com.amplifyframework.pinpoint.core

import io.kotest.matchers.equals.shouldNotBeEqual
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SessionTest {
    @Test
    fun `sessions created at different times have different session IDs`() = runTest {
        val uniqueId = "id"
        val session = Session(uniqueId)
        delay(10)
        val session2 = Session(uniqueId)

        session.sessionId shouldNotBeEqual session2.sessionId
    }
}
