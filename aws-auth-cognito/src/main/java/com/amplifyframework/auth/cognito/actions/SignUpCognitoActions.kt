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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.*
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.data.SignedUpData
import com.amplifyframework.auth.cognito.events.AuthenticationEvent
import com.amplifyframework.auth.cognito.events.SignUpEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.SignUpActions

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
                }

                env.cognitoAuthService.cognitoIdentityProviderClient?.signUp(options)
            }.onSuccess {
                val deliveryDetails = it?.codeDeliveryDetails?.let { details ->
                    mapOf(
                        "DESTINATION" to details.destination,
                        "MEDIUM" to details.deliveryMedium?.value,
                        "ATTRIBUTE" to details.attributeName
                    )
                } ?: mapOf()

                SignUpEvent(
                    SignUpEvent.EventType.InitiateSignUpSuccess(
                        SignedUpData(it?.userSub, event.username, deliveryDetails)
                    )
                )
            }.onFailure {
                dispatcher.send(
                    SignUpEvent(SignUpEvent.EventType.InitiateSignUpFailure(it as Exception))
                )
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
                }
                env.cognitoAuthService.cognitoIdentityProviderClient?.confirmSignUp(options)
            }.onSuccess {
                dispatcher.send(
                    SignUpEvent(SignUpEvent.EventType.ConfirmSignUpSuccess())
                )
            }.onFailure {
                dispatcher.send(
                    SignUpEvent(SignUpEvent.EventType.ConfirmSignUpFailure(it as Exception))
                )
            }
        }

    override fun resendConfirmationCodeAction() = Action { dispatcher, environment ->
        val env = (environment as AuthEnvironment)
        runCatching {
            val options = ResendConfirmationCodeRequest {
                clientId = env.configuration.userPool?.appClient
//                username = event.username
            }
            env.cognitoAuthService.cognitoIdentityProviderClient?.resendConfirmationCode(options)
        }
    }

    override fun resetSignUpAction() = Action { dispatcher, environment ->
        dispatcher.send(AuthenticationEvent(AuthenticationEvent.EventType.resetSignUp()))
    }
}