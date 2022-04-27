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
        Action<AuthEnvironment>("StartSignUp") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = try {
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
                    this.clientId = configuration.userPool?.appClient
                    this.secretHash = SRPHelper.getSecretHash(
                        event.username,
                        configuration.userPool?.appClient,
                        configuration.userPool?.appClientSecret
                    )
                }

                val response = cognitoAuthService.cognitoIdentityProviderClient?.signUp(options)
                val deliveryDetails = response?.codeDeliveryDetails?.let { details ->
                    mapOf(
                        "DESTINATION" to details.destination,
                        "MEDIUM" to details.deliveryMedium?.value,
                        "ATTRIBUTE" to details.attributeName
                    )
                }

                SignUpEvent(
                    SignUpEvent.EventType.InitiateSignUpSuccess(
                        SignedUpData(response?.userSub, event.username, deliveryDetails)
                    )
                )
            } catch (e: Exception) {
                SignUpEvent(SignUpEvent.EventType.InitiateSignUpFailure(e))
            }
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun confirmSignUpAction(event: SignUpEvent.EventType.ConfirmSignUp) =
        Action<AuthEnvironment>("ConfirmSignUp") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = try {
                val options = ConfirmSignUpRequest {
                    this.username = event.username
                    this.confirmationCode = event.confirmationCode
                    this.clientId = configuration.userPool?.appClient
                    this.secretHash = SRPHelper.getSecretHash(
                        event.username,
                        configuration.userPool?.appClient,
                        configuration.userPool?.appClientSecret
                    )
                }

                cognitoAuthService.cognitoIdentityProviderClient?.confirmSignUp(options)
                SignUpEvent(SignUpEvent.EventType.ConfirmSignUpSuccess())
            } catch (e: Exception) {
                SignUpEvent(SignUpEvent.EventType.ConfirmSignUpFailure(e))
            }
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun resendConfirmationCodeAction(event: SignUpEvent.EventType.ResendSignUpCode) =
        Action<AuthEnvironment>("ResendConfirmationCode") { id, dispatcher ->
            logger?.verbose("$id Starting execution")
            val evt = try {
                val options = ResendConfirmationCodeRequest {
                    clientId = configuration.userPool?.appClient
                    username = event.username
                }

                val response = cognitoAuthService.cognitoIdentityProviderClient?.resendConfirmationCode(options)
                val deliveryDetails = response?.codeDeliveryDetails?.let { details ->
                    mapOf(
                        "DESTINATION" to details.destination,
                        "MEDIUM" to details.deliveryMedium?.value,
                        "ATTRIBUTE" to details.attributeName
                    )
                }

                SignUpEvent(
                    SignUpEvent.EventType.ResendSignUpCodeSuccess(SignedUpData("", event.username, deliveryDetails))
                )
            } catch (e: Exception) {
                SignUpEvent(SignUpEvent.EventType.ResendSignUpCodeFailure(e))
            }
            logger?.verbose("$id Sending event ${evt.type}")
            dispatcher.send(evt)
        }

    override fun resetSignUpAction() = Action<AuthEnvironment>("ResetSignUp") { id, dispatcher ->
        logger?.verbose("$id Starting execution")
        val evt = AuthenticationEvent(AuthenticationEvent.EventType.ResetSignUp())
        logger?.verbose("$id Sending event ${evt.type}")
        dispatcher.send(evt)
    }
}
