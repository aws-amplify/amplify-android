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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthFlowType
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.events.FetchUserPoolTokensEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher
import java.lang.Exception

interface FetchUserPoolTokensAction : Action

//TODO: This action will be called from FetchAuthSession SM
class ConfigureFetchUserPoolTokensAction : FetchUserPoolTokensAction {
    override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
        val env = (environment as AuthEnvironment)
        //TODO :Check the validity of credentials dropped into here from FetchAuthSession SM
        //TODO: If not valid call Refresh
        //TODO: Also check if the userPool is enabled(Basically check if userPool exists). If not then exit this SM with
        //a Fetched state

        val event =
            FetchUserPoolTokensEvent(
                FetchUserPoolTokensEvent.EventType.Fetched()
            )
        dispatcher.send(event)
    }
}

class RefreshFetchUserPoolTokensAction : FetchUserPoolTokensAction {
    override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
        val env = (environment as AuthEnvironment)
        try {
            val refreshTokenResponse = env.cognitoAuthService.cognitoIdentityProviderClient?.initiateAuth {
                authFlow = AuthFlowType.RefreshToken
                clientId = env.configuration.userPool?.appClient
                authParameters = mapOf(
                    "REFRESH_TOKEN" to "REFRESH_TOKEN_FROM_AmplifyCredential"
                )
            }
            refreshTokenResponse?.authenticationResult?.let {
                //TODO: Once the credentialStorage and AmplifyCentral is implemented, initialize/replace those values here.
                val refreshToken = it.refreshToken
                val idToken = it.idToken
            }
            val event =
                FetchUserPoolTokensEvent(
                    FetchUserPoolTokensEvent.EventType.Fetched()
                )
            dispatcher.send(event)
        } catch (e: Exception) {
            val event =
                FetchUserPoolTokensEvent(
                    FetchUserPoolTokensEvent.EventType.ThrowError(e.localizedMessage)
                )
            dispatcher.send(event)
        }
    }
}