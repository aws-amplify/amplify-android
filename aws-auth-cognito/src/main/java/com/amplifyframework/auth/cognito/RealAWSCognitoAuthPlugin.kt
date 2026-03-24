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

package com.amplifyframework.auth.cognito

import androidx.annotation.WorkerThread
import com.amplifyframework.AmplifyException
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.auth.AWSCognitoAuthMetadataType
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.StateChangeListenerToken
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.states.AuthState
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile

@Suppress("ktlint:standard:function-naming")
internal class RealAWSCognitoAuthPlugin(
    val configuration: AuthConfiguration,
    private val authEnvironment: AuthEnvironment,
    private val authStateMachine: AuthStateMachine,
    private val logger: Logger
) {
    init {
        addAuthStateChangeListener()
        configureAuthStates()
    }

    fun escapeHatch() = authEnvironment.cognitoAuthService

    @InternalAmplifyApi
    fun addToUserAgent(type: AWSCognitoAuthMetadataType, value: String) {
        authEnvironment.cognitoAuthService.customUserAgentPairs[type.key] = value
    }

    @WorkerThread
    @Throws(AmplifyException::class)
    fun initialize() {
        val token = StateChangeListenerToken()
        val latch = CountDownLatch(1)
        authStateMachine.listen(
            token,
            { authState ->
                if (authState is AuthState.Configured) {
                    authStateMachine.cancel(token)
                    latch.countDown()
                }
            },
            { }
        )
        try {
            latch.await(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            throw AmplifyException(
                "Failed to configure auth plugin.",
                "Make sure your amplifyconfiguration.json is valid"
            )
        }
    }

    internal suspend fun suspendWhileConfiguring() {
        authStateMachine.state.takeWhile { it !is AuthState.Configured && it !is AuthState.Error }.collect()
    }

    private fun addAuthStateChangeListener() {
        authStateMachine.listen(
            StateChangeListenerToken(),
            { authState -> logger.verbose("Auth State Change: $authState") },
            null
        )
    }

    private fun configureAuthStates() {
        val token = StateChangeListenerToken()
        authStateMachine.listen(
            token,
            { authState ->
                when (authState) {
                    is AuthState.Configured -> {
                        authStateMachine.cancel(token)
                    }
                    else -> Unit // handle errors
                }
            },
            {
                authStateMachine.send(AuthEvent(AuthEvent.EventType.ConfigureAuth(configuration)))
            }
        )
    }
}
