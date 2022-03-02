package com.amplifyframework.auth.cognito.data

data class CredentialStoreError(override val message: String, override val cause: Throwable? = null) : Error() {
    val type = "Unknown Error"
}
