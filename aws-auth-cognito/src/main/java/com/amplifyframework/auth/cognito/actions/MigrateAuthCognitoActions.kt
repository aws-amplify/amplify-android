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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthFlowType
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.MigrateAuthActions
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent

object MigrateAuthCognitoActions : MigrateAuthActions {
    private const val KEY_USERNAME = "USERNAME"
    private const val KEY_PASSWORD = "PASSWORD"
    private const val KEY_SECRET_HASH = "SECRET_HASH"

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
                val encodedContextData = userContextDataProvider?.getEncodedContextData(event.username)

                val response = cognitoAuthService.cognitoIdentityProviderClient?.initiateAuth {
                    authFlow = AuthFlowType.UserPasswordAuth
                    clientId = configuration.userPool?.appClient
                    authParameters = authParams
                    clientMetadata = event.metadata
                    encodedContextData?.let { userContextData { encodedData = it } }
                }

                if (response != null) {
                    SignInChallengeHelper.evaluateNextStep(
                        event.username,
                        response.challengeName,
                        response.session,
                        response.challengeParameters,
                        response.authenticationResult
                    )
                } else {
                    throw AuthException("Sign in failed", AuthException.TODO_RECOVERY_SUGGESTION)
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
