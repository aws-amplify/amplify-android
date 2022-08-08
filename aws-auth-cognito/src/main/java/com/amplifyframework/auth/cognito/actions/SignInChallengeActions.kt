package com.amplifyframework.auth.cognito.actions

import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthFlowType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SignInChallengeActions
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.CustomSignInEvent
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent

class SignInChallengeActions: SignInChallengeActions {
    override fun initiateChallengeAuthAction(event: SignInChallengeEvent.EventType.WaitForAnswer): Action  =
        Action<AuthEnvironment>("InitSRPAuth") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = try {


                val respondToAuthChallengeResponse = cognitoAuthService.cognitoIdentityProviderClient?.respondToAuthChallenge{
                    challengeName = event.challengeName
                    clientId = configuration.userPool?.appClient
                    challengeResponses = event.challengeResponses
                }

                when (initiateAuthResponse?.challengeName) {
                    //TODO: Call the Challenge State machine which deals with the challenge and its parameters and once done call the finalize sign in
                    ChallengeNameType.CustomChallenge -> initiateAuthResponse.challengeParameters?.let {
                        CustomSignInEvent(CustomSignInEvent.EventType.FinalizeSignIn("TODO"))
                    } ?: throw Exception("Auth challenge parameters are empty.")
                    else -> throw Exception("Not yet implemented.")
                }
            } catch (e: Exception) {
                val errorEvent = CustomSignInEvent(CustomSignInEvent.EventType.ThrowAuthError(e))
                logger?.verbose("$id Sending event ${errorEvent.type}")
                dispatcher.send(errorEvent)

                AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())
            }
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun verifyChallengeAuthAction(event: SignInChallengeEvent.EventType.VerifyChallengeAnswer): Action {
        TODO("Not yet implemented")
    }
}