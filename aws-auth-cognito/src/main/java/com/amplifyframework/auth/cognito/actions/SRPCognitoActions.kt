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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthFlowType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.AuthConstants.KEY_PASSWORD_CLAIM_SECRET_BLOCK
import com.amplifyframework.auth.cognito.AuthConstants.KEY_PASSWORD_CLAIM_SIGNATURE
import com.amplifyframework.auth.cognito.AuthConstants.KEY_SALT
import com.amplifyframework.auth.cognito.AuthConstants.KEY_SECRET_BLOCK
import com.amplifyframework.auth.cognito.AuthConstants.KEY_SECRET_HASH
import com.amplifyframework.auth.cognito.AuthConstants.KEY_SRP_A
import com.amplifyframework.auth.cognito.AuthConstants.KEY_SRP_B
import com.amplifyframework.auth.cognito.AuthConstants.KEY_TIMESTAMP
import com.amplifyframework.auth.cognito.AuthConstants.KEY_USERNAME
import com.amplifyframework.auth.cognito.AuthConstants.KEY_USER_ID_FOR_SRP
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SRPActions
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.SRPEvent

object SRPCognitoActions : SRPActions {
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
                val initiateAuthResponse = cognitoAuthService.cognitoIdentityProviderClient?.initiateAuth {
                    authFlow = AuthFlowType.UserSrpAuth
                    clientId = configuration.userPool?.appClient
                    authParameters = authParams
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
                val m1Signature = srpHelper.getSignature(salt, srpB, secretBlock)
                val secretHash = AuthHelper.getSecretHash(
                    username,
                    configuration.userPool.appClient,
                    configuration.userPool.appClientSecret
                )

                val challengeParams = mutableMapOf(
                    KEY_USERNAME to username,
                    KEY_PASSWORD_CLAIM_SECRET_BLOCK to secretBlock,
                    KEY_PASSWORD_CLAIM_SIGNATURE to m1Signature,
                    KEY_TIMESTAMP to srpHelper.dateString,
                )
                secretHash?.let { challengeParams[KEY_SECRET_HASH] = it }
                val response = cognitoAuthService.cognitoIdentityProviderClient?.respondToAuthChallenge {
                    challengeName = ChallengeNameType.PasswordVerifier
                    clientId = configuration.userPool.appClient
                    challengeResponses = challengeParams
                }
                if (response != null) {
                    SignInChallengeHelper.evaluateNextStep(
                        userId,
                        username,
                        response.challengeName,
                        response.session,
                        response.challengeParameters,
                        response.authenticationResult
                    )
                } else {
                    throw AuthException("Sign in failed", AuthException.TODO_RECOVERY_SUGGESTION)
                }
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
