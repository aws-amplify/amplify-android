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

package com.amplifyframework.analytics.pinpoint.internal.core.idresolver

import android.content.SharedPreferences
import com.amplifyframework.analytics.pinpoint.internal.core.util.putString
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase
import org.junit.Before

class SharedPrefsUniqueIdServiceTest : TestCase() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var uniqueIdService: SharedPrefsUniqueIdService

    @Before
    override fun setUp() {
        sharedPreferences = mockk()
        uniqueIdService = SharedPrefsUniqueIdService(sharedPreferences)
    }

    fun testGetUniqueIdWhenNotStored() {
        val slot = slot<String>()
        every { sharedPreferences.getString(any(), any()) } returns null
        every { sharedPreferences.putString(any(), capture(slot)) }
        assertEquals(uniqueIdService.getUniqueId(), slot.captured)
    }

    fun testGetUniqueIdWhenStored() {
        val uuid = "RANDOM_UUID"
        every { sharedPreferences.getString(any(), any()) } returns uuid
        assertEquals(uniqueIdService.getUniqueId(), uuid)
    }
}
