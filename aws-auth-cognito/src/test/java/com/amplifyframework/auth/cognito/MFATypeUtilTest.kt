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

package com.amplifyframework.auth.cognito

import com.amplifyframework.auth.MFAType
import kotlin.test.assertEquals
import org.junit.Test

class MFATypeUtilTest {
    @Test
    fun challengeResponse_returns_correct_sms_string() {
        assertEquals("SMS_MFA", MFAType.SMS.challengeResponse)
    }

    @Test
    fun challengeResponse_returns_correct_totp_string() {
        assertEquals("SOFTWARE_TOKEN_MFA", MFAType.TOTP.challengeResponse)
    }
}
