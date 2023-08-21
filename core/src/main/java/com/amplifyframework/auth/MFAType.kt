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

enum class MFAType(val value: String) {
    SMS("SMS_MFA"),
    TOTP("SOFTWARE_TOKEN_MFA");
    companion object {
        fun toMFAType(value: String): MFAType {
            return when (value) {
                MFAType.SMS.value -> MFAType.SMS
                MFAType.TOTP.value -> MFAType.TOTP
                else -> throw IllegalArgumentException("Unsupported MFA type")
            }
        }
    }
}
