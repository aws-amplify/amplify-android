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

import com.amplifyframework.auth.result.AuthSessionResult

abstract class AWSAuthSessionBehavior<TokensType>(
    isSignedIn: Boolean,
    open val identityIdResult: AuthSessionResult<String>,
    open val awsCredentialsResult: AuthSessionResult<AWSCredentials>,
    open val userSubResult: AuthSessionResult<String>,
    val tokensResult: AuthSessionResult<TokensType>
) : AuthSession(isSignedIn) {
    abstract val accessToken: String?
}
