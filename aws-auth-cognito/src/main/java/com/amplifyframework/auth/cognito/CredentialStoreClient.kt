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
import androidx.annotation.VisibleForTesting
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.data.AWSCognitoAuthCredentialStore
import com.amplifyframework.auth.cognito.data.AWSCognitoLegacyCredentialStore
import com.amplifyframework.auth.cognito.helpers.collectWhile
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CredentialType
import com.amplifyframework.statemachine.codegen.events.CredentialStoreEvent
import com.amplifyframework.statemachine.codegen.states.CredentialStoreState
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onSubscription

internal interface StoreClientBehavior {
    suspend fun loadCredentials(credentialType: CredentialType): AmplifyCredential
    suspend fun storeCredentials(credentialType: CredentialType, amplifyCredential: AmplifyCredential)
    suspend fun clearCredentials(credentialType: CredentialType)
}

internal class CredentialStoreClient @VisibleForTesting constructor(
    private val credentialStoreStateMachine: CredentialStoreStateMachine,
    val logger: Logger
) : StoreClientBehavior {

    constructor(configuration: AuthConfiguration, context: Context, logger: Logger) : this(
        credentialStoreStateMachine = createCredentialStoreStateMachine(configuration, context, logger),
        logger = logger
    )

    private suspend fun listenForResult(event: CredentialStoreEvent.EventType): AmplifyCredential {
        var result: Result<AmplifyCredential>? = null
        credentialStoreStateMachine.state
            .onSubscription { credentialStoreStateMachine.send(CredentialStoreEvent(event)) }
            .drop(1) // skip current state
            .onEach { state ->
                when (state) {
                    is CredentialStoreState.Error -> result = result ?: Result.failure(state.error)
                    is CredentialStoreState.Success -> result = Result.success(state.storedCredentials)
                    else -> Unit // no-op
                }
            }
            .collectWhile { state -> state !is CredentialStoreState.Idle }
        return result?.getOrThrow() ?: throw InvalidStateException(
            message = "Credential operation failed",
            recoverySuggestion = AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
        )
    }

    override suspend fun loadCredentials(credentialType: CredentialType): AmplifyCredential =
        listenForResult(CredentialStoreEvent.EventType.LoadCredentialStore(credentialType))

    override suspend fun storeCredentials(credentialType: CredentialType, amplifyCredential: AmplifyCredential) {
        listenForResult(CredentialStoreEvent.EventType.StoreCredentials(credentialType, amplifyCredential))
    }

    override suspend fun clearCredentials(credentialType: CredentialType) {
        listenForResult(CredentialStoreEvent.EventType.ClearCredentialStore(credentialType))
    }

    companion object {
        private fun createCredentialStoreStateMachine(
            configuration: AuthConfiguration,
            context: Context,
            logger: Logger
        ): CredentialStoreStateMachine {
            val awsCognitoAuthCredentialStore =
                AWSCognitoAuthCredentialStore(context.applicationContext, configuration)
            val legacyCredentialStore = AWSCognitoLegacyCredentialStore(context.applicationContext, configuration)
            val credentialStoreEnvironment =
                CredentialStoreEnvironment(awsCognitoAuthCredentialStore, legacyCredentialStore, logger)
            return CredentialStoreStateMachine(credentialStoreEnvironment)
        }
    }
}
