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
package com.amplifyframework.statemachine.codegen.data

internal data class SignInTOTPSetupData(
    val secretCode: String,
    val session: String?,
    val username: String
) {
    override fun toString(): String {
        return "SignInTOTPSetupData(" +
            "secretCode = ${mask(secretCode)}, " +
            "session = ${mask(session)}, " +
            "username = $username}" +
            ")"
    }

    private fun mask(value: String?): String {
        return if (value == null || value.length <= 4) {
            "***"
        } else {
            "${value.substring(0 until 4)}***"
        }
    }
}
