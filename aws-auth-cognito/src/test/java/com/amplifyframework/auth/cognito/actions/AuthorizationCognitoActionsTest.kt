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

package com.amplifyframework.auth.cognito.actions

import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.auth.cognito.AWSCognitoAuthService
import com.amplifyframework.auth.cognito.AuthConfiguration
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.StoreClientBehavior
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.EventDispatcher
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.IdentityPoolConfiguration
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent
import com.amplifyframework.statemachine.codegen.events.FetchAuthSessionEvent
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthorizationCognitoActionsTest {

    private val userPool = mockk<UserPoolConfiguration> {
        every { poolId } returns "pool_id"
    }
    private val logger = mockk<Logger>(relaxed = true)
    private val capturedEvent = slot<StateMachineEvent>()
    private val dispatcher = mockk<EventDispatcher> {
        every { send(capture(capturedEvent)) } just Runs
    }

    private fun authEnvironment(identityPoolId: String?): AuthEnvironment {
        val identityPool = identityPoolId?.let {
            mockk<IdentityPoolConfiguration> { every { poolId } returns it }
        }
        val configuration = mockk<AuthConfiguration> {
            every { this@mockk.userPool } returns this@AuthorizationCognitoActionsTest.userPool
            every { this@mockk.identityPool } returns identityPool
        }
        return AuthEnvironment(
            ApplicationProvider.getApplicationContext(),
            configuration,
            mockk<AWSCognitoAuthService>(relaxed = true),
            mockk<StoreClientBehavior>(relaxed = true),
            null,
            null,
            logger
        )
    }

    @Test
    fun `initializeFetchUnAuthSession reports signed out when no identity pool is configured`() = runTest {
        // A user pool only configuration is valid, so a signed-out user must not be told the app is
        // misconfigured. Reporting a ConfigurationException here surfaced to callers as an opaque
        // UnknownException("Fetch auth session failed.") instead of a signed-out session.
        AuthorizationCognitoActions.initializeFetchUnAuthSession()
            .execute(dispatcher, authEnvironment(identityPoolId = null))

        val event = capturedEvent.captured
        event.shouldBeInstanceOf<AuthorizationEvent>()
        val eventType = event.eventType
        eventType.shouldBeInstanceOf<AuthorizationEvent.EventType.ThrowError>()
        eventType.exception.shouldBeInstanceOf<SignedOutException>()
    }

    @Test
    fun `initializeFetchUnAuthSession fetches identity when an identity pool is configured`() = runTest {
        AuthorizationCognitoActions.initializeFetchUnAuthSession()
            .execute(dispatcher, authEnvironment(identityPoolId = "identity_pool_id"))

        val event = capturedEvent.captured
        event.shouldBeInstanceOf<FetchAuthSessionEvent>()
        event.eventType.shouldBeInstanceOf<FetchAuthSessionEvent.EventType.FetchIdentity>()
    }
}
