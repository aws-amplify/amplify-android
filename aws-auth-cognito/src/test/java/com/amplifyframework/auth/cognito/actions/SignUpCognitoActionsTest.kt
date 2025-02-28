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
package com.amplifyframework.auth.cognito.actions

import androidx.test.core.app.ApplicationProvider
import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeDeliveryDetailsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmSignUpResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeliveryMediumType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpResponse
import com.amplifyframework.auth.cognito.AWSCognitoAuthService
import com.amplifyframework.auth.cognito.AuthConfiguration
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.StoreClientBehavior
import com.amplifyframework.auth.cognito.util.toAuthCodeDeliveryDetails
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.EventDispatcher
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.data.SignUpData
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import com.amplifyframework.statemachine.codegen.events.SignUpEvent
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SignUpCognitoActionsTest {

    private val pool = mockk<UserPoolConfiguration> {
        every { appClient } returns "client"
        every { appClientSecret } returns null
        every { pinpointAppId } returns null
    }
    private val configuration = mockk<AuthConfiguration> {
        every { userPool } returns pool
    }
    private val cognitoAuthService = mockk<AWSCognitoAuthService>()
    private val credentialStoreClient = mockk<StoreClientBehavior> {
        coEvery { loadCredentials(CredentialType.ASF) } returns AmplifyCredential.ASFDevice("asf_id")
    }
    private val logger = mockk<Logger>(relaxed = true)
    private val cognitoIdentityProviderClientMock = mockk<CognitoIdentityProviderClient>()

    private val capturedEvent = slot<StateMachineEvent>()
    private val dispatcher = mockk<EventDispatcher> {
        every { send(capture(capturedEvent)) } just Runs
    }

    private lateinit var authEnvironment: AuthEnvironment

    @Before
    fun setup() {
        every { cognitoAuthService.cognitoIdentityProviderClient }.answers { cognitoIdentityProviderClientMock }
        authEnvironment = AuthEnvironment(
            ApplicationProvider.getApplicationContext(),
            configuration,
            cognitoAuthService,
            credentialStoreClient,
            null,
            null,
            logger
        )
    }

    @Test
    fun `sign up succeeds with sign up initiated state`() = runTest {
        val session = "SESSION"
        val username = "USERNAME"
        val userSub = "123"
        val codeDeliveryDetails = CodeDeliveryDetailsType.invoke {
            this.destination = "DESTINATION"
            this.deliveryMedium = DeliveryMediumType.Email
            this.attributeName = "ATTRIBUTE"
        }
        coEvery { cognitoIdentityProviderClientMock.signUp(any()) } returns SignUpResponse {
            this.codeDeliveryDetails = codeDeliveryDetails
            this.userSub = userSub
            this.session = session
            this.userConfirmed = false
        }

        val signUpData = SignUpData(username, mapOf(), mapOf(), null, null)
        SignUpCognitoActions.initiateSignUpAction(
            SignUpEvent.EventType.InitiateSignUp(signUpData)
        ).execute(dispatcher, authEnvironment)

        val event = capturedEvent.captured.shouldBeInstanceOf<SignUpEvent>()
        val eventType = event.eventType.shouldBeInstanceOf<SignUpEvent.EventType.InitiateSignUpComplete>()

        eventType.signUpData.should {
            it.username shouldBe username
            it.session shouldBe session
            it.userId shouldBe userSub
        }

        eventType.signUpResult.should {
            it.nextStep.signUpStep shouldBe AuthSignUpStep.CONFIRM_SIGN_UP_STEP
            it.nextStep.codeDeliveryDetails shouldBe codeDeliveryDetails.toAuthCodeDeliveryDetails()
            it.isSignUpComplete.shouldBeFalse()
            it.userId shouldBe userSub
        }
    }

    @Test
    fun `sign up succeeds with confirm sign up state`() = runTest {
        val session = "SESSION"
        val username = "USERNAME"
        val userSub = "123"
        val codeDeliveryDetails = CodeDeliveryDetailsType.invoke {
            this.destination = "DESTINATION"
            this.deliveryMedium = DeliveryMediumType.Email
            this.attributeName = "ATTRIBUTE"
        }
        coEvery { cognitoIdentityProviderClientMock.signUp(any()) } returns SignUpResponse {
            this.codeDeliveryDetails = codeDeliveryDetails
            this.userSub = userSub
            this.session = session
            this.userConfirmed = false
        }

        val signUpData = SignUpData(username, mapOf(), mapOf(), null, null)
        SignUpCognitoActions.initiateSignUpAction(
            SignUpEvent.EventType.InitiateSignUp(signUpData)
        ).execute(dispatcher, authEnvironment)

        val event = capturedEvent.captured.shouldBeInstanceOf<SignUpEvent>()
        val eventType = event.eventType.shouldBeInstanceOf<SignUpEvent.EventType.InitiateSignUpComplete>()

        eventType.signUpData.should {
            it.username shouldBe username
            it.session shouldBe session
            it.userId shouldBe userSub
        }

        eventType.signUpResult.should {
            it.nextStep.signUpStep shouldBe AuthSignUpStep.CONFIRM_SIGN_UP_STEP
            it.nextStep.codeDeliveryDetails shouldBe codeDeliveryDetails.toAuthCodeDeliveryDetails()
            it.isSignUpComplete.shouldBeFalse()
            it.userId shouldBe userSub
        }
    }

    @Test
    fun `sign up succeeds with auto sign in state`() = runTest {
        val session = "SESSION"
        val username = "USERNAME"
        val userSub = "123"
        coEvery { cognitoIdentityProviderClientMock.signUp(any()) } returns SignUpResponse {
            this.userSub = userSub
            this.session = session
            this.userConfirmed = true
        }

        val signUpData = SignUpData(username, mapOf(), mapOf(), null, null)
        SignUpCognitoActions.initiateSignUpAction(
            SignUpEvent.EventType.InitiateSignUp(signUpData)
        ).execute(dispatcher, authEnvironment)

        val event = capturedEvent.captured.shouldBeInstanceOf<SignUpEvent>()
        val eventType = event.eventType.shouldBeInstanceOf<SignUpEvent.EventType.SignedUp>()

        eventType.signUpData.should {
            it.username shouldBe username
            it.session shouldBe session
            it.userId shouldBe userSub
        }

        eventType.signUpResult.should {
            it.nextStep.signUpStep shouldBe AuthSignUpStep.COMPLETE_AUTO_SIGN_IN
            it.isSignUpComplete.shouldBeTrue()
            it.userId shouldBe userSub
        }
    }

    @Test
    fun `sign up succeeds with done state`() = runTest {
        val username = "USERNAME"
        val userSub = "123"
        coEvery { cognitoIdentityProviderClientMock.signUp(any()) } returns SignUpResponse {
            this.userSub = userSub
            this.userConfirmed = true
        }

        val signUpData = SignUpData(username, mapOf(), mapOf(), null, null)
        SignUpCognitoActions.initiateSignUpAction(
            SignUpEvent.EventType.InitiateSignUp(signUpData)
        ).execute(dispatcher, authEnvironment)

        val event = capturedEvent.captured.shouldBeInstanceOf<SignUpEvent>()
        val eventType = event.eventType.shouldBeInstanceOf<SignUpEvent.EventType.SignedUp>()

        eventType.signUpData.should {
            it.username shouldBe username
            it.session.shouldBeNull()
            it.userId shouldBe userSub
        }

        eventType.signUpResult.should {
            it.nextStep.signUpStep shouldBe AuthSignUpStep.DONE
            it.isSignUpComplete.shouldBeTrue()
            it.userId shouldBe userSub
        }
    }

    @Test
    fun `confirm sign up succeeds with done state`() = runTest {
        val username = "USERNAME"
        val userSub = "123"
        val confirmationCode = "456"
        coEvery { cognitoIdentityProviderClientMock.confirmSignUp(any()) } returns ConfirmSignUpResponse {
            this.session = null
        }

        val signUpData = SignUpData(username, mapOf(), mapOf(), null, userSub)
        SignUpCognitoActions.confirmSignUpAction(
            SignUpEvent.EventType.ConfirmSignUp(signUpData, confirmationCode)
        ).execute(dispatcher, authEnvironment)

        val event = capturedEvent.captured.shouldBeInstanceOf<SignUpEvent>()
        val eventType = event.eventType.shouldBeInstanceOf<SignUpEvent.EventType.SignedUp>()

        eventType.signUpData.should {
            it.username shouldBe username
            it.session.shouldBeNull()
            it.userId shouldBe userSub
        }

        eventType.signUpResult.should {
            it.nextStep.signUpStep shouldBe AuthSignUpStep.DONE
            it.isSignUpComplete.shouldBeTrue()
            it.userId shouldBe userSub
        }
    }

    @Test
    fun `confirm sign up succeeds with auto sign in state`() = runTest {
        val username = "USERNAME"
        val userSub = "123"
        val confirmationCode = "456"
        val session = "SESSION"
        coEvery { cognitoIdentityProviderClientMock.confirmSignUp(any()) } returns ConfirmSignUpResponse {
            this.session = session
        }

        val signUpData = SignUpData(username, mapOf(), mapOf(), null, userSub)
        SignUpCognitoActions.confirmSignUpAction(
            SignUpEvent.EventType.ConfirmSignUp(signUpData, confirmationCode)
        ).execute(dispatcher, authEnvironment)

        val event = capturedEvent.captured.shouldBeInstanceOf<SignUpEvent>()
        val eventType = event.eventType.shouldBeInstanceOf<SignUpEvent.EventType.SignedUp>()

        eventType.signUpData.should {
            it.username shouldBe username
            it.session shouldBe session
            it.userId shouldBe userSub
        }

        eventType.signUpResult.should {
            it.nextStep.signUpStep shouldBe AuthSignUpStep.COMPLETE_AUTO_SIGN_IN
            it.isSignUpComplete.shouldBeTrue()
            it.userId shouldBe userSub
        }
    }
}
