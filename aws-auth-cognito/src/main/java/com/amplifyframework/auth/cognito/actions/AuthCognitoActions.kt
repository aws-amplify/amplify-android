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

import aws.sdk.kotlin.services.cognitoidentity.CognitoIdentityClient
import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import com.amplifyframework.auth.cognito.AuthEnvironment
import com.amplifyframework.auth.cognito.events.AuthEvent
import com.amplifyframework.auth.cognito.events.AuthenticationEvent
import com.amplifyframework.auth.cognito.events.AuthorizationEvent
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.EventDispatcher
import com.amplifyframework.statemachine.codegen.actions.AuthActions

object AuthCognitoActions : AuthActions {
    override fun initializeAuthConfigurationAction(event: AuthEvent.EventType.ConfigureAuth) =
        object : Action {
            override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
                with(environment as AuthEnvironment) {
                    configuration = event.configuration
                    val configureEvent: AuthEvent = configuration.identityPool?.run {
                        AuthEvent(AuthEvent.EventType.ConfigureAuthorization(configuration))
                    } ?: AuthEvent(
                        AuthEvent.EventType.ConfigureAuthentication(
                            configuration,
                            event.storedCredentials
                        )
                    )
                    dispatcher.send(configureEvent)
                }
            }
        }

    override fun initializeAuthenticationConfigurationAction(event: AuthEvent.EventType.ConfigureAuthentication) =
        object : Action {
            override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
                with(environment as AuthEnvironment) {
                    configuration.userPool?.let {
                        cognitoIdentityProviderClient = CognitoIdentityProviderClient {
                            this.region = configuration.userPool?.region
                        }
                    }
                    dispatcher.send(
                        AuthenticationEvent(
                            AuthenticationEvent.EventType.Configure(
                                event.configuration,
                                event.storedCredentials
                            )
                        )
                    )
                }
            }
        }

    override fun initializeAuthorizationConfigurationAction(event: AuthEvent.EventType) =
        object : Action {
            override suspend fun execute(dispatcher: EventDispatcher, environment: Environment) {
                with(environment as AuthEnvironment) {
                    configuration.identityPool?.let {
                        cognitoIdentityClient = CognitoIdentityClient {
                            this.region = configuration.identityPool?.region
                        }
                    }
                    dispatcher.send(
                        AuthorizationEvent(
                            AuthorizationEvent.EventType.Configure(
                                configuration
                            )
                        )
                    )
                }
            }
        }
}