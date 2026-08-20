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

import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthFactorType
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.TOTPSetupDetails
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.step.AuthNextSignInStep
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.statemachine.codegen.data.SignInTOTPSetupData
import com.amplifyframework.statemachine.codegen.states.CustomSignInState
import com.amplifyframework.statemachine.codegen.states.HostedUISignInState
import com.amplifyframework.statemachine.codegen.states.SRPSignInState
import com.amplifyframework.statemachine.codegen.states.SetupTOTPState
import com.amplifyframework.statemachine.codegen.states.SignInChallengeState
import com.amplifyframework.statemachine.codegen.states.SignInState
import com.amplifyframework.statemachine.codegen.states.WebAuthnSignInState

internal object UserPoolSignInHelper {
    fun checkNextStep(signInState: SignInState): AuthSignInResult? = when (signInState) {
        is SignInState.Error -> throw signInState.exception
        is SignInState.SigningInWithSRP -> when (val srpState = signInState.srpSignInState) {
            is SRPSignInState.Error -> throw srpState.exception
            else -> null
        }
        is SignInState.SigningInWithSRPCustom -> when (val srpState = signInState.srpSignInState) {
            is SRPSignInState.Error -> throw srpState.exception
            else -> null
        }
        // Swift has an error state for migrate auth, Android does not
        is SignInState.SigningInViaMigrateAuth -> null
        is SignInState.SigningInWithCustom -> when (val customState = signInState.customSignInState) {
            is CustomSignInState.Error -> throw customState.error
            else -> null
        }
        is SignInState.SigningInWithHostedUI -> when (val hostedUiState = signInState.hostedUISignInState) {
            is HostedUISignInState.Error -> throw hostedUiState.exception
            else -> null
        }
        is SignInState.ResolvingChallenge -> when (val challengeState = signInState.challengeState) {
            is SignInChallengeState.Error -> throw challengeState.exception
            is SignInChallengeState.WaitingForAnswer -> SignInChallengeHelper.getNextStep(challengeState.challenge)
            else -> null
        }
        is SignInState.ResolvingTOTPSetup -> when (val totpState = signInState.setupTOTPState) {
            is SetupTOTPState.Error -> throw totpState.exception
            is SetupTOTPState.WaitingForAnswer -> signInResult(
                signInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_TOTP_SETUP,
                totpSetupDetails = totpState.signInTOTPSetupData.toTotpSetupDetails()
            )
            else -> null
        }
        is SignInState.SigningInWithWebAuthn -> when (val webAuthnState = signInState.webAuthnSignInState) {
            is WebAuthnSignInState.Error -> throw webAuthnState.exception
            else -> null
        }
        else -> null
    }

    fun signInResult(
        signInStep: AuthSignInStep,
        additionalInfo: Map<String, String> = emptyMap(),
        codeDeliveryDetails: AuthCodeDeliveryDetails? = null,
        totpSetupDetails: TOTPSetupDetails? = null,
        allowedMFATypes: Set<MFAType>? = null,
        availableFactors: Set<AuthFactorType>? = null
    ) = AuthSignInResult(
        signInStep.isSignedIn,
        AuthNextSignInStep(
            signInStep,
            additionalInfo,
            codeDeliveryDetails,
            totpSetupDetails,
            allowedMFATypes,
            availableFactors
        )
    )

    fun signedInResult() = signInResult(signInStep = AuthSignInStep.DONE)

    private fun SignInTOTPSetupData.toTotpSetupDetails() = TOTPSetupDetails(
        sharedSecret = secretCode,
        username = username
    )

    private val AuthSignInStep.isSignedIn: Boolean
        get() = this == AuthSignInStep.DONE
}
