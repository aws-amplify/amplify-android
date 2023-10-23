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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
import com.amplifyframework.auth.exceptions.ServiceException
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.CustomSignInActions
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.CustomSignInEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent

internal object SignInCustomCognitoActions : CustomSignInActions {
    private const val KEY_SECRET_HASH = "SECRET_HASH"
    private const val KEY_USERNAME = "USERNAME"
    private const val KEY_DEVICE_KEY = "DEVICE_KEY"
    private const val KEY_USERID_FOR_SRP = "USER_ID_FOR_SRP"
    override fun initiateCustomSignInAuthAction(event: CustomSignInEvent.EventType.InitiateCustomSignIn): Action =
        Action<AuthEnvironment>("InitCustomAuth") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val secretHash = AuthHelper.getSecretHash(
                    event.username,
                    configuration.userPool?.appClient,
                    configuration.userPool?.appClientSecret
                )

                val authParams = mutableMapOf(KEY_USERNAME to event.username)
                secretHash?.let { authParams[KEY_SECRET_HASH] = it }

                val encodedContextData = getUserContextData(event.username)
                val deviceMetadata = getDeviceMetadata(event.username)
                deviceMetadata?.let { authParams[KEY_DEVICE_KEY] = it.deviceKey }
                val pinpointEndpointId = getPinpointEndpointId()

                val initiateAuthResponse = cognitoAuthService.cognitoIdentityProviderClient?.initiateAuth {
                    authFlow = AuthFlowType.CustomAuth
                    clientId = configuration.userPool?.appClient
                    authParameters = authParams
                    clientMetadata = event.metadata
                    pinpointEndpointId?.let { analyticsMetadata { analyticsEndpointId = it } }
                    encodedContextData?.let { userContextData { encodedData = it } }
                }

                if (initiateAuthResponse?.challengeName == ChallengeNameType.CustomChallenge &&
                    initiateAuthResponse.challengeParameters != null
                ) {
                    val activeUserName = AuthHelper.getActiveUsername(
                        username = event.username,
                        alternateUsername = initiateAuthResponse.challengeParameters?.get(KEY_USERNAME),
                        userIDForSRP = initiateAuthResponse.challengeParameters?.get(
                            KEY_USERID_FOR_SRP
                        )
                    )
                    SignInChallengeHelper.evaluateNextStep(
                        username = activeUserName,
                        challengeNameType = initiateAuthResponse.challengeName,
                        session = initiateAuthResponse.session,
                        challengeParameters = initiateAuthResponse.challengeParameters,
                        authenticationResult = initiateAuthResponse.authenticationResult,
                        signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.CUSTOM_AUTH)
                    )
                } else {
                    throw ServiceException(
                        "This sign in method is not supported",
                        "Please consult our docs for supported sign in methods"
                    )
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
