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
package com.amplifyframework.predictions.aws.service

import androidx.annotation.WorkerThread
import aws.sdk.kotlin.services.polly.PollyClient
import aws.sdk.kotlin.services.polly.model.SynthesizeSpeechRequest
import aws.sdk.kotlin.services.polly.presigners.presignSynthesizeSpeech
import aws.smithy.kotlin.runtime.util.emptyAttributes
import java.net.URL
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Client for accessing Amazon Polly and generating a presigned URL of an
 * Amazon Polly SynthesizeSpeech request.
 */
class AmazonPollyPresigningClient(private val pollyClient: PollyClient) : PollyClient by pollyClient {

    /**
     * Creates a presigned URL for a SynthesizeSpeech request.
     * @param synthesizeSpeechRequest The request to create a presigned URL of.
     * @return a presigned URL for a SynthesizeSpeech request.
     */
    @WorkerThread
    fun getPresignedSynthesizeSpeechUrl(synthesizeSpeechRequest: SynthesizeSpeechRequest): URL {
        return getPresignedSynthesizeSpeechUrl(synthesizeSpeechRequest, PresignedSynthesizeSpeechUrlOptions.defaults())
    }

    /**
     * Creates a presigned URL for a SynthesizeSpeech request with the given options.
     * @param synthesizeSpeechRequest The request to create a presigned URL of.
     * @param options The options for creating the presigned URL.
     * @return a presigned URL for a SynthesizeSpeech request.
     */
    @WorkerThread
    fun getPresignedSynthesizeSpeechUrl(
        synthesizeSpeechRequest: SynthesizeSpeechRequest,
        options: PresignedSynthesizeSpeechUrlOptions
    ): URL {
        val presignCredentialsProvider = options.credentialsProvider ?: this.config.credentialsProvider
        val presignedRequest = runBlocking {
            val credentials = presignCredentialsProvider.resolve(emptyAttributes())
            pollyClient.presignSynthesizeSpeech(synthesizeSpeechRequest) {
                this.expiresAfter = options.expires.seconds
                this.credentials = credentials
            }
        }
        return URL(presignedRequest.url.toString())
    }
}
