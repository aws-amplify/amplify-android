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
