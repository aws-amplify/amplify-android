package com.amplifyframework.auth.cognito.actions

import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SignInChallengeActions
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.CustomSignInEvent
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent

class SignInChallengeActions : SignInChallengeActions {

    override fun verifyChallengeAuthAction(
        event: SignInChallengeEvent.EventType.VerifyChallengeAnswer,
        challenge: AuthChallenge
    ): Action =
        Action<AuthEnvironment>("InitSRPAuth") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = try {
                val respondToAuthChallengeResponse =
                    cognitoAuthService.cognitoIdentityProviderClient?.respondToAuthChallenge {
                        challengeName = ChallengeNameType.fromValue(challenge.challengeName)
                        clientId = configuration.userPool?.appClient
                        challengeResponses = challenge.parameters
                        session = challenge.session
                    }

                respondToAuthChallengeResponse?.authenticationResult?.let {
                    CustomSignInEvent(CustomSignInEvent.EventType.FinalizeSignIn(it.accessToken ?: ""))
                } ?: SignInChallengeEvent(SignInChallengeEvent.EventType.WaitForAnswer(
                    AuthChallenge(
                        challengeName = respondToAuthChallengeResponse?.challengeName.toString(),
                        session = respondToAuthChallengeResponse?.session,
                        parameters = respondToAuthChallengeResponse?.challengeParameters
                    )
                ))
            } catch (e: Exception) {
                val errorEvent = CustomSignInEvent(CustomSignInEvent.EventType.ThrowAuthError(e))
                logger?.verbose("$id Sending event ${errorEvent.type}")
                dispatcher.send(errorEvent)

                AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())
            }

            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}