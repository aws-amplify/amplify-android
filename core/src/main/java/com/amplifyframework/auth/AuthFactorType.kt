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
package com.amplifyframework.auth

/**
 * Enumeration of the possible mechanisms that can be used to authenticate a user when signing in with USER_AUTH.
 * @param challengeResponse The response that should be sent to select a factor during the
 *                          CONTINUE_SIGN_IN_WITH_FIRST_FACTOR_SELECTION step.
 */
enum class AuthFactorType(val challengeResponse: String) {
    /**
     * Sign in using the user's password
     */
    PASSWORD("PASSWORD"),

    /**
     * Sign in using the user's password via a Secure Remote Password flow
     */
    PASSWORD_SRP("PASSWORD_SRP"),

    /**
     * Sign in using a One Time Password sent to the user's email address
     */
    EMAIL_OTP("EMAIL_OTP"),

    /**
     * Sign in using a One Time Password sent to the user's SMS number
     */
    SMS_OTP("SMS_OTP"),

    /**
     * Sign in with WebAuthn (i.e. PassKey)
     */
    WEB_AUTHN("WEB_AUTHN")
}
