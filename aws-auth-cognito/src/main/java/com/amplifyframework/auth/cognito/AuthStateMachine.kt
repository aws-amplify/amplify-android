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
import com.amplifyframework.auth.cognito.actions.DeleteUserCognitoActions
import com.amplifyframework.auth.cognito.actions.DeviceSRPCognitoSignInActions
import com.amplifyframework.auth.cognito.actions.FetchAuthSessionCognitoActions
import com.amplifyframework.auth.cognito.actions.HostedUICognitoActions
import com.amplifyframework.auth.cognito.actions.MigrateAuthCognitoActions
import com.amplifyframework.auth.cognito.actions.SRPCognitoActions
import com.amplifyframework.auth.cognito.actions.SetupTOTPCognitoActions
import com.amplifyframework.auth.cognito.actions.SignInChallengeCognitoActions
import com.amplifyframework.auth.cognito.actions.SignInCognitoActions
import com.amplifyframework.auth.cognito.actions.SignInCustomCognitoActions
import com.amplifyframework.auth.cognito.actions.SignOutCognitoActions
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.StateMachine
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.CustomSignInState
import com.amplifyframework.statemachine.codegen.states.DeleteUserState
import com.amplifyframework.statemachine.codegen.states.DeviceSRPSignInState
import com.amplifyframework.statemachine.codegen.states.FetchAuthSessionState
import com.amplifyframework.statemachine.codegen.states.HostedUISignInState
import com.amplifyframework.statemachine.codegen.states.MigrateSignInState
import com.amplifyframework.statemachine.codegen.states.RefreshSessionState
import com.amplifyframework.statemachine.codegen.states.SRPSignInState
import com.amplifyframework.statemachine.codegen.states.SetupTOTPState
import com.amplifyframework.statemachine.codegen.states.SignInChallengeState
import com.amplifyframework.statemachine.codegen.states.SignInState
import com.amplifyframework.statemachine.codegen.states.SignOutState

internal class AuthStateMachine(
    resolver: StateMachineResolver<AuthState>,
    environment: Environment,
    initialState: AuthState? = null
) :
    StateMachine<AuthState, Environment>(resolver, environment, initialState = initialState) {
    constructor(environment: Environment, initialState: AuthState? = null) : this(
        AuthState.Resolver(
            AuthenticationState.Resolver(
                SignInState.Resolver(
                    SRPSignInState.Resolver(SRPCognitoActions),
                    CustomSignInState.Resolver(SignInCustomCognitoActions),
                    MigrateSignInState.Resolver(MigrateAuthCognitoActions),
                    SignInChallengeState.Resolver(SignInChallengeCognitoActions),
                    HostedUISignInState.Resolver(HostedUICognitoActions),
                    DeviceSRPSignInState.Resolver(DeviceSRPCognitoSignInActions),
                    SetupTOTPState.Resolver(SetupTOTPCognitoActions),
                    SignInCognitoActions
                ),
                SignOutState.Resolver(SignOutCognitoActions),
                AuthenticationCognitoActions
            ),
            AuthorizationState.Resolver(
                FetchAuthSessionState.Resolver(FetchAuthSessionCognitoActions),
                RefreshSessionState.Resolver(
                    FetchAuthSessionState.Resolver(FetchAuthSessionCognitoActions),
                    FetchAuthSessionCognitoActions
                ),
                DeleteUserState.Resolver(DeleteUserCognitoActions),
                AuthorizationCognitoActions
            ),
            AuthCognitoActions
        ),
        environment,
        initialState
    )

    companion object {
        fun logging(environment: Environment) = AuthStateMachine(
            AuthState.Resolver(
                AuthenticationState.Resolver(
                    SignInState.Resolver(
                        SRPSignInState.Resolver(SRPCognitoActions).logging(),
                        CustomSignInState.Resolver(SignInCustomCognitoActions).logging(),
                        MigrateSignInState.Resolver(MigrateAuthCognitoActions).logging(),
                        SignInChallengeState.Resolver(SignInChallengeCognitoActions).logging(),
                        HostedUISignInState.Resolver(HostedUICognitoActions).logging(),
                        DeviceSRPSignInState.Resolver(DeviceSRPCognitoSignInActions).logging(),
                        SetupTOTPState.Resolver(SetupTOTPCognitoActions).logging(),
                        SignInCognitoActions
                    ).logging(),
                    SignOutState.Resolver(SignOutCognitoActions).logging(),
                    AuthenticationCognitoActions
                ).logging(),
                AuthorizationState.Resolver(
                    FetchAuthSessionState.Resolver(FetchAuthSessionCognitoActions).logging(),
                    RefreshSessionState.Resolver(
                        FetchAuthSessionState.Resolver(FetchAuthSessionCognitoActions).logging(),
                        FetchAuthSessionCognitoActions
                    ).logging(),
                    DeleteUserState.Resolver(DeleteUserCognitoActions),
                    AuthorizationCognitoActions
                ).logging(),
                AuthCognitoActions
            ).logging(),
            environment
        )
    }
}
