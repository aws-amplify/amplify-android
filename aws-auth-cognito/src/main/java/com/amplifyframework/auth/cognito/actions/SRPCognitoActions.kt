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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ResourceNotFoundException
import aws.sdk.kotlin.services.cognitoidentityprovider.respondToAuthChallenge
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
import com.amplifyframework.auth.cognito.helpers.toCognitoType
import com.amplifyframework.auth.cognito.options.AuthFlowType
import com.amplifyframework.auth.exceptions.ServiceException
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SRPActions
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.SRPEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent

internal object SRPCognitoActions : SRPActions {
    private const val KEY_PASSWORD_CLAIM_SECRET_BLOCK = "PASSWORD_CLAIM_SECRET_BLOCK"
    private const val KEY_PASSWORD_CLAIM_SIGNATURE = "PASSWORD_CLAIM_SIGNATURE"
    private const val KEY_TIMESTAMP = "TIMESTAMP"
    private const val KEY_SALT = "SALT"
    private const val KEY_SECRET_BLOCK = "SECRET_BLOCK"
    private const val KEY_SRP_A = "SRP_A"
    private const val VALUE_SRP_A = "SRP_A"
    private const val KEY_SRP_B = "SRP_B"
    private const val KEY_USER_ID_FOR_SRP = "USER_ID_FOR_SRP"
    private const val KEY_SECRET_HASH = "SECRET_HASH"
    private const val KEY_USERNAME = "USERNAME"
    private const val KEY_USERID_FOR_SRP = "USER_ID_FOR_SRP"
    private const val KEY_DEVICE_KEY = "DEVICE_KEY"
    private const val KEY_CHALLENGE_NAME = "CHALLENGE_NAME"
    private const val KEY_ANSWER = "ANSWER"
    private const val KEY_PREFERRED_CHALLENGE = "PREFERRED_CHALLENGE"
    private const val KEY_PASSWORD_SRP = "PASSWORD_SRP"

    override fun initiateSRPAuthAction(event: SRPEvent.EventType.InitiateSRP) =
        Action<AuthEnvironment>("InitSRPAuth") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                srpHelper = SRPHelper(event.password)

                val secretHash = AuthHelper.getSecretHash(
                    event.username,
                    configuration.userPool?.appClient,
                    configuration.userPool?.appClientSecret
                )

                val authParams = mutableMapOf(KEY_USERNAME to event.username, KEY_SRP_A to srpHelper.getPublicA())
                secretHash?.let { authParams[KEY_SECRET_HASH] = it }

                val encodedContextData = getUserContextData(event.username)
                val deviceMetadata = getDeviceMetadata(event.username)
                deviceMetadata?.let { authParams[KEY_DEVICE_KEY] = it.deviceKey }
                val pinpointEndpointId = getPinpointEndpointId()

