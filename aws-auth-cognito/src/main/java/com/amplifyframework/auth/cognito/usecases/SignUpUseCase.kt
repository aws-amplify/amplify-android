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
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignUpOptions
import com.amplifyframework.auth.cognito.throwIfNotConfigured
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.statemachine.codegen.data.SignUpData
import com.amplifyframework.statemachine.codegen.events.SignUpEvent
import com.amplifyframework.statemachine.codegen.states.SignUpState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformWhile

internal class SignUpUseCase(private val stateMachine: AuthStateMachine) {
    suspend fun execute(username: String, password: String?, options: AuthSignUpOptions): AuthSignUpResult {
        stateMachine.throwIfNotConfigured()

        val awsOptions = options as? AWSCognitoAuthSignUpOptions

        val result = stateMachine.stateTransitions.onStart {
            val validationData = awsOptions?.validationData
            val clientMetadata = awsOptions?.clientMetadata
            val signupData = SignUpData(username, validationData, clientMetadata)
            val event = SignUpEvent(SignUpEvent.EventType.InitiateSignUp(signupData, password, options.userAttributes))
            stateMachine.send(event)
        }.transformWhile { authState ->
            when (val signUpState = authState.authSignUpState) {
                is SignUpState.AwaitingUserConfirmation -> {
                    emit(signUpState.signUpResult)
                    false
                }
                is SignUpState.SignedUp -> {
                    emit(signUpState.signUpResult)
                    false
                }
                is SignUpState.Error -> {
                    throw CognitoAuthExceptionConverter.lookup(signUpState.exception, "Sign up failed.")
                }
                else -> true
            }
        }.first()

        return result
    }
}
