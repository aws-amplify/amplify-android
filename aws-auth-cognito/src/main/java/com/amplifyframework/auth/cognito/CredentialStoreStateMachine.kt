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

package com.amplifyframework.auth.cognito;

import android.content.Context
import com.amplifyframework.auth.cognito.data.AWSCognitoAuthCredentialStore
import com.amplifyframework.auth.cognito.data.AWSCognitoLegacyCredentialStore
import com.amplifyframework.auth.cognito.states.CredentialStoreState
import com.amplifyframework.statemachine.Environment
import com.amplifyframework.statemachine.StateMachine
import com.amplifyframework.statemachine.StateMachineResolver

internal class CredentialStoreStateMachine(
    resolver: StateMachineResolver<CredentialStoreState>,
    environment: Environment,
) : StateMachine<CredentialStoreState, Environment>(resolver, environment) {
    constructor(environment: Environment) : this(
        CredentialStoreState.Resolver(), environment
    )

    companion object {
        fun logging() = CredentialStoreStateMachine(
            CredentialStoreState.Resolver().logging(), CredentialStoreEnvironment.empty
        )
    }
}

class CredentialStoreEnvironment : Environment {
    internal lateinit var applicationContext: Context
    internal lateinit var credentialStore: AWSCognitoAuthCredentialStore
    internal lateinit var legacyCredentialStore: AWSCognitoLegacyCredentialStore

    companion object {
        val empty = CredentialStoreEnvironment()
    }
}
