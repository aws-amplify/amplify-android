package com.amplifyframework.aws.appsync.core.util

import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigner
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.toHttpBody
import com.amplifyframework.auth.AuthCredentialsProvider
import com.amplifyframework.aws.appsync.core.AppSyncRequest
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.maps.shouldContain
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AppSyncRequestSignerTest {
    @OptIn(InternalApi::class)
    @Test
    fun `signs request`() = runTest {
        val expectedUrl = "https://amazon.com"
        val expectedBody = "hello"
        val expectedHeaders = mapOf("k1" to "v1")
        val credentialProvider = mockk<AuthCredentialsProvider> {
            coEvery { resolve(any()) } returns mockk()
        }
        val slot = CapturingSlot<HttpRequest>()
        val signer = mockk<AwsSigner> {
            coEvery { sign(capture(slot), any()) } returns mockk {
                every { output.headers.entries() } returns mapOf("test" to listOf("value")).entries
            }
        }
        val request = object : AppSyncRequest {
            override val method = AppSyncRequest.HttpMethod.POST
            override val url = expectedUrl
            override val headers = expectedHeaders
            override val body = expectedBody
        }
        val requestSigner = AppSyncRequestSigner(credentialProvider, signer)

        val result = requestSigner.signAppSyncRequest(request, "us-east-1")
        val signedRequest = slot.captured
        signedRequest.url.toString() shouldBeEqual expectedUrl
        signedRequest.method shouldBeEqual HttpMethod.POST
        signedRequest.body shouldBeEqual expectedBody.toHttpBody()
        signedRequest.headers shouldBeEqual Headers { append("k1", "v1") }
        result shouldContain ("test" to "value")
    }
}
