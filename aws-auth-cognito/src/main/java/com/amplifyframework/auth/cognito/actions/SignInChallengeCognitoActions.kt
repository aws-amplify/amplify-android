package com.amplifyframework.auth.cognito.actions

import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SignInChallengeActions
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent

object SignInChallengeCognitoActions : SignInChallengeActions {
    override fun verifySignInChallenge(event: SignInChallengeEvent.EventType.VerifyChallengeAnswer) =
        Action<AuthEnvironment>(
            "VerifySignInChallenge"
        ) { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = try {
                val params = event.challengeParameters
                val username = params.getValue("USERNAME")

                val secretHash = SRPHelper.getSecretHash(
                    username,
                    configuration.userPool?.appClient,
                    configuration.userPool?.appClientSecret
                )

                var challengeParams = mapOf(
                    "USERNAME" to username
                )

                secretHash?.also { challengeParams = challengeParams.plus("SECRET_HASH" to secretHash) }
                val response = cognitoAuthService.cognitoIdentityProviderClient?.respondToAuthChallenge {
                    clientId = configuration.userPool?.appClient
                    challengeName = ChallengeNameType.SmsMfa
                    challengeResponses = mapOf()
                    session = ""
                }

                SignInChallengeHelper.getNextStepEvent("", username, response)
            } catch (e: Exception) {
                SignInEvent(SignInEvent.EventType.ThrowError(e))
            }
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
