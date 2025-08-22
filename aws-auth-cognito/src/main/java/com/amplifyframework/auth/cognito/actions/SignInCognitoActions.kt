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

import android.os.Build
import aws.sdk.kotlin.services.cognitoidentityprovider.initiateAuth
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthFlowType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmDeviceRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeviceSecretVerifierConfigType
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.cognito.helpers.CognitoDeviceHelper
import com.amplifyframework.auth.cognito.helpers.SignInChallengeHelper
import com.amplifyframework.auth.exceptions.ServiceException
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SignInActions
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.CustomSignInEvent
import com.amplifyframework.statemachine.codegen.events.DeviceSRPSignInEvent
import com.amplifyframework.statemachine.codegen.events.HostedUIEvent
import com.amplifyframework.statemachine.codegen.events.SRPEvent
import com.amplifyframework.statemachine.codegen.events.SetupTOTPEvent
import com.amplifyframework.statemachine.codegen.events.SignInChallengeEvent
import com.amplifyframework.statemachine.codegen.events.SignInEvent
import com.amplifyframework.statemachine.codegen.events.WebAuthnEvent

internal object SignInCognitoActions : SignInActions {
    private const val KEY_SECRET_HASH = "SECRET_HASH"
    private const val KEY_USERNAME = "USERNAME"

