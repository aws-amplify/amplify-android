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
import aws.sdk.kotlin.services.cognitoidentityprovider.respondToAuthChallenge
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SRPActions
import com.amplifyframework.statemachine.codegen.data.DeviceMetaData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.SRPEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent

object SRPCognitoActions : SRPActions {
    private const val KEY_PASSWORD_CLAIM_SECRET_BLOCK = "PASSWORD_CLAIM_SECRET_BLOCK"
    private const val KEY_PASSWORD_CLAIM_SIGNATURE = "PASSWORD_CLAIM_SIGNATURE"
    private const val KEY_TIMESTAMP = "TIMESTAMP"
    private const val KEY_SALT = "SALT"
    private const val KEY_SECRET_BLOCK = "SECRET_BLOCK"
    private const val KEY_SRP_A = "SRP_A"
    private const val KEY_SRP_B = "SRP_B"
    private const val KEY_USER_ID_FOR_SRP = "USER_ID_FOR_SRP"
    private const val KEY_SECRET_HASH = "SECRET_HASH"
    private const val KEY_USERNAME = "USERNAME"

    private const val KEY_ID_TOKEN = "ID_TOKEN"
    private const val KEY_ACCESS_TOKEN = "ID_TOKEN"
    private const val KEY_REFRESH_TOKEN = "REFRESH_TOKEN"
    private const val KEY_DEVICE_GROUP_KEY = "DEVICE_GROUP_KEY"
    private const val KEY_DEVICE_KEY = "DEVICE_KEY"

    override fun initiateSRPAuthAction(event: SRPEvent.EventType.InitiateSRP) =
        Action<AuthEnvironment>("InitSRPAuth") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = try {
                srpHelper = SRPHelper(event.password)

                val secretHash = AuthHelper.getSecretHash(
                    event.username,
                    configuration.userPool?.appClient,
                    configuration.userPool?.appClientSecret
                )

                val authParams = mutableMapOf(KEY_USERNAME to event.username, KEY_SRP_A to srpHelper.getPublicA())
                secretHash?.let { authParams[KEY_SECRET_HASH] = it }
                val encodedContextData = userContextDataProvider?.getEncodedContextData(event.username)

                val initiateAuthResponse = cognitoAuthService.cognitoIdentityProviderClient?.initiateAuth {
                    authFlow = AuthFlowType.UserSrpAuth
                    clientId = configuration.userPool?.appClient
                    authParameters = authParams
                    encodedContextData?.let { userContextData { encodedData = it } }
                }

                when (initiateAuthResponse?.challengeName) {
                    ChallengeNameType.PasswordVerifier -> initiateAuthResponse.challengeParameters?.let {
                        SRPEvent(SRPEvent.EventType.RespondPasswordVerifier(it))
                    } ?: throw Exception("Auth challenge parameters are empty.")
                    else -> throw Exception("Not yet implemented.")
                }
            } catch (e: Exception) {
                val errorEvent = SRPEvent(SRPEvent.EventType.ThrowAuthError(e))
                logger?.verbose("$id Sending event ${errorEvent.type}")
                dispatcher.send(errorEvent)

                AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())
            }
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun verifyPasswordSRPAction(event: SRPEvent.EventType.RespondPasswordVerifier) =
        Action<AuthEnvironment>("VerifyPasswordSRP") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = try {
                val params = event.challengeParameters
                val salt = params.getValue(KEY_SALT)
                val secretBlock = params.getValue(KEY_SECRET_BLOCK)
                val srpB = params.getValue(KEY_SRP_B)
                val username = params.getValue(KEY_USERNAME)
                val userId = params.getValue(KEY_USER_ID_FOR_SRP)

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
                    KEY_TIMESTAMP to srpHelper.dateString,
                )
                secretHash?.let { challengeParams[KEY_SECRET_HASH] = it }
                val encodedContextData = userContextDataProvider?.getEncodedContextData(username)

                val response = cognitoAuthService.cognitoIdentityProviderClient?.respondToAuthChallenge {
                    challengeName = ChallengeNameType.PasswordVerifier
                    clientId = configuration.userPool.appClient
                    challengeResponses = challengeParams
                    encodedContextData?.let { userContextData { encodedData = it } }
                }
                response?.let { respondToAuthChallengeResponse ->
                    respondToAuthChallengeResponse.authenticationResult?.newDeviceMetadata?.let { deviceMetaData ->
                        val confirmDeviceParams = DeviceMetaData(
                            idToken = respondToAuthChallengeResponse.authenticationResult?.idToken
                                ?: throw AuthException("Sign in failed", AuthException.TODO_RECOVERY_SUGGESTION),
                            refreshToken = respondToAuthChallengeResponse.authenticationResult?.refreshToken
                                ?: throw AuthException("Sign in failed", AuthException.TODO_RECOVERY_SUGGESTION),
                            accessToken = respondToAuthChallengeResponse.authenticationResult?.accessToken
                                ?: throw AuthException("Sign in failed", AuthException.TODO_RECOVERY_SUGGESTION),
                            deviceKey = deviceMetaData.deviceKey ?: throw AuthException(
                                "Sign in failed",
                                AuthException.TODO_RECOVERY_SUGGESTION
                            ),
                            userId = userId,
                            username = username,
                            deviceGroupKey = deviceMetaData.deviceGroupKey ?: throw AuthException(
                                "Sign in failed",
                                AuthException.TODO_RECOVERY_SUGGESTION
                            ),
                            expiresIn = respondToAuthChallengeResponse.authenticationResult?.expiresIn
                                ?: throw AuthException("Sign in failed", AuthException.TODO_RECOVERY_SUGGESTION)
                        )
                        SignInEvent(SignInEvent.EventType.ConfirmDevice(confirmDeviceParams))
                    } ?: SignInChallengeHelper.evaluateNextStep(
                        userId,
                        username,
                        respondToAuthChallengeResponse.challengeName,
                        respondToAuthChallengeResponse.session,
                        respondToAuthChallengeResponse.challengeParameters,
                        respondToAuthChallengeResponse.authenticationResult
                    )
                } ?: throw AuthException("Sign in failed", AuthException.TODO_RECOVERY_SUGGESTION)
            } catch (e: Exception) {
                val errorEvent = SRPEvent(SRPEvent.EventType.ThrowPasswordVerifierError(e))
                logger?.verbose("$id Sending event ${errorEvent.type}")
                dispatcher.send(errorEvent)

                AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())
            }
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
