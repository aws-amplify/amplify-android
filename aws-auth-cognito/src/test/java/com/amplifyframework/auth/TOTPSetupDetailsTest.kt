/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TOTPSetupDetailsTest {

    @Test
    fun getSetupURI() {
        val ss = "SS123"
        val username = "User123"
        val appName = "MyApp"
        val expectedSetupURI = "otpauth://totp/MyApp:User123?secret=SS123&issuer=MyApp"

        val actual = TOTPSetupDetails(ss, username).getSetupURI(appName)

        assertEquals(expectedSetupURI, actual.toString())
    }

    @Test
    fun getSetupURIWithAccountNameOverride() {
        val ss = "SS123"
        val username = "User123"
        val accountNameOverride = "AccountOverride"
        val appName = "MyApp"
        val expectedSetupURI = "otpauth://totp/MyApp:AccountOverride?secret=SS123&issuer=MyApp"

        val actual = TOTPSetupDetails(ss, username).getSetupURI(appName, accountNameOverride)

        assertEquals(expectedSetupURI, actual.toString())
    }
}