    override fun startSRPAuthAction(event: SignInEvent.EventType.InitiateSignInWithSRP) =
        Action<AuthEnvironment>("StartSRPAuth") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = SRPEvent(
                SRPEvent.EventType.InitiateSRP(
                    username = event.username,
                    password = event.password,
                    metadata = event.metadata,
                    authFlowType = event.authFlowType,
                    respondToAuthChallenge = event.respondToAuthChallenge
                )
            )
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun startCustomAuthAction(event: SignInEvent.EventType.InitiateSignInWithCustom) =
        Action<AuthEnvironment>("StartCustomAuth") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = CustomSignInEvent(
                CustomSignInEvent.EventType.InitiateCustomSignIn(event.username, event.metadata)
            )
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun startMigrationAuthAction(event: SignInEvent.EventType.InitiateMigrateAuth) =
        Action<AuthEnvironment>("StartMigrationAuth") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = SignInEvent(
                SignInEvent.EventType.InitiateMigrateAuth(
                    username = event.username,
                    password = event.password,
                    metadata = event.metadata,
                    authFlowType = event.authFlowType
                )
            )
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun startCustomAuthWithSRPAction(event: SignInEvent.EventType.InitiateCustomSignInWithSRP): Action =
        Action<AuthEnvironment>("StartCustomSRPAuth") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = SRPEvent(SRPEvent.EventType.InitiateSRPWithCustom(event.username, event.password, event.metadata))
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun startDeviceSRPAuthAction(event: SignInEvent.EventType.InitiateSignInWithDeviceSRP) =
        Action<AuthEnvironment>("StartDeviceSRPAuth") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = DeviceSRPSignInEvent(
                DeviceSRPSignInEvent.EventType.RespondDeviceSRPChallenge(event.username, event.metadata)
            )
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun initResolveChallenge(event: SignInEvent.EventType.ReceivedChallenge) =
        Action<AuthEnvironment>("InitResolveChallenge") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = SignInChallengeEvent(
                SignInChallengeEvent.EventType.WaitForAnswer(event.challenge, event.signInMethod, true)
            )
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun confirmDevice(event: SignInEvent.EventType.ConfirmDevice): Action =
        Action<AuthEnvironment>("ConfirmDevice") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val deviceMetadata = event.deviceMetadata
            val deviceKey = deviceMetadata.deviceKey
            val deviceGroupKey = deviceMetadata.deviceGroupKey
            val evt = try {
                val deviceVerifierMap = CognitoDeviceHelper.generateVerificationParameters(deviceKey, deviceGroupKey)

                cognitoAuthService.cognitoIdentityProviderClient?.confirmDevice(
                    ConfirmDeviceRequest.invoke {
                        this.accessToken = event.signedInData.cognitoUserPoolTokens.accessToken
                        this.deviceKey = deviceKey
                        this.deviceName = Build.MODEL
                        this.deviceSecretVerifierConfig = DeviceSecretVerifierConfigType.invoke {
                            this.passwordVerifier = deviceVerifierMap["verifier"]
                            this.salt = deviceVerifierMap["salt"]
                        }
                    }
                ) ?: throw ServiceException("Sign in failed", AmplifyException.TODO_RECOVERY_SUGGESTION)

                val updatedDeviceMetadata = deviceMetadata.copy(deviceSecret = deviceVerifierMap["secret"])
                credentialStoreClient.storeCredentials(
                    CredentialType.Device(event.signedInData.username),
                    AmplifyCredential.DeviceData(updatedDeviceMetadata)
                )

                AuthenticationEvent(
                    AuthenticationEvent.EventType.SignInCompleted(
                        event.signedInData,
                        DeviceMetadata.Metadata(deviceKey, deviceGroupKey)
                    )
                )
            } catch (e: Exception) {
                SignInEvent(SignInEvent.EventType.ThrowError(e))
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun startHostedUIAuthAction(event: SignInEvent.EventType.InitiateHostedUISignIn) =
        Action<AuthEnvironment>("StartHostedUIAuth") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = HostedUIEvent(HostedUIEvent.EventType.ShowHostedUI(event.hostedUISignInData))
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun initiateTOTPSetupAction(event: SignInEvent.EventType.InitiateTOTPSetup) =
        Action<AuthEnvironment>("initiateTOTPSetup") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = SetupTOTPEvent(
                SetupTOTPEvent.EventType.SetupTOTP(
                    totpSetupDetails = event.signInTOTPSetupData,
                    challengeParams = event.challengeParams,
                    signInMethod = event.signInMethod
                )
            )
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun initiateWebAuthnSignInAction(event: SignInEvent.EventType.InitiateWebAuthnSignIn) =
        Action<AuthEnvironment>("initiateWebAuthnSignIn") { id, dispatcher ->
            logger.verbose("$id Starting excution")
            val signInContext = event.signInContext
            val requestJson = signInContext.requestJson
            val evt = if (requestJson == null) {
                // If we don't already have the request JSON then fetch it
                WebAuthnEvent(WebAuthnEvent.EventType.FetchCredentialOptions(signInContext))
            } else {
                // We do have the request JSON so go directly to asserting it
                WebAuthnEvent(WebAuthnEvent.EventType.AssertCredentialOptions(signInContext))
            }
            logger.verbose("$id sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun autoSignInAction(event: SignInEvent.EventType.InitiateAutoSignIn): Action =
        Action<AuthEnvironment>("AutoSignIn") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val username = event.signInData.username
                val secretHash = AuthHelper.getSecretHash(
                    username,
                    configuration.userPool?.appClient,
                    configuration.userPool?.appClientSecret
                )

                val authParams = mutableMapOf(
                    KEY_USERNAME to username
                )
                secretHash?.let { authParams[KEY_SECRET_HASH] = it }

                val encodedContextData = getUserContextData(username)
                val pinpointEndpointId = getPinpointEndpointId()

                val response = cognitoAuthService.cognitoIdentityProviderClient?.initiateAuth {
                    authFlow = AuthFlowType.UserAuth
                    clientId = configuration.userPool?.appClient
                    authParameters = authParams
                    clientMetadata = event.signInData.metadata
                    pinpointEndpointId?.let { analyticsMetadata { analyticsEndpointId = it } }
                    encodedContextData?.let { userContextData { encodedData = it } }
                    session = event.signInData.session
                }

                if (response != null) {
                    SignInChallengeHelper.evaluateNextStep(
                        username = username,
                        challengeNameType = response.challengeName,
                        session = response.session,
                        challengeParameters = response.challengeParameters,
                        authenticationResult = response.authenticationResult,
                        signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_AUTH)
                    )
                } else {
                    throw ServiceException(
                        "Sign in failed",
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                    )
                }
            } catch (e: Exception) {
                val signInError = SignInEvent(SignInEvent.EventType.ThrowError(e))
                logger.verbose("$id Sending event ${signInError.type}")
                dispatcher.send(signInError)

                AuthenticationEvent(AuthenticationEvent.EventType.CancelSignIn())
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
