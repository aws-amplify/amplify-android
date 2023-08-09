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

import android.content.Context
import com.amplifyframework.auth.cognito.data.AWSCognitoAuthCredentialStore
import com.amplifyframework.auth.cognito.data.AWSCognitoLegacyCredentialStore
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.StateChangeListenerToken
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.events.CredentialStoreEvent
import com.amplifyframework.statemachine.codegen.states.CredentialStoreState
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal interface StoreClientBehavior {
    suspend fun loadCredentials(credentialType: CredentialType): AmplifyCredential
    suspend fun storeCredentials(credentialType: CredentialType, amplifyCredential: AmplifyCredential)
    suspend fun clearCredentials(credentialType: CredentialType)
}

internal class CredentialStoreClient(configuration: AuthConfiguration, context: Context, val logger: Logger) :
    StoreClientBehavior {
    private val credentialStoreStateMachine = createCredentialStoreStateMachine(configuration, context)

    private fun createCredentialStoreStateMachine(
        configuration: AuthConfiguration,
        context: Context
    ): CredentialStoreStateMachine {
        val awsCognitoAuthCredentialStore = AWSCognitoAuthCredentialStore(context.applicationContext, configuration)
        val legacyCredentialStore = AWSCognitoLegacyCredentialStore(context.applicationContext, configuration)
        val credentialStoreEnvironment =
            CredentialStoreEnvironment(awsCognitoAuthCredentialStore, legacyCredentialStore, logger)
        return CredentialStoreStateMachine(credentialStoreEnvironment)
    }

    private fun listenForResult(
        event: CredentialStoreEvent,
        onSuccess: (Result<AmplifyCredential>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val token = StateChangeListenerToken()
        val credentialStoreStateListener = OneShotCredentialStoreStateListener(
            {
                credentialStoreStateMachine.cancel(token)
                onSuccess(it)
            }, {
            credentialStoreStateMachine.cancel(token)
            onError(it)
        },
            logger
        )
        credentialStoreStateMachine.listen(
            token,
            credentialStoreStateListener::listen
        ) { credentialStoreStateMachine.send(event) }
    }

    override suspend fun loadCredentials(credentialType: CredentialType): AmplifyCredential {
        return suspendCoroutine { continuation ->
            listenForResult(
                CredentialStoreEvent(CredentialStoreEvent.EventType.LoadCredentialStore(credentialType)),
                { continuation.resumeWith(it) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun storeCredentials(credentialType: CredentialType, amplifyCredential: AmplifyCredential) {
        return suspendCoroutine { continuation ->
            listenForResult(
                CredentialStoreEvent(
                    CredentialStoreEvent.EventType.StoreCredentials(credentialType, amplifyCredential)
                ),
                { continuation.resumeWith(Result.success(Unit)) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    override suspend fun clearCredentials(credentialType: CredentialType) {
        return suspendCoroutine { continuation ->
            listenForResult(
                CredentialStoreEvent(CredentialStoreEvent.EventType.ClearCredentialStore(credentialType)),
                { continuation.resumeWith(Result.success(Unit)) },
                { continuation.resumeWithException(it) }
            )
        }
    }

    /*
    This class is a necessary workaround due to undesirable threading issues within the Auth State Machine. If state
    machine threading is improved, this class should be considered for removal.
     */
    internal class OneShotCredentialStoreStateListener(
        val onSuccess: (Result<AmplifyCredential>) -> Unit,
        val onError: (Exception) -> Unit,
        val logger: Logger
    ) {
        private var capturedSuccess: Result<AmplifyCredential>? = null
        private var capturedError: Exception? = null
        private val isActive = AtomicBoolean(true)
        fun listen(storeState: CredentialStoreState) {
            logger.verbose("Credential Store State Change: $storeState")
            when (storeState) {
                is CredentialStoreState.Success -> {
                    capturedSuccess = Result.success(storeState.storedCredentials)
                }

                is CredentialStoreState.Error -> {
                    capturedError = storeState.error
                }

                is CredentialStoreState.Idle -> {
                    val success = capturedSuccess
                    val error = capturedError

                    if ((success != null || error != null) && isActive.getAndSet(false)) {
                        if (success != null) {
                            onSuccess(success)
                        } else if (error != null) {
                            onError(error)
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}
