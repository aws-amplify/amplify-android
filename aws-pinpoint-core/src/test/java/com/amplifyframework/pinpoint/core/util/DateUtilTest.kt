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

import junit.framework.TestCase

class DateUtilTest : TestCase() {
    fun testIsoDateFromMillis() {
        val millis1 = 1660082735587
        assertEquals("2022-08-09T22:05:35.587Z", millis1.millisToIsoDate())
        val millis2 = 0L
        assertEquals("1970-01-01T00:00:00.000Z", millis2.millisToIsoDate())
    }
}
