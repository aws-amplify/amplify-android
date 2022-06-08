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

import com.amplifyframework.auth.cognito.actions.*
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.StateMachine
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.codegen.states.*

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
                    SignUpState.Resolver(SignUpCognitoActions).logging(),
                    SRPSignInState.Resolver(SRPCognitoActions).logging(),
                    SignOutState.Resolver(SignOutCognitoActions).logging(),
                    AuthenticationCognitoActions
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
