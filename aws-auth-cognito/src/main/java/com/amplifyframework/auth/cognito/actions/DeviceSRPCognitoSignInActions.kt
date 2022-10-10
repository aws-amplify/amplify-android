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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.RespondToAuthChallengeRequest
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.exceptions.configuration.InvalidUserPoolConfigurationException
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.DeviceSRPSignInActions
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.DeviceSRPSignInEvent

object DeviceSRPCognitoSignInActions : DeviceSRPSignInActions {

    private const val KEY_PASSWORD_CLAIM_SECRET_BLOCK = "PASSWORD_CLAIM_SECRET_BLOCK"
    private const val KEY_PASSWORD_CLAIM_SIGNATURE = "PASSWORD_CLAIM_SIGNATURE"
    private const val KEY_TIMESTAMP = "TIMESTAMP"
    private const val KEY_SALT = "SALT"
    private const val KEY_SECRET_BLOCK = "SECRET_BLOCK"
    private const val KEY_SRP_A = "SRP_A"
    private const val KEY_SRP_B = "SRP_B"
    private const val KEY_USERNAME = "USERNAME"
    private const val KEY_DEVICE_KEY = "DEVICE_KEY"

    override fun respondDeviceSRP(event: DeviceSRPSignInEvent.EventType.RespondDeviceSRPChallenge): Action =
        Action<AuthEnvironment>("RespondToDevicePasswordVerifier") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                event.challengeParameters?.let { params ->
                    val username = params.getValue(KEY_USERNAME)
                    val encodedContextData = userContextDataProvider?.getEncodedContextData(username)

                    val deviceCredentials = credentialStoreClient.loadCredentials(CredentialType.Device(username))
                    val deviceMetadata = (deviceCredentials as AmplifyCredential.DeviceData)
                        .deviceMetadata as? DeviceMetadata.Metadata

                    srpHelper = SRPHelper(deviceMetadata?.deviceSecret ?: "")

                    cognitoAuthService.cognitoIdentityProviderClient?.let {
                        val respondToAuthChallenge = it.respondToAuthChallenge(
                            RespondToAuthChallengeRequest.invoke {
                                challengeName = ChallengeNameType.DeviceSrpAuth
                                clientId = configuration.userPool?.appClient
                                challengeResponses = mapOf(
                                    KEY_USERNAME to username,
                                    KEY_DEVICE_KEY to (deviceMetadata?.deviceKey ?: ""),
                                    KEY_SRP_A to srpHelper.getPublicA()
                                )
                                encodedContextData?.let { userContextData { encodedData = it } }
                            }
                        )

                        DeviceSRPSignInEvent(
                            DeviceSRPSignInEvent.EventType.RespondDevicePasswordVerifier(
                                respondToAuthChallenge.challengeParameters
                            )
                        )
                    } ?: throw InvalidUserPoolConfigurationException()
                } ?: throw UnknownException("There was a problem while signing you in")
            } catch (e: Exception) {
                val errorEvent = DeviceSRPSignInEvent(DeviceSRPSignInEvent.EventType.CancelSRPSignIn())
                logger.verbose("$id Sending event ${errorEvent.type}")
                dispatcher.send(errorEvent)
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
            val evt = try {
                event.challengeParameters?.let { params ->
                    val salt = params.getValue(KEY_SALT)
                    val secretBlock = params.getValue(KEY_SECRET_BLOCK)
                    val srpB = params.getValue(KEY_SRP_B)
                    val username = params.getValue(KEY_USERNAME)
                    val deviceKey = params.getValue(KEY_DEVICE_KEY)
                    val encodedContextData = userContextDataProvider?.getEncodedContextData(username)

                    val deviceCredentials = credentialStoreClient.loadCredentials(CredentialType.Device(username))
                    val deviceMetadata = (deviceCredentials as AmplifyCredential.DeviceData)
                        .deviceMetadata as? DeviceMetadata.Metadata

                    srpHelper.setUserPoolParams(deviceMetadata?.deviceKey ?: "", deviceMetadata?.deviceGroupKey ?: "")

                    cognitoAuthService.cognitoIdentityProviderClient?.let {
                        val respondToAuthChallenge = it.respondToAuthChallenge(
                            RespondToAuthChallengeRequest.invoke {
                                challengeName = ChallengeNameType.DevicePasswordVerifier
                                clientId = configuration.userPool?.appClient
                                challengeResponses = mapOf(
                                    KEY_USERNAME to username,
                                    KEY_PASSWORD_CLAIM_SECRET_BLOCK to secretBlock,
                                    KEY_TIMESTAMP to srpHelper.dateString,
                                    KEY_PASSWORD_CLAIM_SIGNATURE to srpHelper.getSignature(salt, srpB, secretBlock),
                                    KEY_DEVICE_KEY to deviceKey
                                )
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
                } ?: throw UnknownException("There was a problem while signing you in")
            } catch (e: Exception) {
                val errorEvent = DeviceSRPSignInEvent(DeviceSRPSignInEvent.EventType.CancelSRPSignIn())
                logger.verbose("$id Sending event ${errorEvent.type}")
                dispatcher.send(errorEvent)
                AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun cancellingSignIn(event: DeviceSRPSignInEvent.EventType.CancelSRPSignIn): Action =
        Action<AuthEnvironment>("CancelSignIn") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = DeviceSRPSignInEvent(DeviceSRPSignInEvent.EventType.RestoreToNotInitialized())
            dispatcher.send(evt)
        }
}
