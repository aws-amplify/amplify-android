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

package com.amplifyframework.auth.cognito.usecases

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GetUserResponse
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FetchMfaPreferenceUseCaseTest {

    private val client: CognitoIdentityProviderClient = mockk()
    private val fetchAuthSession: FetchAuthSessionUseCase = mockk {
        coEvery { execute().accessToken } returns "access token"
    }
    private val stateMachine: AuthStateMachine = mockk {
        coEvery { getCurrentState().authNState } returns AuthenticationState.SignedIn(mockk(), mockk())
    }

    private val useCase = FetchMfaPreferenceUseCase(
        client = client,
        fetchAuthSession = fetchAuthSession,
        stateMachine = stateMachine
    )

    @Test
    fun `returns mfa preferences`() = runTest {
        coEvery { client.getUser(any()) } returns GetUserResponse {
            userMfaSettingList = listOf("SMS_MFA", "SOFTWARE_TOKEN_MFA")
            preferredMfaSetting = "SOFTWARE_TOKEN_MFA"
            userAttributes = listOf()
            username = ""
        }

        val result = useCase.execute()

        result.enabled shouldBe setOf(MFAType.SMS, MFAType.TOTP)
        result.preferred shouldBe MFAType.TOTP
    }

    @Test
    fun `throws exception if not in signed in state`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.NotConfigured()

        shouldThrow<InvalidStateException> {
            useCase.execute()
        }
    }

    @Test
    fun `throws exception if there is no access token`() = runTest {
        coEvery { fetchAuthSession.execute().accessToken } returns null

        shouldThrow<InvalidUserPoolConfigurationException> {
            useCase.execute()
        }
    }
}
