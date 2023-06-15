/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.predictions.aws

import android.net.UrlQuerySanitizer
import aws.sdk.kotlin.services.polly.PollyClient
import aws.sdk.kotlin.services.polly.model.LanguageCode
import aws.sdk.kotlin.services.polly.model.SynthesizeSpeechRequest
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.util.Attributes
import aws.smithy.kotlin.runtime.util.emptyAttributes
import com.amplifyframework.predictions.aws.service.AmazonPollyPresigningClient
import com.amplifyframework.predictions.aws.service.PresignedSynthesizeSpeechUrlOptions
import io.mockk.coVerify
import io.mockk.spyk
import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests creating a presigned SynthesizeSpeech
 * URL from Amazon Polly.
 */
class AmazonPollyPresigningClientTest {
    private lateinit var pollyPresigningClient: AmazonPollyPresigningClient
    private val defaultCredentialsProvider = spyk(TestCredentialsProvider())
    private val otherCredentialsProvider = spyk(TestCredentialsProvider())

    companion object {
        private const val TEXT_KEY = "Text"
        private const val LANGUAGE_CODE_KEY = "LanguageCode"
        private const val EXPIRES_KEY = "X-Amz-Expires"
    }

    /**
     * Configure AmazonPollyPresigningClient before each test.
     */
    @Before
    fun setup() {
        pollyPresigningClient = AmazonPollyPresigningClient(
            PollyClient {
                this.region = "us-east-1"
                this.credentialsProvider = defaultCredentialsProvider
            }
        )
    }

    /**
     * Tests that a presigned synthesize speech URL can be generated.
     */
    @Test
    fun testGetPresignedUrl() {
        val request = SynthesizeSpeechRequest { }
        val presignedUrl = pollyPresigningClient.getPresignedSynthesizeSpeechUrl(request)
        coVerify { defaultCredentialsProvider.resolve(emptyAttributes()) }
        assertNotNull(presignedUrl)
    }

    /**
     * Tests that a presigned synthesize speech URL is generated with a given text.
     */
    @Test
    fun testGetPresignedUrlWithText() {
        val request = SynthesizeSpeechRequest {
            this.text = "hello"
        }
        val presignedUrl = pollyPresigningClient.getPresignedSynthesizeSpeechUrl(request)
        coVerify { defaultCredentialsProvider.resolve(emptyAttributes()) }
        assertNotNull(presignedUrl)
        checkUrlForQueryParameter(presignedUrl, TEXT_KEY, request.text!!)
    }

    /**
     * Tests that a presigned synthesize speech URL is generated with a given language code.
     */
    @Test
    fun testGetPresignedUrlWithLanguageCode() {
        val request = SynthesizeSpeechRequest {
            this.languageCode = LanguageCode.EnUs
        }
        val presignedUrl = pollyPresigningClient.getPresignedSynthesizeSpeechUrl(request)
        coVerify { defaultCredentialsProvider.resolve(emptyAttributes()) }
        assertNotNull(presignedUrl)
        checkUrlForQueryParameter(presignedUrl, LANGUAGE_CODE_KEY, request.languageCode?.value ?: "")
    }

    /**
     * Tests that a presigned synthesize speech URL is generated with the provided credentials provider.
     */
    @Test
    fun testGetPresignedUrlWithCredentialsProvider() {
        val request = SynthesizeSpeechRequest { }
        val options = PresignedSynthesizeSpeechUrlOptions
            .builder()
            .credentialsProvider(otherCredentialsProvider)
            .build()
        val presignedUrl = pollyPresigningClient.getPresignedSynthesizeSpeechUrl(request, options)
        coVerify { otherCredentialsProvider.resolve(emptyAttributes()) }
        assertNotNull(presignedUrl)
    }

    /**
     * Tests that a presigned synthesize speech URL is generated with the provided expiration time.
     */
    @Test
    fun testGetPresignedUrlWithExpiration() {
        val request = SynthesizeSpeechRequest { }
        val options = PresignedSynthesizeSpeechUrlOptions
            .builder()
            .expires(60)
            .build()
        val presignedUrl = pollyPresigningClient.getPresignedSynthesizeSpeechUrl(request, options)
        coVerify { defaultCredentialsProvider.resolve(emptyAttributes()) }
        assertNotNull(presignedUrl)
        checkUrlForQueryParameter(presignedUrl, EXPIRES_KEY, options.expires.toString())
    }

    private fun checkUrlForQueryParameter(url: URL, parameterKey: String, expectedParameterValue: String) {
        val querySanitizer = UrlQuerySanitizer(url.toString())
        var parameterFound = false
        querySanitizer.parameterList.forEach { parameterValuePair ->
            if (parameterValuePair.mParameter.equals(parameterKey)) {
                parameterFound = true
                assertEquals(expectedParameterValue, parameterValuePair.mValue)
            }
        }
        assertTrue(parameterFound)
    }

    open class TestCredentialsProvider : CredentialsProvider {
        override suspend fun resolve(attributes: Attributes): Credentials {
            return Credentials("testAccessKey", "testSecretKey")
        }
    }
}
