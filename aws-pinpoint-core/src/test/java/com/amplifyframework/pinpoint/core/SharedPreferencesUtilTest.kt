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

package com.amplifyframework.pinpoint.core

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amplifyframework.pinpoint.core.util.putString
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class SharedPreferencesUtilTest : TestCase() {
    @Test
    fun testPutString() {
        val sharedPreferences = constructSharedPreferences()
        sharedPreferences.putString("key", "value")
        assertEquals("value", sharedPreferences.getString("key", "not found"))
    }
}
