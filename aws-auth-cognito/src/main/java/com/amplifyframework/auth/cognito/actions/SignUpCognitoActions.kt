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

import aws.sdk.kotlin.services.cognitoidentityprovider.confirmSignUp
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AnalyticsMetadataType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AttributeType
import aws.sdk.kotlin.services.cognitoidentityprovider.signUp
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.step.AuthNextSignUpStep
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SignUpActions
import com.amplifyframework.statemachine.codegen.data.SignUpData
import com.amplifyframework.statemachine.codegen.events.SignUpEvent

internal object SignUpCognitoActions : SignUpActions {

    override fun initiateSignUpAction(event: SignUpEvent.EventType.InitiateSignUp): Action =
        Action<AuthEnvironment>("InitiatingSignUp") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val username = event.signUpData.username
                val encodedContextData = getUserContextData(username)
                val pinpointEndpointId = getPinpointEndpointId()

                val response = cognitoAuthService.cognitoIdentityProviderClient?.signUp {
                    this.username = username
                    this.password = event.password
                    this.userAttributes = event.userAttributes?.map {
                        AttributeType {
                            name = it.key.keyString
                            value = it.value
                        }
                    }
                    this.clientId = configuration.userPool?.appClient
                    this.secretHash = AuthHelper.getSecretHash(
                        username,
                        configuration.userPool?.appClient,
                        configuration.userPool?.appClientSecret
                    )
                    pinpointEndpointId?.let {
                        this.analyticsMetadata = AnalyticsMetadataType.invoke { analyticsEndpointId = it }
                    }
                    encodedContextData?.let { this.userContextData { encodedData = it } }
                    this.clientMetadata = event.signUpData.clientMetadata
                    this.validationData = event.signUpData.validationData?.mapNotNull { option ->
                        AttributeType {
                            name = option.key
                            value = option.value
                        }
                    }
                }

                val codeDeliveryDetails = AuthCodeDeliveryDetails(
                    response?.codeDeliveryDetails?.destination ?: "",
                    AuthCodeDeliveryDetails.DeliveryMedium.fromString(
                        response?.codeDeliveryDetails?.deliveryMedium?.value
                    ),
                    response?.codeDeliveryDetails?.attributeName
                )
                val signUpData = SignUpData(
                    username,
                    event.signUpData.validationData,
                    event.signUpData.clientMetadata,
                    response?.session,
                    response?.userSub
                )
                if (response?.userConfirmed == true) {
                    var signUpStep = AuthSignUpStep.DONE
                    if (response.session != null) {
                        signUpStep = AuthSignUpStep.COMPLETE_AUTO_SIGN_IN
                    }
                    val signUpResult =
                        AuthSignUpResult(
                            true,
                            AuthNextSignUpStep(
                                signUpStep,
                                mapOf(),
                                codeDeliveryDetails
                            ),
                            response.userSub
                        )
                    SignUpEvent(SignUpEvent.EventType.SignedUp(signUpData, signUpResult))
                } else {
                    val signUpResult =
                        AuthSignUpResult(
                            false,
                            AuthNextSignUpStep(
                                AuthSignUpStep.CONFIRM_SIGN_UP_STEP,
                                mapOf(),
                                codeDeliveryDetails
                            ),
                            response?.userSub
                        )
                    SignUpEvent(SignUpEvent.EventType.InitiateSignUpComplete(signUpData, signUpResult))
                }
            } catch (e: Exception) {
                SignUpEvent(SignUpEvent.EventType.ThrowError(e))
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun confirmSignUpAction(event: SignUpEvent.EventType.ConfirmSignUp): Action =
        Action<AuthEnvironment>("ConfirmSignUp") { id, dispatcher ->
            logger.verbose("$id Starting execution")
            val evt = try {
                val username = event.signUpData.username
                val encodedContextData = getUserContextData(username)
                val pinpointEndpointId = getPinpointEndpointId()

                val response = cognitoAuthService.cognitoIdentityProviderClient?.confirmSignUp {
                    this.username = username
                    this.confirmationCode = event.confirmationCode
                    this.clientId = configuration.userPool?.appClient
                    this.secretHash = AuthHelper.getSecretHash(
                        username,
                        configuration.userPool?.appClient,
                        configuration.userPool?.appClientSecret
                    )
                    pinpointEndpointId?.let {
                        this.analyticsMetadata = AnalyticsMetadataType.invoke { analyticsEndpointId = it }
                    }
                    encodedContextData?.let { this.userContextData { encodedData = it } }
                    this.clientMetadata = event.signUpData.clientMetadata
                    this.session = event.signUpData.session
                }
                val signUpData = SignUpData(
                    username,
                    event.signUpData.validationData,
                    event.signUpData.clientMetadata,
                    response?.session,
                    event.signUpData.userId
                )
                var signUpStep = AuthSignUpStep.DONE
                if (response?.session != null) {
                    signUpStep = AuthSignUpStep.COMPLETE_AUTO_SIGN_IN
                }
                val signUpResult =
                    AuthSignUpResult(
                        true,
                        AuthNextSignUpStep(
                            signUpStep,
                            mapOf(),
                            null
                        ),
                        event.signUpData.userId
                    )
                SignUpEvent(SignUpEvent.EventType.SignedUp(signUpData, signUpResult))
            } catch (e: Exception) {
                SignUpEvent(SignUpEvent.EventType.ThrowError(e))
            }
            logger.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }
}
