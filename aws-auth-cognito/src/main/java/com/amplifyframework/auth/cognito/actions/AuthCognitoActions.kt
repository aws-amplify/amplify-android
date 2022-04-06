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
import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.actions.AuthActions
import com.amplifyframework.statemachine.codegen.events.AuthEvent
import com.amplifyframework.statemachine.codegen.events.AuthenticationEvent
import com.amplifyframework.statemachine.codegen.events.AuthorizationEvent

object AuthCognitoActions : AuthActions {
    override fun initializeAuthConfigurationAction(event: AuthEvent.EventType.ConfigureAuth) =
        Action { dispatcher, environment ->
            with(environment as AuthEnvironment) {
                configuration = event.configuration
                val configureEvent: AuthEvent = configuration.userPool?.run {
                    AuthEvent(AuthEvent.EventType.ConfigureAuthentication(configuration, event.storedCredentials))
                } ?: AuthEvent(AuthEvent.EventType.ConfigureAuthorization(configuration))
                dispatcher.send(configureEvent)
            }
        }

    override fun initializeAuthenticationConfigurationAction(event: AuthEvent.EventType.ConfigureAuthentication) =
        Action { dispatcher, environment ->
            with(environment as AuthEnvironment) {
                configuration.userPool?.let {
                    cognitoAuthService.cognitoIdentityProviderClient =
                        CognitoIdentityProviderClient {
                            this.region = configuration.userPool?.region
                        }
                }
                dispatcher.send(
                    AuthenticationEvent(
                        AuthenticationEvent.EventType.Configure(event.configuration, event.storedCredentials)
                    )
                )
            }
        }

    override fun initializeAuthorizationConfigurationAction(event: AuthEvent.EventType) =
        Action { dispatcher, environment ->
            with(environment as AuthEnvironment) {
                configuration.identityPool?.let {
                    cognitoAuthService.cognitoIdentityClient = CognitoIdentityClient {
                        this.region = configuration.identityPool?.region
                    }
                }
                dispatcher.send(AuthorizationEvent(AuthorizationEvent.EventType.Configure(configuration)))
            }
        }
}
