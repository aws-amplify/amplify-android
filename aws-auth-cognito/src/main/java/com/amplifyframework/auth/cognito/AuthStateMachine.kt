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

import aws.sdk.kotlin.services.cognitoidentity.CognitoIdentityClient
import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import com.amplifyframework.auth.cognito.data.AuthConfiguration
import com.amplifyframework.auth.cognito.actions.*
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.StateMachine
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.auth.cognito.states.*
import com.amplifyframework.statemachine.codegen.actions.*

internal class AuthStateMachine(
    resolver: StateMachineResolver<AuthState>,
    environment: Environment
) :
    StateMachine<AuthState, Environment>(resolver, environment) {
    constructor(environment: Environment) : this(
        AuthState.Resolver(
            AuthenticationState.Resolver(
                CredentialStoreState.Resolver(),
                SignUpState.Resolver(SignUpCognitoActions),
                SRPSignInState.Resolver(SRPCognitoActions),
                SignOutState.Resolver(SignOutCognitoActions),
                AuthenticationCognitoActions
            ),
            AuthorizationState.Resolver(AuthorizationCognitoActions),
            AuthCognitoActions
        ), environment
    )

    companion object {
        private object defaultAuthActions : AuthActions
        private object defaultAuthenticationActions : AuthenticationActions
        private object defaultAuthorizationActions : AuthorizationActions
        private object defaultSRPActions : SRPActions
        private object defaultSignUpActions : SignUpActions
        private object defaultSignOutActions : SignOutActions

        fun logging() = AuthStateMachine(
            AuthState.Resolver(
                AuthenticationState.Resolver(
                    CredentialStoreState.Resolver().logging(),
                    SignUpState.Resolver(defaultSignUpActions).logging(),
                    SRPSignInState.Resolver(defaultSRPActions).logging(),
                    SignOutState.Resolver(defaultSignOutActions).logging(),
                    defaultAuthenticationActions
                ).logging(),
                AuthorizationState.Resolver(defaultAuthorizationActions).logging(),
                defaultAuthActions
            ).logging(), AuthEnvironment.empty
        )
    }
}

class AuthEnvironment : Environment {
    lateinit var configuration: AuthConfiguration
    internal lateinit var srpHelper: SRPHelper
    lateinit var cognitoIdentityProviderClient: CognitoIdentityProviderClient
    lateinit var cognitoIdentityClient: CognitoIdentityClient

    //TODO: temporary, needs to be in credential store
    var accessToken: String? = null

    companion object {
        val empty = AuthEnvironment()
    }
}