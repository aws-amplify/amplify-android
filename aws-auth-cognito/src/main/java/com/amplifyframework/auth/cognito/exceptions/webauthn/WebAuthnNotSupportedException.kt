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

package com.amplifyframework.auth.cognito.exceptions.webauthn

/**
 * Exception that is thrown because WebAuthn is not supported on the device. This indicates that either the device
 * did not ship with WebAuthn support, or that your application is missing a required dependency or service.
 */
class WebAuthnNotSupportedException internal constructor(cause: Throwable?) :
    WebAuthnFailedException(
        message = "WebAuthn is not supported on this device",
        recoverySuggestion = TODO_RECOVERY_SUGGESTION,
        cause = cause
    )
