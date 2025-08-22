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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AttributeType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GetUserResponse
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FetchUserAttributesUseCaseTest {

    private val client: CognitoIdentityProviderClient = mockk()
    private val fetchAuthSession: FetchAuthSessionUseCase = mockk {
        coEvery { execute().accessToken } returns "access token"
    }
    private val stateMachine: AuthStateMachine = mockk {
        coEvery { getCurrentState().authNState } returns AuthenticationState.SignedIn(mockk(), mockk())
    }

    private val useCase = FetchUserAttributesUseCase(
        client = client,
        fetchAuthSession = fetchAuthSession,
        stateMachine = stateMachine
    )

    @Test
    fun `fetch user attributes with success`() = runTest {
        val attributes = listOf(
            AttributeType {
                name = "email"
                value = "email"
            },
            AttributeType {
                name = "nickname"
                value = "nickname"
            }
        )

        val expectedResult = attributes.map { AuthUserAttribute(AuthUserAttributeKey.custom(it.name), it.value) }

        coEvery {
            client.getUser(any())
        } returns GetUserResponse {
            userAttributes = attributes
            username = ""
        }

        val result = useCase.execute()

        result shouldBe expectedResult
    }

    @Test
    fun `fetch user attributes fails when not in SignedIn state`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.SignedOut(mockk())

        shouldThrow<SignedOutException> {
            useCase.execute()
        }
    }
}
