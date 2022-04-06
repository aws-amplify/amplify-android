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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.AttributeType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmSignUpRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ResendConfirmationCodeRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpRequest
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.helpers.SRPHelper
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SignUpActions
import com.amplifyframework.statemachine.codegen.data.SignedUpData
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.SignUpEvent

object SignUpCognitoActions : SignUpActions {
    override fun startSignUpAction(event: SignUpEvent.EventType.InitiateSignUp) =
        Action { dispatcher, environment ->
            val env = (environment as AuthEnvironment)
            runCatching {
                val userAttributes = event.options.userAttributes.map {
                    AttributeType {
                        name = it.key.keyString
                        value = it.value
                    }
                }
                val options = SignUpRequest {
                    this.username = event.username
                    this.password = event.password
                    this.userAttributes = userAttributes
                    this.clientId = env.configuration.userPool?.appClient
                    this.secretHash = SRPHelper.getSecretHash(
                        event.username,
                        env.configuration.userPool?.appClient,
                        env.configuration.userPool?.appClientSecret
                    )
                }

                env.cognitoAuthService.cognitoIdentityProviderClient?.signUp(options)
            }.onSuccess {
                val deliveryDetails = it?.codeDeliveryDetails?.let { details ->
                    mapOf(
                        "DESTINATION" to details.destination,
                        "MEDIUM" to details.deliveryMedium?.value,
                        "ATTRIBUTE" to details.attributeName
                    )
                }

                dispatcher.send(
                    SignUpEvent(
                        SignUpEvent.EventType.InitiateSignUpSuccess(
                            SignedUpData(it?.userSub, event.username, deliveryDetails)
                        )
                    )
                )
            }.onFailure {
                dispatcher.send(SignUpEvent(SignUpEvent.EventType.InitiateSignUpFailure(it as Exception)))
            }
        }

    override fun confirmSignUpAction(event: SignUpEvent.EventType.ConfirmSignUp) =
        Action { dispatcher, environment ->
            val env = (environment as AuthEnvironment)
            runCatching {
                val options = ConfirmSignUpRequest {
                    this.username = event.username
                    this.confirmationCode = event.confirmationCode
                    this.clientId = env.configuration.userPool?.appClient
                    this.secretHash = SRPHelper.getSecretHash(
                        event.username,
                        env.configuration.userPool?.appClient,
                        env.configuration.userPool?.appClientSecret
                    )
                }
                env.cognitoAuthService.cognitoIdentityProviderClient?.confirmSignUp(options)
            }.onSuccess {
                dispatcher.send(SignUpEvent(SignUpEvent.EventType.ConfirmSignUpSuccess()))
            }.onFailure {
                dispatcher.send(SignUpEvent(SignUpEvent.EventType.ConfirmSignUpFailure(it as Exception)))
            }
        }

    override fun resendConfirmationCodeAction(event: SignUpEvent.EventType.ResendSignUpCode) =
        Action { dispatcher, environment ->
            val env = (environment as AuthEnvironment)
            runCatching {
                val options = ResendConfirmationCodeRequest {
                    clientId = env.configuration.userPool?.appClient
                    username = event.username
                }
                env.cognitoAuthService.cognitoIdentityProviderClient?.resendConfirmationCode(options)
            }.onSuccess {
                val deliveryDetails = it?.codeDeliveryDetails?.let { details ->
                    mapOf(
                        "DESTINATION" to details.destination,
                        "MEDIUM" to details.deliveryMedium?.value,
                        "ATTRIBUTE" to details.attributeName
                    )
                } ?: mapOf()

                SignUpEvent(
                    SignUpEvent.EventType.ResendSignUpCodeSuccess(
                        SignedUpData("", event.username, deliveryDetails)
                    )
                )
            }.onFailure {
                dispatcher.send(SignUpEvent(SignUpEvent.EventType.ResendSignUpCodeFailure(it as Exception)))
            }
        }

    override fun resetSignUpAction() = Action { dispatcher, environment ->
        dispatcher.send(AuthenticationEvent(AuthenticationEvent.EventType.ResetSignUp()))
    }
}
