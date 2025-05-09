package com.amazonaws.sdk.appsync.events.testmodels

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class TestMessage(
    val messageId: String = UUID.randomUUID().toString(),
    val content: String = "Hello World",
    val likes: Int = 2,
    val likedBy: List<TestUser> = listOf(
        TestUser("Jane Doe", "@janedoe"),
        TestUser("Jim Doe", "@jimdoe")
    ),
    val author: TestUser = TestUser()
)
