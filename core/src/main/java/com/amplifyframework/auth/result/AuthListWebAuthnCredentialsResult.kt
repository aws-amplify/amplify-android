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

package com.amplifyframework.auth.result

import java.time.Instant

/**
 * A WebAuthn credential associated with the user's account
 */
interface AuthWebAuthnCredential {
    /**
     * The identifier for the credential
     */
    val credentialId: String

    /**
     * The user-readable credential name
     */
    val friendlyName: String?

    /**
     * The ID of the Relying Party used when registering the passkey
     */
    val relyingPartyId: String

    /**
     * When the credential was registered
     */
    val createdAt: Instant
}

/**
 * The result returned from the listWebAuthnCredentials API
 */
interface AuthListWebAuthnCredentialsResult {
    /**
     * The returned credentials
     */
    val credentials: List<AuthWebAuthnCredential>
}
