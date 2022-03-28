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

import com.amplifyframework.auth.cognito.actions.AuthCognitoActions
import com.amplifyframework.auth.cognito.actions.AuthenticationCognitoActions
import com.amplifyframework.auth.cognito.actions.AuthorizationCognitoActions
import com.amplifyframework.auth.cognito.actions.FetchAuthSessionActions
import com.amplifyframework.auth.cognito.actions.FetchAwsCredentialsActions
import com.amplifyframework.auth.cognito.actions.FetchIdentityActions
import com.amplifyframework.auth.cognito.actions.FetchUserPoolTokensActions
import com.amplifyframework.auth.cognito.actions.SRPCognitoActions
import com.amplifyframework.auth.cognito.actions.SignOutCognitoActions
import com.amplifyframework.auth.cognito.actions.SignUpCognitoActions
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.StateMachine
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.FetchAuthSessionState
import com.amplifyframework.statemachine.codegen.states.FetchAwsCredentialsState
import com.amplifyframework.statemachine.codegen.states.FetchIdentityState
import com.amplifyframework.statemachine.codegen.states.FetchUserPoolTokensState
import com.amplifyframework.statemachine.codegen.states.SRPSignInState
import com.amplifyframework.statemachine.codegen.states.SignOutState
import com.amplifyframework.statemachine.codegen.states.SignUpState

internal class AuthStateMachine(
    resolver: StateMachineResolver<AuthState>,
    environment: Environment
) :
    StateMachine<AuthState, Environment>(resolver, environment) {
    constructor(environment: Environment) : this(
        AuthState.Resolver(
            AuthenticationState.Resolver(
                SignUpState.Resolver(SignUpCognitoActions),
                SRPSignInState.Resolver(SRPCognitoActions),
                SignOutState.Resolver(SignOutCognitoActions),
                AuthenticationCognitoActions
            ),
            AuthorizationState.Resolver(
                FetchAuthSessionState.Resolver(
                    FetchAwsCredentialsState.Resolver(FetchAwsCredentialsActions),
                    FetchIdentityState.Resolver(FetchIdentityActions),
                    FetchUserPoolTokensState.Resolver(FetchUserPoolTokensActions),
                    FetchAuthSessionActions
                ),
                AuthorizationCognitoActions
            ),
            AuthCognitoActions
        ),
        environment
    )

    companion object {
        fun logging() = AuthStateMachine(
            AuthState.Resolver(
                AuthenticationState.Resolver(
                    SignUpState.Resolver(SignUpCognitoActions).logging(),
                    SRPSignInState.Resolver(SRPCognitoActions).logging(),
                    SignOutState.Resolver(SignOutCognitoActions).logging(),
                    AuthenticationCognitoActions
                ).logging(),
                AuthorizationState.Resolver(
                    FetchAuthSessionState.Resolver(
                        FetchAwsCredentialsState.Resolver(FetchAwsCredentialsActions).logging(),
                        FetchIdentityState.Resolver(FetchIdentityActions).logging(),
                        FetchUserPoolTokensState.Resolver(FetchUserPoolTokensActions).logging(),
                        FetchAuthSessionActions
                    ).logging(),
                    AuthorizationCognitoActions
                ).logging(),
                AuthCognitoActions
            ).logging(),
            AuthEnvironment.empty
        )
    }
}

class AuthEnvironment : Environment {
    lateinit var configuration: AuthConfiguration
    internal lateinit var srpHelper: SRPHelper

    val cognitoAuthService = AWSCognitoAuthService

    companion object {
        val empty = AuthEnvironment()
    }
}
