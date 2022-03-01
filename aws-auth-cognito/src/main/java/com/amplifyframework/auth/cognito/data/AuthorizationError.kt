package com.amplifyframework.auth.cognito.data

import com.amplifyframework.statemachine.State

data class AuthorizationError(val msg: String) : State, Error() {
    override val type = "Unknown Error."
}
