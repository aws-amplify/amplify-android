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

import aws.sdk.kotlin.services.cognitoidentity.model.GetIdRequest
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.events.FetchIdentityEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher

interface FetchIdentityAction : Action

class InitFetchIdentityAction : FetchIdentityAction {
    override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
        val env = (environment as AuthEnvironment)
        val getIdRequest = GetIdRequest.invoke {
            accountId = env.configuration.userPool?.appClient
            identityPoolId = env.configuration.identityPool?.poolId
        }
        try {
            val getIDResponse = env.cognitoAuthService.cognitoIdentityClient?.getId(getIdRequest)
            env.awsCognitoAuthCredentialStore.savePartialCredential(identityId = getIDResponse?.identityId)
            val event =
                FetchIdentityEvent(
                    FetchIdentityEvent.EventType.Fetched()
                )
            dispatcher.send(event)
        } catch (e: Exception) {
            val event =
                FetchIdentityEvent(
                    FetchIdentityEvent.EventType.ThrowError(e)
                )
            dispatcher.send(event)
        }
    }
}