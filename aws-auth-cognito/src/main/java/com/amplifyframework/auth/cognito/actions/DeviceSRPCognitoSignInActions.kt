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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.NotAuthorizedException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.RespondToAuthChallengeRequest
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
import com.amplifyframework.auth.exceptions.ServiceException
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.DeviceSRPSignInActions
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.DeviceSRPSignInEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent

internal object DeviceSRPCognitoSignInActions : DeviceSRPSignInActions {

    private const val KEY_PASSWORD_CLAIM_SECRET_BLOCK = "PASSWORD_CLAIM_SECRET_BLOCK"
    private const val KEY_PASSWORD_CLAIM_SIGNATURE = "PASSWORD_CLAIM_SIGNATURE"
    private const val KEY_TIMESTAMP = "TIMESTAMP"
    private const val KEY_SALT = "SALT"
    private const val KEY_SECRET_BLOCK = "SECRET_BLOCK"
    private const val KEY_SRP_A = "SRP_A"
    private const val KEY_SECRET_HASH = "SECRET_HASH"
    private const val KEY_SRP_B = "SRP_B"
    private const val KEY_USERNAME = "USERNAME"
    private const val KEY_DEVICE_KEY = "DEVICE_KEY"
    private const val KEY_DEVICE_GROUP_KEY = "DEVICE_GROUP_KEY"

    override fun respondDeviceSRP(event: DeviceSRPSignInEvent.EventType.RespondDeviceSRPChallenge): Action =
        Action<AuthEnvironment>("RespondDeviceSRP") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val username = event.username
            val evt = try {
                val encodedContextData = getUserContextData(username)
                val deviceMetadata = getDeviceMetadata(username)
                val pinpointEndpointId = getPinpointEndpointId()

                srpHelper = SRPHelper(deviceMetadata?.deviceSecret ?: "")

                val challengeResponse = mutableMapOf(
                    KEY_USERNAME to username,
                    KEY_DEVICE_KEY to (deviceMetadata?.deviceKey ?: ""),
                    KEY_SRP_A to srpHelper.getPublicA()
                )

                val secretHash = AuthHelper.getSecretHash(
                    username,
                    configuration.userPool?.appClient,
                    configuration.userPool?.appClientSecret
                )
                secretHash?.let { challengeResponse[KEY_SECRET_HASH] = it }

                cognitoAuthService.cognitoIdentityProviderClient?.let { client ->
                    val respondToAuthChallenge = client.respondToAuthChallenge(
                        RespondToAuthChallengeRequest.invoke {
                            challengeName = ChallengeNameType.DeviceSrpAuth
                            clientId = configuration.userPool?.appClient
                            challengeResponses = challengeResponse
                            clientMetadata = event.metadata
                            pinpointEndpointId?.let { analyticsMetadata { analyticsEndpointId = it } }
                            encodedContextData?.let { userContextData { encodedData = it } }
                        }
                    )

                    respondToAuthChallenge.challengeParameters?.let { params ->
                        var challengeParams = params
                        deviceMetadata?.let {
                            challengeParams = challengeParams.plus(KEY_DEVICE_KEY to it.deviceKey)
                            challengeParams = challengeParams.plus(KEY_DEVICE_GROUP_KEY to it.deviceGroupKey)
                        }

                        DeviceSRPSignInEvent(
                            DeviceSRPSignInEvent.EventType.RespondDevicePasswordVerifier(
                                challengeParams,
                                event.metadata
                            )
                        )
                    } ?: throw ServiceException(
                        "Challenge params are empty.",
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                    )
                } ?: throw InvalidUserPoolConfigurationException()
            } catch (exception: Exception) {
                if (exception is NotAuthorizedException) {
                    credentialStoreClient.clearCredentials(CredentialType.Device(username))
                }
                val errorEvent = DeviceSRPSignInEvent(DeviceSRPSignInEvent.EventType.ThrowAuthError(exception))
                logger.verbose("$id Sending event ${errorEvent.type}")
                dispatcher.send(errorEvent)
                val errorEvent2 = SignInEvent(SignInEvent.EventType.ThrowError(exception))
                logger.verbose("$id Sending event ${errorEvent.type}")
                dispatcher.send(errorEvent2)
                AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun respondDevicePasswordVerifier(
        event: DeviceSRPSignInEvent.EventType.RespondDevicePasswordVerifier
    ): Action =
        Action<AuthEnvironment>("RespondToDevicePasswordVerifier") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val params = event.challengeParameters
            val username = params.getValue(KEY_USERNAME)
            val evt = try {
                val salt = params.getValue(KEY_SALT)
                val secretBlock = params.getValue(KEY_SECRET_BLOCK)
                val srpB = params.getValue(KEY_SRP_B)
                val deviceKey = params.getValue(KEY_DEVICE_KEY)
                val deviceGroupKey = params.getValue(KEY_DEVICE_GROUP_KEY)

                val encodedContextData = getUserContextData(username)
                val pinpointEndpointId = getPinpointEndpointId()

                srpHelper.setUserPoolParams(deviceKey, deviceGroupKey)

                val challengeResponse = mutableMapOf(
                    KEY_USERNAME to username,
                    KEY_PASSWORD_CLAIM_SECRET_BLOCK to secretBlock,
                    KEY_TIMESTAMP to srpHelper.dateString,
                    KEY_PASSWORD_CLAIM_SIGNATURE to srpHelper.getSignature(salt, srpB, secretBlock),
                    KEY_DEVICE_KEY to deviceKey
                )

                val secretHash = AuthHelper.getSecretHash(
                    username,
                    configuration.userPool?.appClient,
                    configuration.userPool?.appClientSecret
                )
                secretHash?.let { challengeResponse[KEY_SECRET_HASH] = it }

                cognitoAuthService.cognitoIdentityProviderClient?.let {
                    val respondToAuthChallenge = it.respondToAuthChallenge(
                        RespondToAuthChallengeRequest.invoke {
                            challengeName = ChallengeNameType.DevicePasswordVerifier
                            clientId = configuration.userPool?.appClient
                            challengeResponses = challengeResponse
                            clientMetadata = event.metadata
                            pinpointEndpointId?.let { analyticsMetadata { analyticsEndpointId = it } }
                            encodedContextData?.let { userContextData { encodedData = it } }
                        }
                    )

                    DeviceSRPSignInEvent(DeviceSRPSignInEvent.EventType.FinalizeSignIn())
                    SignInChallengeHelper.evaluateNextStep(
                        username = username,
                        signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
                        authenticationResult = respondToAuthChallenge.authenticationResult,
                        challengeNameType = respondToAuthChallenge.challengeName,
                        challengeParameters = respondToAuthChallenge.challengeParameters,
                        session = respondToAuthChallenge.session,
                    )
                } ?: throw InvalidUserPoolConfigurationException()
            } catch (exception: Exception) {
                if (exception is NotAuthorizedException) {
                    credentialStoreClient.clearCredentials(CredentialType.Device(username))
                }
                val errorEvent = DeviceSRPSignInEvent(
                    DeviceSRPSignInEvent.EventType.ThrowPasswordVerifiedError(exception)
                )
                logger.verbose("$id Sending event ${errorEvent.type}")
                dispatcher.send(errorEvent)
                val errorEvent2 = SignInEvent(SignInEvent.EventType.ThrowError(exception))
                logger.verbose("$id Sending event ${errorEvent.type}")
                dispatcher.send(errorEvent2)
                AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
