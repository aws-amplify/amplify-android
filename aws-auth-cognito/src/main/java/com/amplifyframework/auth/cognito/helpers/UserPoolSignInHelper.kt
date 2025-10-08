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
    fun checkNextStep(signInState: SignInState): AuthSignInResult? {
        when (signInState) {
            is SignInState.Error -> throw signInState.exception
            is SignInState.SigningInWithSRP -> {
                val srpState = signInState.srpSignInState
                if (srpState is SRPSignInState.Error) throw srpState.exception
            }
            is SignInState.SigningInWithSRPCustom -> {
                val srpState = signInState.srpSignInState
                if (srpState is SRPSignInState.Error) throw srpState.exception
            }
            // Swift has an error state for migrate auth, Android does not
            is SignInState.SigningInViaMigrateAuth -> Unit
            is SignInState.SigningInWithCustom -> {
                val customState = signInState.customSignInState
                if (customState is CustomSignInState.Error) throw customState.error
            }
            is SignInState.SigningInWithHostedUI -> {
                val hostedUiState = signInState.hostedUISignInState
                if (hostedUiState is HostedUISignInState.Error) throw hostedUiState.exception
            }
            is SignInState.ResolvingChallenge -> {
                val challengeState = signInState.challengeState
                if (challengeState is SignInChallengeState.WaitingForAnswer && challengeState.hasNewResponse) {
                    challengeState.hasNewResponse = false
                    return SignInChallengeHelper.getNextStep(challengeState.challenge)
                }
                if (challengeState is SignInChallengeState.Error && challengeState.hasNewResponse) {
                    challengeState.hasNewResponse = false
                    throw challengeState.exception
                }
            }
            is SignInState.ResolvingTOTPSetup -> {
                val setupTotpState = signInState.setupTOTPState
                if (setupTotpState is SetupTOTPState.Error && setupTotpState.hasNewResponse) {
                    setupTotpState.hasNewResponse = false
                    throw setupTotpState.exception
                }
                if (setupTotpState is SetupTOTPState.WaitingForAnswer && setupTotpState.hasNewResponse) {
                    setupTotpState.hasNewResponse = false
                    return signInResult(
                        signInStep = AuthSignInStep.CONTINUE_SIGN_IN_WITH_TOTP_SETUP,
                        totpSetupDetails = setupTotpState.signInTOTPSetupData.toTotpSetupDetails()
                    )
                }
            }
            is SignInState.SigningInWithWebAuthn -> {
                val webAuthnState = signInState.webAuthnSignInState
                if (webAuthnState is WebAuthnSignInState.Error && webAuthnState.hasNewResponse) {
                    webAuthnState.hasNewResponse = false
                    throw webAuthnState.exception
                }
            }
            else -> Unit
        }

        return null
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

    fun signInDoneResult() = signInResult(signInStep = AuthSignInStep.DONE)

    private fun SignInTOTPSetupData.toTotpSetupDetails() = TOTPSetupDetails(
        sharedSecret = secretCode,
        username = username
    )

    private val AuthSignInStep.isSignedIn: Boolean
        get() = this == AuthSignInStep.DONE
}
