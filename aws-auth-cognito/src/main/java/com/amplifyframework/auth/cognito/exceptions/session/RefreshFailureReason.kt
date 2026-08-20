/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.exceptions.session

/**
 * Why Cognito rejected a refresh token. Cognito returns every rejection as a NotAuthorizedException
 * and distinguishes the cause only by the message text, so these values are a best-effort
 * classification of that text. [Unknown] is used for anything unrecognised, and the raw service
 * message always remains available.
 */
enum class RefreshFailureReason {
    /** The refresh token is past the validity that was in force when it was issued. */
    Expired,

    /** The refresh token was revoked, e.g. by RevokeToken or a global sign out. */
    Revoked,

    /**
     * The token is bound to a device record that could not be validated: the device was forgotten,
     * ConfirmDevice never completed, or the DeviceKey sent did not match the binding. Signing in
     * again establishes a new device binding.
     */
    DeviceBindingFailed,

    /** The token could not be validated for this app client at all. */
    Invalid,

    /** The user account is disabled. */
    UserDisabled,

    /** The user account was deleted. */
    UserDeleted,

    /** The service message was not recognised. Inspect the service message for detail. */
    Unknown
}
