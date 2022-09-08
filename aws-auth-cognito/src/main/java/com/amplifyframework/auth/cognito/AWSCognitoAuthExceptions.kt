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

package com.amplifyframework.auth.cognito

import com.amplifyframework.auth.AuthException

class RevokeTokenException(exception: Exception) : AuthException(
    "Failed to revoke token",
    exception,
    "See attached exception for more details. RevokeToken can be retried using the CognitoIdentityProviderClient " +
        "accessible from the escape hatch."
)

class GlobalSignOutException(exception: Exception) : AuthException(
    "Failed to sign out globally",
    exception,
    "See attached exception for more details. GlobalSignOut can be retried using the CognitoIdentityProviderClient " +
        "accessible from the escape hatch."
)
