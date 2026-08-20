/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.testutils

import com.amplifyframework.api.graphql.GraphQLBehavior
import com.amplifyframework.api.graphql.SimpleGraphQLRequest
import com.amplifyframework.datastore.generated.model.MfaInfo
import com.amplifyframework.testutils.Assets
import com.amplifyframework.testutils.api.SubscriptionHolder
import com.amplifyframework.testutils.api.subscribe
import com.amplifyframework.testutils.coroutines.blockingAwait
import com.amplifyframework.testutils.coroutines.runBlockingWithTimeout
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow

// Creates the subscription to receive MFA codes.
fun GraphQLBehavior.createMfaSubscription(): SubscriptionHolder<MfaInfo> = subscribe(
    SimpleGraphQLRequest(
        Assets.readAsString("create-mfa-subscription.graphql"),
        MfaInfo::class.java,
        null
    )
)

// Block until the subscription has been established
fun SubscriptionHolder<MfaInfo>.blockUntilEstablished() {
    subscriptionEstablished.blockingAwait(
        timeout = 10.seconds,
        message = "Timed out waiting for subscription to be established"
    )
}

// Blocks until an MFA code is received for the given username
fun SubscriptionHolder<MfaInfo>.blockForCode(username: String): String = runBlockingWithTimeout(
    timeout = 30.seconds,
    message = "Timed out while waiting to receive OTP code"
) {
    val info = dataChannel.receiveAsFlow().first { it.username == username }
    info.code
}
