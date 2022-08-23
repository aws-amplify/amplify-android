/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SignInChallengeActions
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent

object SignInChallengeCognitoActions : SignInChallengeActions {
    override fun verifySignInChallenge(
        event: SignInChallengeEvent.EventType.VerifyChallengeAnswer,
        challenge: AuthChallenge
    ) = Action<AuthEnvironment>("VerifySignInChallenge") { id, dispatcher ->
        logger?.verbose("$id Starting execution")
        val evt = try {
            val username = challenge.username
            var challengeResponses = mapOf("USERNAME" to username)

            challenge.getChallengeResponseKey()?.also {
                challengeResponses = challengeResponses.plus(it to event.answer)
            }

            // TODO: add attributes data from event to challenge response map

            val secretHash = SRPHelper.getSecretHash(
                username,
                configuration.userPool?.appClient,
                configuration.userPool?.appClientSecret
            )
            secretHash?.also { challengeResponses = challengeResponses.plus("SECRET_HASH" to secretHash) }

            val response = cognitoAuthService.cognitoIdentityProviderClient?.respondToAuthChallenge {
                clientId = configuration.userPool?.appClient
                challengeName = ChallengeNameType.fromValue(challenge.challengeName)
                this.challengeResponses = challengeResponses
                session = challenge.session
            }

            SignInChallengeHelper.evaluateNextStep("", username, response)
        } catch (e: Exception) {
            SignInEvent(SignInEvent.EventType.ThrowError(e))
        }
        logger?.verbose("$id Sending event ${evt.type}")
        dispatcher.send(evt)
    }

    fun AuthChallenge.getChallengeResponseKey() = when (ChallengeNameType.fromValue(challengeName)) {
        is ChallengeNameType.SmsMfa -> "SMS_MFA_CODE"
        is ChallengeNameType.NewPasswordRequired -> "NEW_PASSWORD"
        is ChallengeNameType.CustomChallenge -> "ANSWER"
        else -> null
    }
}
