/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.usecases

import com.amplifyframework.auth.cognito.AuthStateMachine
import com.amplifyframework.auth.cognito.CognitoAuthExceptionConverter
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthConfirmSignUpOptions
import com.amplifyframework.auth.cognito.throwIfNotConfigured
import com.amplifyframework.auth.options.AuthConfirmSignUpOptions
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.statemachine.codegen.data.SignUpData
import com.amplifyframework.statemachine.codegen.events.SignUpEvent
import com.amplifyframework.statemachine.codegen.states.SignUpState
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.transformWhile

internal class ConfirmSignUpUseCase(private val stateMachine: AuthStateMachine) {
    suspend fun execute(
        username: String,
        confirmationCode: String,
        options: AuthConfirmSignUpOptions = AuthConfirmSignUpOptions.defaults()
    ): AuthSignUpResult {
        stateMachine.throwIfNotConfigured()

        val startingState = stateMachine.getCurrentState().authSignUpState

        val result = stateMachine.state
            .onSubscription {
                var userId: String? = null
                var session: String? = null
                if (startingState is SignUpState.AwaitingUserConfirmation &&
                    startingState.signUpData.username == username
                ) {
                    session = startingState.signUpData.session
                    userId = startingState.signUpResult.userId
                }
                val clientMetadata = (options as? AWSCognitoAuthConfirmSignUpOptions)?.clientMetadata
                val signupData = SignUpData(username, null, clientMetadata, session, userId)
                val event = SignUpEvent(SignUpEvent.EventType.ConfirmSignUp(signupData, confirmationCode))
                stateMachine.send(event)
            }
            .drop(1)
            .transformWhile { authState ->
                when (val signUpState = authState.authSignUpState) {
                    is SignUpState.Error -> {
                        throw CognitoAuthExceptionConverter.lookup(signUpState.exception, "Sign up failed.")
                    }
                    is SignUpState.SignedUp -> {
                        emit(signUpState.signUpResult)
                        false
                    }
                    else -> true
                }
            }.first()

        return result
    }
}
