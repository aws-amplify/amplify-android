package com.amplifyframework.statemachine.codegen.errors

data class CredentialStoreError(override val message: String, override val cause: Throwable? = null) : Exception()
