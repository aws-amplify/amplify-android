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

package com.amplifyframework.storage.s3.transfer

import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TransferDBTest {

    @After
    fun teardown() {
        // Clear instance
        TransferDB.instance = null
    }

    @Test
    fun `getInstance returns the same object`() {
        val context = RuntimeEnvironment.getApplication()

        val db1 = TransferDB.getInstance(context)
        val db2 = TransferDB.getInstance(context)

        db1 shouldBeSameInstanceAs db2
    }
}
