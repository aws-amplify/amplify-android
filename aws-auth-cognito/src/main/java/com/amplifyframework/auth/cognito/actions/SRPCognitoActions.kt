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

import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.SRPHelper
import com.amplifyframework.auth.cognito.events.SRPEvent
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthFlowType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import com.amplifyframework.auth.cognito.data.CognitoUserPoolTokens
import com.amplifyframework.auth.cognito.data.SignInMethod
import com.amplifyframework.auth.cognito.data.SignedInData
import com.amplifyframework.auth.cognito.events.AuthenticationEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SRPActions
import java.util.*

object SRPCognitoActions : SRPActions {
    override fun initiateSRPAuthAction(event: SRPEvent.EventType.InitiateSRP) =
        Action { dispatcher, environment ->
            with(environment as AuthEnvironment) {
                srpHelper = SRPHelper(event.username, event.password)

                try {
                    val initiateAuthResponse =
                        cognitoAuthService.cognitoIdentityProviderClient?.initiateAuth {
                            authFlow = AuthFlowType.UserSrpAuth
                            clientId = configuration.userPool?.appClient
                            authParameters = mapOf(
                                "USERNAME" to event.username,
                                "SRP_A" to srpHelper.getPublicA()
                            )
                        }

                    if (initiateAuthResponse?.challengeName == ChallengeNameType.PasswordVerifier) {
                        initiateAuthResponse.challengeParameters?.let {
                            dispatcher.send(
                                SRPEvent(SRPEvent.EventType.RespondPasswordVerifier(it))
                            )
                        } ?: throw Exception("Auth challenge parameters are empty.")
                    }
                } catch (exception: Exception) {
                    dispatcher.send(SRPEvent(SRPEvent.EventType.ThrowAuthError(exception)))
                    dispatcher.send(
                        AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())
                    )
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

                    cognitoAuthService.cognitoIdentityProviderClient?.respondToAuthChallenge {
                        challengeName = ChallengeNameType.PasswordVerifier
                        clientId = configuration.userPool?.appClient
                        challengeResponses = mapOf(
                            "USERNAME" to username,
                            "PASSWORD_CLAIM_SECRET_BLOCK" to secretBlock,
                            "PASSWORD_CLAIM_SIGNATURE" to m1Signature,
                            "TIMESTAMP" to srpHelper.dateString
                        )
                    }
                }.onSuccess {
                    it?.authenticationResult?.run {
                        val signedInData = SignedInData(
                            userId, username, Date(), SignInMethod.SRP, CognitoUserPoolTokens(
                                idToken, accessToken, refreshToken, expiresIn
                            )
                        )
                        dispatcher.send(SRPEvent(SRPEvent.EventType.FinalizeSRPSignIn()))
                        dispatcher.send(
                            AuthenticationEvent(
                                AuthenticationEvent.EventType.InitializedSignedIn(signedInData)
                            )
                        )
                    }
                }.onFailure {
                    dispatcher.send(
                        SRPEvent(
                            SRPEvent.EventType.ThrowPasswordVerifierError(it as Exception)
                        )
                    )
                    dispatcher.send(
                        AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())
                    )
                }
            }
        }
}