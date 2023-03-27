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

package com.amplifyframework.pinpoint.core.util

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import junit.framework.TestCase
import org.junit.Before
import org.junit.Test

class SharedPrefsUniqueIdTest : TestCase() {
    private lateinit var sharedPreferences: SharedPreferences

    @Before
    override fun setUp() {
        sharedPreferences = mockk()
        mockkStatic("com.amplifyframework.pinpoint.core.util.SharedPreferencesUtilKt")
    }

    @Test
    fun testGetUniqueIdWhenNotStored() {
        val slot = slot<String>()
        every { sharedPreferences.getString(any(), any()) } returns null
        every { sharedPreferences.putString(any(), capture(slot)) } returns Unit
        assertEquals(sharedPreferences.getUniqueId(), slot.captured)
    }

    @Test
    fun testGetUniqueIdWhenStored() {
        val uuid = "RANDOM_UUID"
        every { sharedPreferences.getString(any(), any()) } returns uuid
        assertEquals(sharedPreferences.getUniqueId(), uuid)
    }
}
