package com.amplifyframework.auth.cognito.helpers

import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.RespondToAuthChallengeResponse
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.step.AuthNextSignInStep
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.core.Consumer
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent
import java.util.Date
import kotlin.time.Duration.Companion.seconds

object SignInChallengeHelper {
    fun evaluateNextStep(
        userId: String = "",
        username: String,
        response: RespondToAuthChallengeResponse?
    ): StateMachineEvent {
        val authenticationResult = response?.authenticationResult
        val challengeNameType = response?.challengeName
        return when {
            authenticationResult != null -> {
                val signedInData = authenticationResult.let {
                    val expiresIn = Instant.now().plus(it.expiresIn.seconds).epochSeconds
                    val tokens = CognitoUserPoolTokens(it.idToken, it.accessToken, it.refreshToken, expiresIn)
                    SignedInData(userId, username, Date(), SignInMethod.SRP, tokens)
                }

                AuthenticationEvent(AuthenticationEvent.EventType.SignInCompleted(signedInData))
            }
            challengeNameType is ChallengeNameType.SmsMfa ||
                challengeNameType is ChallengeNameType.CustomChallenge
                || challengeNameType is ChallengeNameType.NewPasswordRequired -> {
                val challenge =
                    AuthChallenge(challengeNameType.value, username, response.session, response.challengeParameters)
                SignInEvent(SignInEvent.EventType.ReceivedChallenge(challenge))
            }
            else -> SignInEvent(SignInEvent.EventType.ThrowError(Exception("Response did not contain sign in info.")))
        }
    }

    fun getNextStep(
        challenge: AuthChallenge,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        val challengeParams = challenge.parameters?.toMutableMap() ?: mapOf()
        when (ChallengeNameType.fromValue(challenge.challengeName)) {
            is ChallengeNameType.SmsMfa -> {
                val deliveryDetails = AuthCodeDeliveryDetails(
                    challengeParams.getValue("CODE_DELIVERY_DESTINATION") ?: "",
                    AuthCodeDeliveryDetails.DeliveryMedium.fromString(
                        challengeParams.getValue("CODE_DELIVERY_DELIVERY_MEDIUM")
                    )
                )
                val authSignInResult = AuthSignInResult(
                    false,
                    AuthNextSignInStep(AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE, mapOf(), deliveryDetails)
                )
                onSuccess.accept(authSignInResult)
            }
            is ChallengeNameType.NewPasswordRequired -> {
                val authSignInResult = AuthSignInResult(
                    false,
                    AuthNextSignInStep(AuthSignInStep.CONFIRM_SIGN_IN_WITH_NEW_PASSWORD, challengeParams, null)
                )
                onSuccess.accept(authSignInResult)
            }
            is ChallengeNameType.CustomChallenge -> {
                val authSignInResult = AuthSignInResult(
                    false,
                    AuthNextSignInStep(AuthSignInStep.CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE, challengeParams, null)
                )
                onSuccess.accept(authSignInResult)
            }
            else -> onError.accept(AuthException.UnknownException(Exception("Challenge type not supported.")))
        }
    }
}
