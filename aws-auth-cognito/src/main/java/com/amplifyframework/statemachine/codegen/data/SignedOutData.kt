/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

// TODO: Define Serializer for Errors
@Serializable
internal data class SignedOutData(
    val lastKnownUsername: String? = null,
    @Transient
    val hostedUIErrorData: HostedUIErrorData? = null,
    @Transient
    val globalSignOutErrorData: GlobalSignOutErrorData? = null,
    @Transient
    val revokeTokenErrorData: RevokeTokenErrorData? = null
) {
    val hasError: Boolean = hostedUIErrorData != null || globalSignOutErrorData != null || revokeTokenErrorData != null
}
