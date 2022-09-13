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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmDeviceRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeviceSecretVerifierConfigType
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SignInActions
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.CustomSignInEvent
import com.amplifyframework.statemachine.codegen.events.SRPEvent
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent
import java.util.Date
import kotlin.time.Duration.Companion.seconds

object SignInCognitoActions : SignInActions {

    private const val KEY_SALT = "SALT"
    private const val KEY_USER_ID_FOR_SRP = "USER_ID_FOR_SRP"
    private const val KEY_USERNAME = "USERNAME"

    private const val KEY_ID_TOKEN = "ID_TOKEN"
    private const val KEY_ACCESS_TOKEN = "ID_TOKEN"
    private const val KEY_REFRESH_TOKEN = "REFRESH_TOKEN"
    private const val KEY_DEVICE_GROUP_KEY = "DEVICE_GROUP_KEY"
    private const val KEY_DEVICE_KEY = "DEVICE_KEY"

    override fun startSRPAuthAction(event: SignInEvent.EventType.InitiateSignInWithSRP) =
        Action<AuthEnvironment>("StartSRPAuth") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = SRPEvent(SRPEvent.EventType.InitiateSRP(event.username, event.password))
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun startCustomAuthAction(event: SignInEvent.EventType.InitiateSignInWithCustom) =
        Action<AuthEnvironment>("StartCustomAuth") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = CustomSignInEvent(
                CustomSignInEvent.EventType.InitiateCustomSignIn(event.username, event.password)
            )
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun initResolveChallenge(event: SignInEvent.EventType.ReceivedChallenge) =
        Action<AuthEnvironment>("InitResolveChallenge") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = SignInChallengeEvent(SignInChallengeEvent.EventType.WaitForAnswer(event.challenge))
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun confirmDevice(event: SignInEvent.EventType.ConfirmDevice): Action =
        Action<AuthEnvironment>("InitResolveChallenge") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val _idToken = event.confirmDeviceParams[KEY_ID_TOKEN]
            val _refreshToken = event.confirmDeviceParams[KEY_REFRESH_TOKEN]
            val _accessToken = event.confirmDeviceParams[KEY_ACCESS_TOKEN]
            val _deviceKey = event.confirmDeviceParams[KEY_DEVICE_KEY]
            val _deviceGroupKey = event.confirmDeviceParams[KEY_DEVICE_GROUP_KEY]
            val userId = event.confirmDeviceParams[KEY_USER_ID_FOR_SRP] ?: ""
            val username = event.confirmDeviceParams[KEY_USERNAME] as String
            val _salt = event.confirmDeviceParams[KEY_SALT]
            try {
                cognitoAuthService.cognitoIdentityProviderClient?.confirmDevice(
                    ConfirmDeviceRequest.invoke {
                        accessToken = _accessToken
                        deviceKey = _deviceKey
                        deviceSecretVerifierConfig = DeviceSecretVerifierConfigType.invoke {
                            if (_deviceGroupKey == null || _salt == null) {
                                throw Exception("Device Information is empty. Please try again")
                            }
                            passwordVerifier = srpHelper.computePasswordVerifier(
                                username,
                                _deviceGroupKey,
                                _salt
                            ).toString()
                            salt = _salt
                        }
                    }
                )
                val evt = SignInEvent(SignInEvent.EventType.FinalizeSignIn())
                val expiresIn = Instant.now().plus(event.expiresIn.seconds).epochSeconds
                val tokens = CognitoUserPoolTokens(_idToken, _accessToken, _refreshToken, expiresIn)
                val signedInData = SignedInData(userId, username, Date(), SignInMethod.SRP, tokens)

                AuthenticationEvent(AuthenticationEvent.EventType.SignInCompleted(signedInData))
                logger?.verbose("$id Sending event ${evt.type}")
                dispatcher.send(evt)
            } catch (e: Exception) {
                val errorEvent = SignInEvent(SignInEvent.EventType.ThrowError(e))
                logger?.verbose("$id Sending event ${errorEvent.type}")
                dispatcher.send(errorEvent)
                AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())
            }
        }
}
