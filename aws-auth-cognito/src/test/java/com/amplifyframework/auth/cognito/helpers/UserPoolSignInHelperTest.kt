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

package com.amplifyframework.auth.cognito.helpers

import com.amplifyframework.auth.AuthFactorType
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.statemachine.codegen.data.SignInTOTPSetupData
import com.amplifyframework.statemachine.codegen.states.CustomSignInState
import com.amplifyframework.statemachine.codegen.states.HostedUISignInState
import com.amplifyframework.statemachine.codegen.states.SRPSignInState
import com.amplifyframework.statemachine.codegen.states.SetupTOTPState
import com.amplifyframework.statemachine.codegen.states.SignInChallengeState
import com.amplifyframework.statemachine.codegen.states.SignInState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.junit.Test

class UserPoolSignInHelperTest {
    private val testException = Exception("this is a test")

    @Test
    fun `throws error from error state`() {
        val state = SignInState.Error(testException)
        shouldThrow<Exception> { UserPoolSignInHelper.checkNextStep(state) } shouldBe testException
    }

    @Test
    fun `throws error from SRP error state`() {
        val srpState = SRPSignInState.Error(testException)
        val state = SignInState.SigningInWithSRP(srpState)
        shouldThrow<Exception> { UserPoolSignInHelper.checkNextStep(state) } shouldBe testException
    }

    @Test
    fun `throws error from SRP custom error state`() {
        val srpState = SRPSignInState.Error(testException)
        val state = SignInState.SigningInWithSRPCustom(srpState)
        shouldThrow<Exception> { UserPoolSignInHelper.checkNextStep(state) } shouldBe testException
    }

    @Test
    fun `throws error from custom sign in error state`() {
        val customState = CustomSignInState.Error(testException)
        val state = SignInState.SigningInWithCustom(customState)
        shouldThrow<Exception> { UserPoolSignInHelper.checkNextStep(state) } shouldBe testException
    }

    @Test
    fun `throws error from hosted UI error state`() {
        val hostedUIState = HostedUISignInState.Error(testException)
        val state = SignInState.SigningInWithHostedUI(hostedUIState)
        shouldThrow<Exception> { UserPoolSignInHelper.checkNextStep(state) } shouldBe testException
    }

    @Test
    fun `returns null for migrate auth state`() {
        val state = SignInState.SigningInViaMigrateAuth(null)
        val result = UserPoolSignInHelper.checkNextStep(state)
        result shouldBe null
    }

    @Test
    fun `throws error from setup totp error state`() {
        val totpState = SetupTOTPState.Error(testException, "username", "session", mockk())
        val state = SignInState.ResolvingTOTPSetup(totpState)

        shouldThrow<Exception> { UserPoolSignInHelper.checkNextStep(state) } shouldBe testException
    }

    @Test
    fun `returns expected result for setup totp waiting for answer`() {
        val setupData = SignInTOTPSetupData(
            secretCode = "secret",
            session = "session",
            username = "username"
        )
        val totpState = SetupTOTPState.WaitingForAnswer(setupData, emptyMap(), mockk())
        val state = SignInState.ResolvingTOTPSetup(totpState)
        val result = UserPoolSignInHelper.checkNextStep(state)

        result.shouldNotBeNull()
        result.isSignedIn.shouldBeFalse()
        result.nextStep.signInStep shouldBe AuthSignInStep.CONTINUE_SIGN_IN_WITH_TOTP_SETUP
        result.nextStep.totpSetupDetails?.username shouldBe "username"
        result.nextStep.totpSetupDetails?.sharedSecret shouldBe "secret"
    }

    @Test
    fun `throws error from resolving challenge error state`() {
        val challengeState = SignInChallengeState.Error(testException, mockk(), mockk())
        val state = SignInState.ResolvingChallenge(challengeState)

        shouldThrow<Exception> { UserPoolSignInHelper.checkNextStep(state) } shouldBe testException
    }

    @Test
    fun `returns expected result from resolving challenge waiting for answer`() {
        val expectedResult = UserPoolSignInHelper.signInResult(AuthSignInStep.CONFIRM_SIGN_IN_WITH_OTP)
        mockkObject(SignInChallengeHelper) {
            every { SignInChallengeHelper.getNextStep(any()) } returns expectedResult

            val challengeState = SignInChallengeState.WaitingForAnswer(mockk(), mockk())
            val state = SignInState.ResolvingChallenge(challengeState)

            val result = UserPoolSignInHelper.checkNextStep(state)
            result shouldBe expectedResult
        }
    }

    @Test
    fun `signInResult creates correct result`() {
        val result = UserPoolSignInHelper.signInResult(
            signInStep = AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE,
            additionalInfo = mapOf("key" to "value"),
            allowedMFATypes = setOf(MFAType.SMS),
            availableFactors = setOf(AuthFactorType.SMS_OTP)
        )

        result.isSignedIn.shouldBeFalse()
        result.nextStep.signInStep shouldBe AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE
        result.nextStep.additionalInfo shouldBe mapOf("key" to "value")
        result.nextStep.allowedMFATypes shouldBe setOf(MFAType.SMS)
        result.nextStep.availableFactors shouldBe setOf(AuthFactorType.SMS_OTP)
    }

    @Test
    fun `signedInResult creates done result`() {
        val result = UserPoolSignInHelper.signedInResult()

        result.isSignedIn.shouldBeTrue()
        result.nextStep.signInStep shouldBe AuthSignInStep.DONE
    }
}
