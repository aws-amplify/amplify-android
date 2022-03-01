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
import com.amplifyframework.auth.cognito.events.SignUpEvent
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmSignUpRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ResendConfirmationCodeRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpResponse
import com.amplifyframework.auth.cognito.data.AuthenticationError
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher
import com.amplifyframework.statemachine.codegen.actions.SignUpActions

object SignUpCognitoActions : SignUpActions {
    override fun startSignUpAction(event: SignUpEvent.EventType.InitiateSignUp) = object : Action {
        override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
            val env = (environment as AuthEnvironment)
            val options = SignUpRequest {
                this.username = event.username
                this.password = event.password
                this.clientId = env.configuration.userPool?.appClient
            }
            var signupResponse: SignUpResponse? = null
            try {
                signupResponse = env.cognitoIdentityProviderClient.signUp(options)
            } catch (e: Exception) {
                dispatcher.send(
                    SignUpEvent(
                        SignUpEvent.EventType.InitiateSignUpFailure(AuthenticationError("Signup error."))
                    )
                )
            }

            val signUpEvent = if (signupResponse?.codeDeliveryDetails != null) {
                SignUpEvent(
                    SignUpEvent.EventType.InitiateSignUpSuccess(event.username, signupResponse)
                )
            } else {
                SignUpEvent(
                    SignUpEvent.EventType.InitiateSignUpFailure(AuthenticationError("Signup error."))
                )
            }
            dispatcher.send(signUpEvent)
        }
    }

    override fun confirmSignUpAction(event: SignUpEvent.EventType.ConfirmSignUp) = object : Action {
        override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
            val env = (environment as AuthEnvironment)
            val options = ConfirmSignUpRequest {
                this.username = event.username
                this.confirmationCode = event.confirmationCode
                this.clientId = env.configuration.userPool?.appClient
            }
            val confirmSignUpResponse = env.cognitoIdentityProviderClient.confirmSignUp(options)

            dispatcher.send(
                SignUpEvent(SignUpEvent.EventType.ConfirmSignUpSuccess(confirmSignUpResponse))
            )
        }
    }

    override fun resendConfirmationCodeAction() = object : Action {
        override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
            val env = (environment as AuthEnvironment)
            val options = ResendConfirmationCodeRequest {
                clientId = env.configuration.userPool?.appClient
//                    username = event.username
            }

//            val resendConfirmationCodeResponse =
//                env.cognitoIdentityProviderClient.resendConfirmationCode(options)
//            val event = SignUpEvent(
//                SignUpEvent.EventType.ConfirmRetrySignUpSuccess(resendConfirmationCodeResponse)
//            )
//            dispatcher.send(event)
        }
    }
}