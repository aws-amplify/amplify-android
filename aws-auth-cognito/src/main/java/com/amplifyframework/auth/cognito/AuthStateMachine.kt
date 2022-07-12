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
import com.amplifyframework.auth.cognito.actions.DeleteUserActions
import com.amplifyframework.auth.cognito.actions.FetchAuthSessionCognitoActions
import com.amplifyframework.auth.cognito.actions.FetchAwsCredentialsCognitoActions
import com.amplifyframework.auth.cognito.actions.FetchIdentityCognitoActions
import com.amplifyframework.auth.cognito.actions.FetchUserPoolTokensCognitoActions
import com.amplifyframework.auth.cognito.actions.SRPCognitoActions
import com.amplifyframework.auth.cognito.actions.SignOutCognitoActions
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.StateMachine
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.DeleteUserState
import com.amplifyframework.statemachine.codegen.states.FetchAuthSessionState
import com.amplifyframework.statemachine.codegen.states.FetchAwsCredentialsState
import com.amplifyframework.statemachine.codegen.states.FetchIdentityState
import com.amplifyframework.statemachine.codegen.states.FetchUserPoolTokensState
import com.amplifyframework.statemachine.codegen.states.SRPSignInState
import com.amplifyframework.statemachine.codegen.states.SignOutState

internal class AuthStateMachine(
    resolver: StateMachineResolver<AuthState>,
    environment: Environment
) :
    StateMachine<AuthState, Environment>(resolver, environment) {
    constructor(environment: Environment) : this(
        AuthState.Resolver(
            AuthenticationState.Resolver(
                SRPSignInState.Resolver(SRPCognitoActions),
                SignOutState.Resolver(SignOutCognitoActions),
                AuthenticationCognitoActions,
            ),
            AuthorizationState.Resolver(
                FetchAuthSessionState.Resolver(
                    FetchAwsCredentialsState.Resolver(FetchAwsCredentialsCognitoActions),
                    FetchIdentityState.Resolver(FetchIdentityCognitoActions),
                    FetchUserPoolTokensState.Resolver(FetchUserPoolTokensCognitoActions),
                    FetchAuthSessionCognitoActions
                ),
                DeleteUserState.Resolver(DeleteUserActions),
                AuthorizationCognitoActions
            ),
            AuthCognitoActions
        ),
        environment
    )

    companion object {
        fun logging(environment: Environment) = AuthStateMachine(
            AuthState.Resolver(
                AuthenticationState.Resolver(
                    SRPSignInState.Resolver(SRPCognitoActions).logging(),
                    SignOutState.Resolver(SignOutCognitoActions).logging(),
                    AuthenticationCognitoActions,
                ).logging(),
                AuthorizationState.Resolver(
                    FetchAuthSessionState.Resolver(
                        FetchAwsCredentialsState.Resolver(FetchAwsCredentialsCognitoActions).logging(),
                        FetchIdentityState.Resolver(FetchIdentityCognitoActions).logging(),
                        FetchUserPoolTokensState.Resolver(FetchUserPoolTokensCognitoActions).logging(),
                        FetchAuthSessionCognitoActions
                    ).logging(),
                    DeleteUserState.Resolver(DeleteUserActions),
                    AuthorizationCognitoActions
                ).logging(),
                AuthCognitoActions
            ).logging(),
            environment
        )
    }
}
