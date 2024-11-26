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

import aws.sdk.kotlin.services.cognitoidentityprovider.initiateAuth
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import aws.sdk.kotlin.services.cognitoidentityprovider.respondToAuthChallenge
import aws.smithy.kotlin.runtime.util.type
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
import com.amplifyframework.auth.cognito.helpers.toCognitoType
import com.amplifyframework.auth.cognito.helpers.toSignInMethod
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.exceptions.ServiceException
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.MigrateAuthActions
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent

internal object MigrateAuthCognitoActions : MigrateAuthActions {
    private const val KEY_USERNAME = "USERNAME"
    private const val KEY_PASSWORD = "PASSWORD"
    private const val KEY_SECRET_HASH = "SECRET_HASH"
    private const val KEY_USERID_FOR_SRP = "USER_ID_FOR_SRP"
    private const val KEY_ANSWER = "ANSWER"
    private const val KEY_PREFERRED_CHALLENGE = "PREFERRED_CHALLENGE"

    override fun initiateMigrateAuthAction(event: SignInEvent.EventType.InitiateMigrateAuth) =
        Action<AuthEnvironment>("InitMigrateAuth") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val secretHash = AuthHelper.getSecretHash(
                    event.username,
                    configuration.userPool?.appClient,
                    configuration.userPool?.appClientSecret
                )

                val authParams = mutableMapOf(KEY_USERNAME to event.username, KEY_PASSWORD to event.password)
                secretHash?.let { authParams[KEY_SECRET_HASH] = it }

                val encodedContextData = getUserContextData(event.username)
                val pinpointEndpointId = getPinpointEndpointId()

                if (event.respondToAuthChallenge?.session != null) {
                    authParams[KEY_ANSWER] = ChallengeNameType.Password.value

                    val response = cognitoAuthService.cognitoIdentityProviderClient?.respondToAuthChallenge {
                        clientId = configuration.userPool?.appClient
                        challengeName = ChallengeNameType.SelectChallenge
                        this.challengeResponses = authParams
                        session = event.respondToAuthChallenge.session
                        clientMetadata = event.metadata
                        pinpointEndpointId?.let { analyticsMetadata { analyticsEndpointId = it } }
                        encodedContextData?.let { this.userContextData { encodedData = it } }
                    }

                    response?.let {
                        SignInChallengeHelper.evaluateNextStep(
                            username = event.username,
                            challengeNameType = response.challengeName,
                            session = response.session,
                            challengeParameters = response.challengeParameters,
                            authenticationResult = response.authenticationResult,
                            signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_AUTH)
                        )
                    } ?: throw ServiceException("Sign in failed", AmplifyException.TODO_RECOVERY_SUGGESTION)
                } else {
                    if (event.authFlowType == AuthFlowType.USER_AUTH) {
                        authParams[KEY_PREFERRED_CHALLENGE] = KEY_PASSWORD
                    }
                    val response = cognitoAuthService.cognitoIdentityProviderClient?.initiateAuth {
                        authFlow = event.authFlowType.toCognitoType()
                        clientId = configuration.userPool?.appClient
                        authParameters = authParams
                        clientMetadata = event.metadata
                        pinpointEndpointId?.let { analyticsMetadata { analyticsEndpointId = it } }
                        encodedContextData?.let { userContextData { encodedData = it } }
                    }

                    response?.let {
                        val username = AuthHelper.getActiveUsername(
                            username = event.username,
                            alternateUsername = response.challengeParameters?.get(KEY_USERNAME),
                            userIDForSRP = response.challengeParameters?.get(
                                KEY_USERID_FOR_SRP
                            )
                        )
                        SignInChallengeHelper.evaluateNextStep(
                            username = username,
                            challengeNameType = response.challengeName,
                            session = response.session,
                            challengeParameters = response.challengeParameters,
                            authenticationResult = response.authenticationResult,
                            signInMethod = event.authFlowType.toSignInMethod()
                        )
                    } ?: throw ServiceException("Sign in failed", AmplifyException.TODO_RECOVERY_SUGGESTION)
                }
            } catch (e: Exception) {
                val errorEvent = SignInEvent(SignInEvent.EventType.ThrowError(e))
                logger.verbose("$id Sending event ${errorEvent.type}")
                dispatcher.send(errorEvent)

                AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
