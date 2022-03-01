package com.amplifyframework.auth.cognito.data

import com.amplifyframework.statemachine.State
import java.lang.Error

data class CredentialStoreError(val msg: String) : State, Error() {
    override val type = "Unknown Error"
}
