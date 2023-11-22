/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ResourceNotFoundException
import aws.sdk.kotlin.services.cognitoidentityprovider.respondToAuthChallenge
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SignInChallengeActions
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.events.CustomSignInEvent
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent

internal object SignInChallengeCognitoActions : SignInChallengeActions {
    private const val KEY_SECRET_HASH = "SECRET_HASH"
    private const val KEY_USERNAME = "USERNAME"
    private const val KEY_PREFIX_USER_ATTRIBUTE = "userAttributes."
    override fun verifyChallengeAuthAction(
        answer: String,
        metadata: Map<String, String>,
        attributes: List<AuthUserAttribute>,
        challenge: AuthChallenge
    ): Action = Action<AuthEnvironment>("VerifySignInChallenge") { id, dispatcher ->
        logger.verbose("$id Starting execution")
        val evt = try {
            val username = challenge.username
            val challengeResponses = mutableMapOf<String, String>()

            if (!username.isNullOrEmpty()) {
                challengeResponses[KEY_USERNAME] = username
            }

            getChallengeResponseKey(challenge.challengeName)?.also { responseKey ->
                challengeResponses[responseKey] = answer
            }

            challengeResponses.putAll(
                attributes.map {
                    Pair("${KEY_PREFIX_USER_ATTRIBUTE}${it.key.keyString}", it.value)
                }
            )

            val secretHash = AuthHelper.getSecretHash(
                username,
                configuration.userPool?.appClient,
                configuration.userPool?.appClientSecret
            )
            secretHash?.let { challengeResponses[KEY_SECRET_HASH] = it }

            val encodedContextData = username?.let { getUserContextData(it) }
            val pinpointEndpointId = getPinpointEndpointId()
            val response = cognitoAuthService.cognitoIdentityProviderClient?.respondToAuthChallenge {
                clientId = configuration.userPool?.appClient
                challengeName = ChallengeNameType.fromValue(challenge.challengeName)
                this.challengeResponses = challengeResponses
                session = challenge.session
                clientMetadata = metadata
                pinpointEndpointId?.let { analyticsMetadata { analyticsEndpointId = it } }
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
                    UnknownException("Sign in failed")
                )
            )
        } catch (e: Exception) {
            if (e is ResourceNotFoundException) {
                challenge.username?.let { username ->
                    credentialStoreClient.clearCredentials(CredentialType.Device(username))
                }
                SignInChallengeEvent(
                    SignInChallengeEvent.EventType.RetryVerifyChallengeAnswer(
                        answer,
                        metadata,
                        attributes,
                        challenge
                    )
                )
            } else {
                SignInChallengeEvent(SignInChallengeEvent.EventType.ThrowError(e, challenge, true))
            }
        }
        logger.verbose("$id Sending event ${evt.type}")
        dispatcher.send(evt)
    }

    private fun getChallengeResponseKey(challengeName: String): String? {
        return when (ChallengeNameType.fromValue(challengeName)) {
            is ChallengeNameType.SmsMfa -> "SMS_MFA_CODE"
            is ChallengeNameType.NewPasswordRequired -> "NEW_PASSWORD"
            is ChallengeNameType.CustomChallenge, ChallengeNameType.SelectMfaType -> "ANSWER"
            is ChallengeNameType.SoftwareTokenMfa -> "SOFTWARE_TOKEN_MFA_CODE"
            else -> null
        }
    }
}
