package com.amazonaws.sdk.appsync.events.testmodels

import kotlinx.serialization.Serializable

@Serializable
data class TestUser(
    val name: String = "John Doe",
    val handle: String = "@johndoe"
)
