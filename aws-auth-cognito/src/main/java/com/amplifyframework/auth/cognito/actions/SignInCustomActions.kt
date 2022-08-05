package com.amplifyframework.auth.cognito.actions

import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthFlowType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.CustomSignInActions
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.CustomSignInEvent

class SignInCustomActions : CustomSignInActions {
    override fun initiateCustomSignInAuthAction(event: CustomSignInEvent.EventType.InitiateCustomSignIn): Action =
        Action<AuthEnvironment>("InitSRPAuth") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = try {
                val secretHash = try {
                    AuthHelper().getSecretHash(
                        event.username,
                        configuration.userPool?.appClient,
                        configuration.userPool?.appClientSecret
                    )
                } catch (e: java.lang.Exception) {
                    null
                }

                var authParams = mapOf("USERNAME" to event.username)
                secretHash?.also { authParams = authParams.plus("SECRET_HASH" to secretHash) }

                val initiateAuthResponse = cognitoAuthService.cognitoIdentityProviderClient?.initiateAuth {
                    authFlow = AuthFlowType.CustomAuth
                    clientId = configuration.userPool?.appClient
                    authParameters = authParams
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

    override fun respondToChallengeAction(event: CustomSignInEvent.EventType.FinalizeSignIn): Action {
        TODO("Not yet implemented")
    }
}