package com.amplifyframework.auth.cognito.helpers

import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.RespondToAuthChallengeResponse
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.result.AuthSignInResult
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
    fun getNextStepEvent(userId:String = "", username: String, response: RespondToAuthChallengeResponse?): StateMachineEvent {
        val authenticationResult = response?.authenticationResult
        val challengeNameType = response?.challengeName
        return when {
            authenticationResult != null -> {
                val signedInData = authenticationResult.let {
                    val expiresIn = Instant.now().plus(it.expiresIn.seconds).epochSeconds
                    val tokens = CognitoUserPoolTokens(it.idToken, it.accessToken, it.refreshToken, expiresIn)
                    SignedInData(userId, username, Date(), SignInMethod.SRP, tokens)
                }
//                SRPEvent(SRPEvent.EventType.FinalizeSRPSignIn(signedInData))
//                logger?.verbose("$id Sending event ${finalizeEvent.type}")
//                dispatcher.send(finalizeEvent)

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

//    fun checkNextStep(signInState: SignInState) {
//        val authChallengeType = ""
//        val challengeState = (signInState as SignInState.ResolvingChallenge).challengeState
//        when (challengeState) {
//            is SignInChallengeState.WaitingForAnswer -> {
//                when (authChallengeType) {
//                    "smsMFA" -> {}
//                    "newPasswordRequired" -> {}
//                    else -> {}
//                }
//            }
//        }
//    }

    fun checkNextStep(
        challengeType: ChallengeNameType,
        challengeResponse: ChallengeResponse,
        onSuccess: Consumer<AuthSignInResult>,
        onError: Consumer<AuthException>
    ) {
        when (challengeType) {
            is ChallengeNameType.SmsMfa -> {}
            is ChallengeNameType.NewPasswordRequired -> {}
            is ChallengeNameType.CustomChallenge -> {}
            else -> {}
        }
    }
}