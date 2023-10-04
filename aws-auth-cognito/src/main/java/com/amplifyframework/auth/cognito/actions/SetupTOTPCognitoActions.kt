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

import aws.sdk.kotlin.services.cognitoidentityprovider.associateSoftwareToken
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.VerifySoftwareTokenResponseType
import aws.sdk.kotlin.services.cognitoidentityprovider.respondToAuthChallenge
import aws.sdk.kotlin.services.cognitoidentityprovider.verifySoftwareToken
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
import com.amplifyframework.auth.exceptions.ServiceException
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SetupTOTPActions
import com.amplifyframework.statemachine.codegen.data.SignInTOTPSetupData
import com.amplifyframework.statemachine.codegen.events.SetupTOTPEvent

internal object SetupTOTPCognitoActions : SetupTOTPActions {
    private const val KEY_DEVICE_KEY = "DEVICE_KEY"
    override fun initiateTOTPSetup(eventType: SetupTOTPEvent.EventType.SetupTOTP): Action = Action<AuthEnvironment>(
        "InitiateTOTPSetup"
    ) { id, dispatcher ->
        logger.verbose("$id Starting execution")
        val evt = try {
            val response = cognitoAuthService.cognitoIdentityProviderClient?.associateSoftwareToken {
                session = eventType.totpSetupDetails.session
            }
            response?.secretCode?.let { secret ->
                SetupTOTPEvent(
                    SetupTOTPEvent.EventType.WaitForAnswer(
                        SignInTOTPSetupData(secret, response.session, eventType.totpSetupDetails.username)
                    )
                )
            } ?: SetupTOTPEvent(
                SetupTOTPEvent.EventType.ThrowAuthError(
                    Exception("Software token setup failed"),
                    eventType.totpSetupDetails.username,
                    eventType.totpSetupDetails.session
                )
            )
        } catch (e: Exception) {
            SetupTOTPEvent(
                SetupTOTPEvent.EventType.ThrowAuthError(
                    e,
                    eventType.totpSetupDetails.username,
                    eventType.totpSetupDetails.session
                )
            )
        }
        logger.verbose("$id Sending event ${evt.type}")
        dispatcher.send(evt)
    }

    override fun verifyChallengeAnswer(
        eventType: SetupTOTPEvent.EventType.VerifyChallengeAnswer
    ): Action =
        Action<AuthEnvironment>("verifyChallengeAnswer") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val response = cognitoAuthService.cognitoIdentityProviderClient?.verifySoftwareToken {
                    userCode = eventType.answer
                    this.session = eventType.session
                    this.friendlyDeviceName = eventType.friendlyDeviceName
                }

                response?.let {
                    when (it.status) {
                        is VerifySoftwareTokenResponseType.Success -> {
                            SetupTOTPEvent(
                                SetupTOTPEvent.EventType.RespondToAuthChallenge(
                                    eventType.username,
                                    it.session
                                )
                            )
                        }
                        else -> {
                            SetupTOTPEvent(
                                SetupTOTPEvent.EventType.ThrowAuthError(
                                    ServiceException(
                                        message = "An unknown service error has occurred",
                                        recoverySuggestion = AmplifyException.TODO_RECOVERY_SUGGESTION
                                    ),
                                    eventType.username,
                                    eventType.session
                                )
                            )
                        }
                    }
                } ?: SetupTOTPEvent(
                    SetupTOTPEvent.EventType.ThrowAuthError(
                        Exception("Software token verification failed"),
                        eventType.username,
                        eventType.session
                    )
                )
            } catch (exception: Exception) {
                SetupTOTPEvent(
                    SetupTOTPEvent.EventType.ThrowAuthError(
                        exception,
                        eventType.username,
                        eventType.session
                    )
                )
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun respondToAuthChallenge(
        eventType: SetupTOTPEvent.EventType.RespondToAuthChallenge
    ): Action =
        Action<AuthEnvironment>("RespondToAuthChallenge") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val challengeResponses = mutableMapOf<String, String>()
                challengeResponses["USERNAME"] = eventType.username
                val deviceMetadata = getDeviceMetadata(eventType.username)
                deviceMetadata?.deviceKey?.let { challengeResponses[KEY_DEVICE_KEY] = it }
                val encodedContextData = getUserContextData(eventType.username)
                val pinpointEndpointId = getPinpointEndpointId()

                val response = cognitoAuthService.cognitoIdentityProviderClient?.respondToAuthChallenge {
                    this.session = eventType.session
                    this.challengeResponses = challengeResponses
                    challengeName = ChallengeNameType.MfaSetup
                    clientId = configuration.userPool?.appClient
                    pinpointEndpointId?.let { analyticsMetadata { analyticsEndpointId = it } }
                    encodedContextData?.let { this.userContextData { encodedData = it } }
                }

                response?.let {
                    SignInChallengeHelper.evaluateNextStep(
                        username = eventType.username,
                        challengeNameType = response.challengeName,
                        session = response.session,
                        challengeParameters = response.challengeParameters,
                        authenticationResult = response.authenticationResult
                    )
                } ?: SetupTOTPEvent(
                    SetupTOTPEvent.EventType.ThrowAuthError(
                        Exception("Software token verification failed"),
                        eventType.username,
                        eventType.session
                    )
                )
            } catch (exception: Exception) {
                SetupTOTPEvent(
                    SetupTOTPEvent.EventType.ThrowAuthError(exception, eventType.username, eventType.session)
                )
            }
            dispatcher.send(evt)
        }
}
