/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
import com.amplifyframework.auth.exceptions.ServiceException
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.UserAuthSignInActions
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent

internal object UserAuthSignInCognitoActions : UserAuthSignInActions {
    private const val KEY_SECRET_HASH = "SECRET_HASH"
    private const val KEY_USERNAME = "USERNAME"
    private const val KEY_DEVICE_KEY = "DEVICE_KEY"
    private const val KEY_USERID_FOR_SRP = "USER_ID_FOR_SRP"
    private const val KEY_PREFERRED_CHALLENGE = "PREFERRED_CHALLENGE"

    override fun initiateUserAuthSignIn(event: SignInEvent.EventType.InitiateUserAuth): Action =
        Action<AuthEnvironment>("InitUserAuth") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val secretHash = AuthHelper.getSecretHash(
                    event.username,
                    configuration.userPool?.appClient,
                    configuration.userPool?.appClientSecret
                )

                val authParams = mutableMapOf(KEY_USERNAME to event.username)

                secretHash?.let { authParams[KEY_SECRET_HASH] = it }

                event.preferredChallenge?.let { authParams[KEY_PREFERRED_CHALLENGE] = it.toString() }

                val encodedContextData = getUserContextData(event.username)
                val deviceMetadata = getDeviceMetadata(event.username)
                deviceMetadata?.let { authParams[KEY_DEVICE_KEY] = it.deviceKey }
                val pinpointEndpointId = getPinpointEndpointId()

                val initiateAuthResponse = cognitoAuthService.cognitoIdentityProviderClient?.initiateAuth {
                    authFlow = AuthFlowType.UserAuth
                    clientId = configuration.userPool?.appClient
                    authParameters = authParams
                    clientMetadata = event.metadata
                    pinpointEndpointId?.let { analyticsMetadata { analyticsEndpointId = it } }
                    encodedContextData?.let { userContextData { encodedData = it } }
                }

                val resolvedSession = initiateAuthResponse?.session
                val resolvedChallenges = initiateAuthResponse?.availableChallenges
                if (initiateAuthResponse?.challengeName == ChallengeNameType.SelectChallenge &&
                    resolvedSession != null &&
                    resolvedChallenges != null
                ) {
                    val activeUserName = AuthHelper.getActiveUsername(
                        username = event.username,
                        alternateUsername = initiateAuthResponse.challengeParameters?.get(KEY_USERNAME),
                        userIDForSRP = initiateAuthResponse.challengeParameters?.get(
                            KEY_USERID_FOR_SRP
                        )
                    )

                    val listOfChallenges = resolvedChallenges.map { it.value }

                    SignInChallengeHelper.evaluateNextStep(
                        username = activeUserName,
                        challengeNameType = ChallengeNameType.SelectChallenge,
                        session = resolvedSession,
                        availableChallenges = listOfChallenges,
                        authenticationResult = initiateAuthResponse.authenticationResult,
                        callingActivity = event.callingActivity,
                        signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_AUTH)
                    )
                } else if (isSupportedChallenge(initiateAuthResponse?.challengeName) &&
                    initiateAuthResponse?.challengeParameters != null &&
                    resolvedSession != null
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
                        session = resolvedSession,
                        challengeParameters = initiateAuthResponse.challengeParameters,
                        authenticationResult = initiateAuthResponse.authenticationResult,
                        callingActivity = event.callingActivity,
                        signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_AUTH)
                    )
                } else {
                    throw ServiceException("Sign in failed", AmplifyException.TODO_RECOVERY_SUGGESTION)
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

    private fun isSupportedChallenge(challengeName: ChallengeNameType?): Boolean = challengeName != null &&
        (
            challengeName is ChallengeNameType.EmailOtp ||
                challengeName is ChallengeNameType.SmsOtp ||
                challengeName is ChallengeNameType.WebAuthn
            )
}