                if (event.respondToAuthChallenge?.session != null) {

                    authParams[KEY_ANSWER] = ChallengeNameType.PasswordSrp.value

                    val response = cognitoAuthService.cognitoIdentityProviderClient?.respondToAuthChallenge {
                        clientId = configuration.userPool?.appClient
                        challengeName = ChallengeNameType.SelectChallenge
                        this.challengeResponses = authParams
                        session = event.respondToAuthChallenge.session
                        clientMetadata = event.metadata
                        pinpointEndpointId?.let { analyticsMetadata { analyticsEndpointId = it } }
                        encodedContextData?.let { this.userContextData { encodedData = it } }
                    }

                    val updatedDeviceMetadata = getDeviceMetadata(
                        AuthHelper.getActiveUsername(
                            username = event.username,
                            alternateUsername = response?.challengeParameters?.get(KEY_USERNAME),
                            userIDForSRP = response?.challengeParameters?.get(
                                KEY_USERID_FOR_SRP
                            )
                        )
                    )

                    parseResponseChallenge(
                        challengeNameType = response?.challengeName,
                        challengeParams = response?.challengeParameters,
                        session = response?.session,
                        updatedDeviceMetadata = updatedDeviceMetadata,
                        metadata = event.metadata
                    )
                } else {
                    if (event.authFlowType == AuthFlowType.USER_AUTH) {
                        authParams[KEY_PREFERRED_CHALLENGE] = KEY_PASSWORD_SRP
                    }
                    val initiateAuthResponse = cognitoAuthService.cognitoIdentityProviderClient?.initiateAuth {
                        authFlow = event.authFlowType.toCognitoType()
                        clientId = configuration.userPool?.appClient
                        authParameters = authParams
                        clientMetadata = event.metadata
                        pinpointEndpointId?.let { analyticsMetadata { analyticsEndpointId = it } }
                        encodedContextData?.let { userContextData { encodedData = it } }
                    }

                    val updatedDeviceMetadata = getDeviceMetadata(
                        AuthHelper.getActiveUsername(
                            username = event.username,
                            alternateUsername = initiateAuthResponse?.challengeParameters?.get(KEY_USERNAME),
                            userIDForSRP = initiateAuthResponse?.challengeParameters?.get(
                                KEY_USERID_FOR_SRP
                            )
                        )
                    )

                    parseResponseChallenge(
                        challengeNameType = initiateAuthResponse?.challengeName,
                        challengeParams = initiateAuthResponse?.challengeParameters,
                        session = initiateAuthResponse?.session,
                        updatedDeviceMetadata = updatedDeviceMetadata,
                        metadata = event.metadata
                    )
                }
            } catch (e: Exception) {
                val errorEvent = SRPEvent(SRPEvent.EventType.ThrowAuthError(e))
                logger.verbose("$id Sending event ${errorEvent.type}")
                dispatcher.send(errorEvent)

                AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    private fun parseResponseChallenge(
        challengeNameType: ChallengeNameType?,
        challengeParams: Map<String, String>?,
        session: String?,
        updatedDeviceMetadata: DeviceMetadata.Metadata?,
        metadata: Map<String, String>
    ): SRPEvent =
        when (challengeNameType) {
            ChallengeNameType.PasswordVerifier -> {
                challengeParams?.let { params ->
                    val updatedChallengeParams = updatedDeviceMetadata?.deviceKey?.let {
                        params.plus(KEY_DEVICE_KEY to it)
                    } ?: params

                    SRPEvent(
                        SRPEvent.EventType.RespondPasswordVerifier(
                            updatedChallengeParams,
                            metadata,
                            session
                        )
                    )
                } ?: throw ServiceException(
                    "Auth challenge parameters are empty.",
                    AmplifyException.TODO_RECOVERY_SUGGESTION
                )
            }
            else -> throw UnknownException(cause = Exception("Challenge type not supported for this flow."))
        }

    override fun initiateSRPWithCustomAuthAction(event: SRPEvent.EventType.InitiateSRPWithCustom): Action =
        Action<AuthEnvironment>("InitSRPCustomAuth") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                srpHelper = SRPHelper(event.password)

                val secretHash = AuthHelper.getSecretHash(
                    event.username,
                    configuration.userPool?.appClient,
                    configuration.userPool?.appClientSecret
                )

                val authParams = mutableMapOf(
                    KEY_USERNAME to event.username,
                    KEY_SRP_A to srpHelper.getPublicA(),
                    KEY_CHALLENGE_NAME to VALUE_SRP_A
                )
                secretHash?.let { authParams[KEY_SECRET_HASH] = it }

                val encodedContextData = getUserContextData(event.username)
                val deviceMetadata = getDeviceMetadata(event.username)
                deviceMetadata?.let { authParams[KEY_DEVICE_KEY] = it.deviceKey }
                val pinpointEndpointId = getPinpointEndpointId()

                val initiateAuthResponse = cognitoAuthService.cognitoIdentityProviderClient?.initiateAuth {
                    authFlow = AuthFlowType.CUSTOM_AUTH.toCognitoType()
                    clientId = configuration.userPool?.appClient
                    authParameters = authParams
                    clientMetadata = event.metadata
                    pinpointEndpointId?.let { analyticsMetadata { analyticsEndpointId = it } }
                    encodedContextData?.let { userContextData { encodedData = it } }
                }

                when (initiateAuthResponse?.challengeName) {
                    ChallengeNameType.PasswordVerifier ->
                        initiateAuthResponse.challengeParameters?.let { params ->
                            val challengeParams = deviceMetadata?.deviceKey?.let {
                                params.plus(KEY_DEVICE_KEY to it)
                            } ?: params
                            SRPEvent(
                                SRPEvent.EventType.RespondPasswordVerifier(
                                    challengeParams,
                                    event.metadata,
                                    initiateAuthResponse.session
                                )
                            )
                        } ?: throw ServiceException(
                            "Auth challenge parameters are empty.",
                            AmplifyException.TODO_RECOVERY_SUGGESTION
                        )
                    else -> throw Exception("Not yet implemented.")
                }
            } catch (e: Exception) {
                val errorEvent = SRPEvent(SRPEvent.EventType.ThrowAuthError(e))
                logger.verbose("$id Sending event ${errorEvent.type}")
                dispatcher.send(errorEvent)
                val errorEvent2 = SignInEvent(SignInEvent.EventType.ThrowError(e))
                logger.verbose("$id Sending event ${errorEvent.type}")
                dispatcher.send(errorEvent2)
                AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun verifyPasswordSRPAction(
        challengeParameters: Map<String, String>,
        metadata: Map<String, String>,
        session: String?
    ) =
        Action<AuthEnvironment>("VerifyPasswordSRP") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val salt = challengeParameters.getValue(KEY_SALT)
                val secretBlock = challengeParameters.getValue(KEY_SECRET_BLOCK)
                val srpB = challengeParameters.getValue(KEY_SRP_B)
                val username = challengeParameters.getValue(KEY_USERNAME)
                val userId = challengeParameters.getValue(KEY_USER_ID_FOR_SRP)
                val deviceKey = challengeParameters.getOrDefault(KEY_DEVICE_KEY, "")

                srpHelper.setUserPoolParams(userId, configuration.userPool?.poolId!!)

                val secretHash = AuthHelper.getSecretHash(
                    username,
                    configuration.userPool.appClient,
                    configuration.userPool.appClientSecret
                )

                val challengeParams = mutableMapOf(
                    KEY_USERNAME to username,
                    KEY_PASSWORD_CLAIM_SECRET_BLOCK to secretBlock,
                    KEY_PASSWORD_CLAIM_SIGNATURE to srpHelper.getSignature(salt, srpB, secretBlock),
                    KEY_TIMESTAMP to srpHelper.dateString
                )
                secretHash?.let { challengeParams[KEY_SECRET_HASH] = it }
                challengeParams[KEY_DEVICE_KEY] = deviceKey

                val encodedContextData = getUserContextData(username)
                val pinpointEndpointId = getPinpointEndpointId()

                val response = cognitoAuthService.cognitoIdentityProviderClient?.respondToAuthChallenge {
                    challengeName = ChallengeNameType.PasswordVerifier
                    clientId = configuration.userPool.appClient
                    challengeResponses = challengeParams
                    clientMetadata = metadata
                    this.session = session
                    pinpointEndpointId?.let { analyticsMetadata { analyticsEndpointId = it } }
                    encodedContextData?.let { userContextData { encodedData = it } }
                }
                if (response != null) {
                    SignInChallengeHelper.evaluateNextStep(
                        username = username,
                        challengeNameType = response.challengeName,
                        session = response.session,
                        challengeParameters = response.challengeParameters,
                        authenticationResult = response.authenticationResult,
                        signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH)
                    )
                } else {
                    throw ServiceException(
                        "Sign in failed",
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                    )
                }
            } catch (e: Exception) {
                if (e is ResourceNotFoundException) {
                    val challengeParams: MutableMap<String, String> = challengeParameters.toMutableMap()
                    challengeParams.remove(KEY_DEVICE_KEY)
                    credentialStoreClient.clearCredentials(
                        CredentialType.Device(
                            challengeParams.getValue(
                                KEY_USERNAME
                            )
                        )
                    )
                    SRPEvent(SRPEvent.EventType.RetryRespondPasswordVerifier(challengeParams, metadata, session))
                } else {
                    val errorEvent = SRPEvent(SRPEvent.EventType.ThrowPasswordVerifierError(e))
                    logger.verbose("$id Sending event ${errorEvent.type}")
                    dispatcher.send(errorEvent)

                    val errorEvent2 = SignInEvent(SignInEvent.EventType.ThrowError(e))
                    logger.verbose("$id Sending event ${errorEvent.type}")
                    dispatcher.send(errorEvent2)

                    AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())
                }
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
