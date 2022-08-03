package com.amplifyframework.statemachine.codegen.data

data class AuthChallenge(
    val challengeName: String,
    val username: String,
    val session: String?,
    val parameters: Map<String, String>?
)
