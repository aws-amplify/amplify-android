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
import aws.sdk.kotlin.services.cognitoidentityprovider.respondToAuthChallenge
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SignInChallengeActions
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.events.CustomSignInEvent
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent

object SignInChallengeCognitoActions : SignInChallengeActions {
    private const val KEY_SECRET_HASH = "SECRET_HASH"
    private const val KEY_USERNAME = "USERNAME"
    override fun verifyChallengeAuthAction(
        event: SignInChallengeEvent.EventType.VerifyChallengeAnswer,
        challenge: AuthChallenge
    ): Action = Action<AuthEnvironment>("VerifySignInChallenge") { id, dispatcher ->
        logger?.verbose("$id Starting execution")
        val evt = try {
            val username = challenge.username
            val challengeResponses = mutableMapOf<String, String>()

            if (!username.isNullOrEmpty()) {
                challengeResponses[KEY_USERNAME] = username
            }

            getChallengeResponseKey(challenge.challengeName)?.also { responseKey ->
                challengeResponses[responseKey] = event.answer
            }
            event.options.forEach { (key, value) ->
                challengeResponses[key] = value
            }

            val secretHash = AuthHelper.getSecretHash(
                username,
                configuration.userPool?.appClient,
                configuration.userPool?.appClientSecret
            )
            secretHash?.let { challengeResponses[KEY_SECRET_HASH] = it }
            val encodedContextData = username?.let { userContextDataProvider?.getEncodedContextData(it) }

            val response = cognitoAuthService.cognitoIdentityProviderClient?.respondToAuthChallenge {
                clientId = configuration.userPool?.appClient
                challengeName = ChallengeNameType.fromValue(challenge.challengeName)
                this.challengeResponses = challengeResponses
                session = challenge.session
                encodedContextData?.let { this.userContextData { encodedData = it } }
            }
            response?.let {
                SignInChallengeHelper.evaluateNextStep(
                    username = username ?: "",
                    challengeNameType = response.challengeName,
                    session = response.session,
                    challengeParameters = response.challengeParameters,
                    authenticationResult = response.authenticationResult
                )
            } ?: CustomSignInEvent(
                CustomSignInEvent.EventType.ThrowAuthError(
                    AuthException(
                        "Sign in failed",
                        AuthException.TODO_RECOVERY_SUGGESTION
                    )
                )
            )
        } catch (e: Exception) {
            SignInEvent(SignInEvent.EventType.ThrowError(e))
        }
        logger?.verbose("$id Sending event ${evt.type}")
        dispatcher.send(evt)
    }

    private fun getChallengeResponseKey(challengeName: String): String? {
        val VALUE_ANSWER = "ANSWER"
        val VALUE_SMS_MFA = "SMS_MFA_CODE"
        val VALUE_NEW_PASSWORD = "NEW_PASSWORD"
        return when (ChallengeNameType.fromValue(challengeName)) {
            is ChallengeNameType.SmsMfa -> VALUE_SMS_MFA
            is ChallengeNameType.NewPasswordRequired -> VALUE_NEW_PASSWORD
            is ChallengeNameType.CustomChallenge -> VALUE_ANSWER
            else -> null
        }
    }
}
