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
 * This exception occurs if associateWebAuthnCredential is invoked on a device that was already associated for the user
 */
class WebAuthnCredentialAlreadyExistsException internal constructor(cause: Throwable?) :
    WebAuthnFailedException(
        message = "The credential is already associated with this user",
        recoverySuggestion = "Remove the old WebAuthn credential and try again",
        cause = cause
    )
