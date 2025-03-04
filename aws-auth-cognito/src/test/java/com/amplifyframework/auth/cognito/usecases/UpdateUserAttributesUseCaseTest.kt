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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeDeliveryDetailsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeliveryMediumType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UpdateUserAttributesResponse
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthUpdateUserAttributeOptions
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthUpdateUserAttributesOptions
import com.amplifyframework.auth.cognito.shouldHaveAttributeName
import com.amplifyframework.auth.cognito.shouldHaveDeliveryMedium
import com.amplifyframework.auth.cognito.shouldHaveDestination
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.result.step.AuthUpdateAttributeStep
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdateUserAttributesUseCaseTest {
    private val client: CognitoIdentityProviderClient = mockk()
    private val fetchAuthSession: FetchAuthSessionUseCase = mockk {
        coEvery { execute().accessToken } returns "access token"
    }
    private val stateMachine: AuthStateMachine = mockk {
        coEvery { getCurrentState().authNState } returns AuthenticationState.SignedIn(
            signedInData = mockk { every { username } returns "user" },
            deviceMetadata = mockk()
        )
    }

    private val useCase = UpdateUserAttributesUseCase(
        client = client,
        fetchAuthSession = fetchAuthSession,
        stateMachine = stateMachine
    )

    private val attributes = listOf(
        AuthUserAttribute(AuthUserAttributeKey.email(), "test@test.com"),
        AuthUserAttribute(AuthUserAttributeKey.nickname(), "test")
    )

    @Test
    fun `update user attribute fails when not in SignedIn state`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.NotConfigured()

        shouldThrow<InvalidStateException> {
            useCase.execute(attributes.first())
        }
    }

    @Test
    fun `update user attributes fails when not in SignedIn state`() = runTest {
        coEvery { stateMachine.getCurrentState().authNState } returns AuthenticationState.NotConfigured()

        shouldThrow<InvalidStateException> {
            useCase.execute(attributes)
        }
    }

    @Test
    fun `update single user attribute with no attribute options and delivery code success`() = runTest {
        coEvery { client.updateUserAttributes(any()) } returns UpdateUserAttributesResponse { }

        val result = useCase.execute(attributes.first())

        result.isUpdated.shouldBeTrue()
        result.nextStep.codeDeliveryDetails.shouldBeNull()
        result.nextStep.updateAttributeStep shouldBe AuthUpdateAttributeStep.DONE
    }

    @Test
    fun `update single user attribute with attribute options and no delivery code success`() = runTest {
        coEvery { client.updateUserAttributes(any()) } returns UpdateUserAttributesResponse { }

        val options = AWSCognitoAuthUpdateUserAttributeOptions.builder().metadata(
            mapOf("x" to "x", "y" to "y", "z" to "z")
        ).build()

        val result = useCase.execute(attributes.first(), options)

        result.isUpdated.shouldBeTrue()
        result.nextStep.codeDeliveryDetails.shouldBeNull()
        result.nextStep.updateAttributeStep shouldBe AuthUpdateAttributeStep.DONE
    }

    @Test
    fun `update user attributes with delivery code success`() = runTest {
        coEvery { client.updateUserAttributes(any()) } returns UpdateUserAttributesResponse {
            codeDeliveryDetailsList = listOf(
                CodeDeliveryDetailsType {
                    attributeName = "email"
                    deliveryMedium = DeliveryMediumType.Email
                    destination = "test"
                }
            )
        }

        val options = AWSCognitoAuthUpdateUserAttributesOptions.builder().metadata(
            mapOf("x" to "x", "y" to "y", "z" to "z")
        ).build()

        val result = useCase.execute(attributes, options)

        result.shouldHaveSize(2)

        val nicknameResult = result[AuthUserAttributeKey.nickname()]!!
        val emailResult = result[AuthUserAttributeKey.email()]!!

        nicknameResult.isUpdated.shouldBeTrue()
        nicknameResult.nextStep.codeDeliveryDetails.shouldBeNull()
        nicknameResult.nextStep.updateAttributeStep shouldBe AuthUpdateAttributeStep.DONE

        emailResult.isUpdated.shouldBeFalse()
        emailResult.nextStep.codeDeliveryDetails.shouldNotBeNull()
            .shouldHaveAttributeName("email")
            .shouldHaveDestination("test")
            .shouldHaveDeliveryMedium(AuthCodeDeliveryDetails.DeliveryMedium.EMAIL)
        emailResult.nextStep.updateAttributeStep shouldBe AuthUpdateAttributeStep.CONFIRM_ATTRIBUTE_WITH_CODE
    }
}
