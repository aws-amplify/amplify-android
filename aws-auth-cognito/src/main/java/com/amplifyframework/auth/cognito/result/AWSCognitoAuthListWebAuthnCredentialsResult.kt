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

package com.amplifyframework.auth.cognito.result

import com.amplifyframework.auth.result.AuthListWebAuthnCredentialsResult
import com.amplifyframework.auth.result.AuthWebAuthnCredential
import java.time.Instant

/**
 * The cognito-specific result to the listWebAuthnCredentials API.
 * @param credentials The returned credentials
 * @param nextToken If there are multiple pages of results this will be non-null, and can be passed in the
 * options object to fetch the next page.
 */
data class AWSCognitoAuthListWebAuthnCredentialsResult(
    override val credentials: List<AuthWebAuthnCredential>,
    val nextToken: String?
) : AuthListWebAuthnCredentialsResult

internal data class CognitoWebAuthnCredential(
    override val credentialId: String,
    override val friendlyName: String?,
    override val relyingPartyId: String,
    override val createdAt: Instant
) : AuthWebAuthnCredential
