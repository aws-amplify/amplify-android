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

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.polly.PollyClient
import aws.sdk.kotlin.services.polly.model.SynthesizeSpeechRequest
import aws.sdk.kotlin.services.polly.presigners.PollyPresignConfig
import aws.sdk.kotlin.services.polly.presigners.presign
import kotlinx.coroutines.runBlocking
import java.net.URL
import kotlin.time.Duration.Companion.seconds

class AmazonPollyPresigningClient(pollyClient: PollyClient): PollyClient by pollyClient {
    
    fun getPresignedSynthesizeSpeechUrl(synthesizeSpeechRequest: SynthesizeSpeechRequest): URL {
        return getPresignedSynthesizeSpeechUrl(synthesizeSpeechRequest, PresignedSynthesizeSpeechUrlOptions.defaults())
    }
    
    fun getPresignedSynthesizeSpeechUrl(synthesizeSpeechRequest: SynthesizeSpeechRequest,
                                        options: PresignedSynthesizeSpeechUrlOptions): URL {
        val presignCredentialsProvider = if (options.credentials != null) {
            StaticCredentialsProvider {
                accessKeyId = options.credentials.accessKeyId
                secretAccessKey = options.credentials.secretAccessKey
            }
        } else {
            this.config.credentialsProvider
        }
        val presignConfig = PollyPresignConfig {
            region = this@AmazonPollyPresigningClient.config.region
            credentialsProvider = presignCredentialsProvider
        }
        val presignedRequest = runBlocking {
            synthesizeSpeechRequest.presign(presignConfig, options.expires.seconds)
        }
        return URL(presignedRequest.url.toString())
    }
}