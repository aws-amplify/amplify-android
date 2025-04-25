package com.amplifyframework.aws.appsync.core

import io.kotest.matchers.shouldBe
import org.junit.Test

class AppSyncRequestTest {

    @Test
    fun `test request implementation`() {
        val testRequest = object : AppSyncRequest {
            override val method = AppSyncRequest.HttpMethod.POST
            override val url = "https://amazon.com"
            override val headers = mapOf(
                HeaderKeys.API_KEY to "123",
                HeaderKeys.AUTHORIZATION to "345",
                HeaderKeys.AMAZON_DATE to "2025"
            )
            override val body = "b"
        }

        testRequest.method shouldBe AppSyncRequest.HttpMethod.POST
        testRequest.url shouldBe "https://amazon.com"
        testRequest.headers shouldBe mapOf(
            HeaderKeys.API_KEY to "123",
            HeaderKeys.AUTHORIZATION to "345",
            HeaderKeys.AMAZON_DATE to "2025"
        )
        testRequest.body shouldBe "b"
    }
}
