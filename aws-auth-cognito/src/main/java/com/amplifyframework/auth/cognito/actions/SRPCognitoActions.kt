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
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SRPActions
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.SRPEvent
import java.util.Date
import kotlin.time.Duration.Companion.seconds

object SRPCognitoActions : SRPActions {
    override fun initiateSRPAuthAction(event: SRPEvent.EventType.InitiateSRP) =
        Action { dispatcher, environment ->
            with(environment as AuthEnvironment) {
                srpHelper = SRPHelper(event.username, event.password)

                try {
                    val secretHash = SRPHelper.getSecretHash(
                        event.username,
                        configuration.userPool?.appClient,
                        configuration.userPool?.appClientSecret
                    )

                    var authParams = mapOf("USERNAME" to event.username, "SRP_A" to srpHelper.getPublicA())
                    secretHash?.also { authParams = authParams.plus("SECRET_HASH" to secretHash) }
                    val initiateAuthResponse = cognitoAuthService.cognitoIdentityProviderClient?.initiateAuth {
                        authFlow = AuthFlowType.UserSrpAuth
                        clientId = configuration.userPool?.appClient
                        authParameters = authParams
                    }

                    if (initiateAuthResponse?.challengeName == ChallengeNameType.PasswordVerifier) {
                        initiateAuthResponse.challengeParameters?.let {
                            dispatcher.send(SRPEvent(SRPEvent.EventType.RespondPasswordVerifier(it)))
                        } ?: throw Exception("Auth challenge parameters are empty.")
                    }
                } catch (exception: Exception) {
                    dispatcher.send(SRPEvent(SRPEvent.EventType.ThrowAuthError(exception)))
                    dispatcher.send(AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn()))
                }
            }
        }

    override fun verifyPasswordSRPAction(event: SRPEvent.EventType.RespondPasswordVerifier) =
        Action { dispatcher, environment ->
            with(environment as AuthEnvironment) {
                val params = event.challengeParameters
                val salt = params.getValue("SALT")
                val secretBlock = params.getValue("SECRET_BLOCK")
                val srpB = params.getValue("SRP_B")
                val username = params.getValue("USERNAME")
                val userId = params.getValue("USER_ID_FOR_SRP")

                runCatching {
                    srpHelper.setUserPoolParams(userId, configuration.userPool?.poolId!!)
                    val m1Signature = srpHelper.getSignature(salt, srpB, secretBlock)
                    val secretHash = SRPHelper.getSecretHash(
                        username,
                        configuration.userPool?.appClient,
                        configuration.userPool?.appClientSecret
                    )
                    var challengeParams = mapOf(
                        "USERNAME" to username,
                        "PASSWORD_CLAIM_SECRET_BLOCK" to secretBlock,
                        "PASSWORD_CLAIM_SIGNATURE" to m1Signature,
                        "TIMESTAMP" to srpHelper.dateString,
                    )
                    secretHash?.also { challengeParams = challengeParams.plus("SECRET_HASH" to secretHash) }
                    cognitoAuthService.cognitoIdentityProviderClient?.respondToAuthChallenge {
                        challengeName = ChallengeNameType.PasswordVerifier
                        clientId = configuration.userPool?.appClient
                        challengeResponses = challengeParams
                    }
                }.onSuccess {
                    it?.authenticationResult?.run {

                        val signedInData = SignedInData(
                            userId,
                            username,
                            Date(),
                            SignInMethod.SRP,
                            CognitoUserPoolTokens(
                                idToken,
                                accessToken,
                                refreshToken,
                                Instant.now().plus(expiresIn.seconds).epochSeconds,
                            )
                        )
                        dispatcher.send(SRPEvent(SRPEvent.EventType.FinalizeSRPSignIn()))
                        dispatcher.send(
                            AuthenticationEvent(AuthenticationEvent.EventType.InitializedSignedIn(signedInData))
                        )
                    }
                }.onFailure {
                    dispatcher.send(SRPEvent(SRPEvent.EventType.ThrowPasswordVerifierError(it as Exception)))
                    dispatcher.send(AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn()))
                }
            }
        }
}
