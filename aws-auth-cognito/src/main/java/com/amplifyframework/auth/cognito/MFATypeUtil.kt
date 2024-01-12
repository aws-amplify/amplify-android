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

@file:JvmName("MFATypeUtil")

package com.amplifyframework.auth.cognito

import com.amplifyframework.auth.MFAType

/**
 * Returns the cognito-specific string to pass to Amplify.Auth.confirmSignIn for a specific [MFAType] when making
 * an MFA selection during the sign-in process.
 */
val MFAType.challengeResponse: String
    get() = when (this) {
        MFAType.SMS -> "SMS_MFA"
        MFAType.TOTP -> "SOFTWARE_TOKEN_MFA"
    }
