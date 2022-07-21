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

import aws.sdk.kotlin.services.polly.PollyClient
import aws.sdk.kotlin.services.polly.model.LanguageCode
import aws.sdk.kotlin.services.polly.model.OutputFormat
import aws.sdk.kotlin.services.polly.model.SynthesizeSpeechRequest
import aws.sdk.kotlin.services.polly.model.TextType
import aws.sdk.kotlin.services.polly.model.VoiceId
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.content.toByteArray
import com.amplifyframework.core.Consumer
import com.amplifyframework.predictions.PredictionsException
import com.amplifyframework.predictions.aws.AWSPredictionsPluginConfiguration
import com.amplifyframework.predictions.aws.models.AWSVoiceType
import com.amplifyframework.predictions.result.TextToSpeechResult
import java.io.InputStream
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking

/**
 * Predictions service for performing text to speech conversion.
 */
internal class AWSPollyService(
    private val pluginConfiguration: AWSPredictionsPluginConfiguration,
    private val authCredentialsProvider: CredentialsProvider
) {
    // TODO : replace PollyClient with PollyPresigningClient
    val client: PollyClient = PollyClient {
        this.region = pluginConfiguration.defaultRegion
        this.credentialsProvider = authCredentialsProvider
    }

    private val executor = Executors.newCachedThreadPool()

    companion object {
        private const val MP3_SAMPLE_RATE = 24000
    }

    fun synthesizeSpeech(
        text: String,
        voiceType: AWSVoiceType,
        onSuccess: Consumer<TextToSpeechResult>,
        onError: Consumer<PredictionsException>
    ) {
        execute(
            {
                val data = synthesizeSpeech(text, voiceType)
                TextToSpeechResult.fromAudioData(data)
            },
            { throwable ->
                PredictionsException(
                    "AWS Polly encountered an error while synthesizing speech.",
                    throwable,
                    "See attached exception for more details."
                )
            },
            onSuccess,
            onError
        )
    }

    private suspend fun synthesizeSpeech(text: String, voiceType: AWSVoiceType): InputStream {
        val languageCode: String
        val voiceId: String
        if (AWSVoiceType.UNKNOWN == voiceType) {
            // Obtain voice + language from plugin configuration by default
            val config = pluginConfiguration.speechGeneratorConfiguration
            languageCode = config.language
            voiceId = config.voice
        } else {
            // Override configuration defaults if explicitly specified in the options
            languageCode = voiceType.languageCode
            voiceId = voiceType.name
        }

        val synthesizeSpeechRequest = SynthesizeSpeechRequest {
            this.text = text
            this.textType = TextType.Text
            this.languageCode = LanguageCode.fromValue(languageCode)
            this.voiceId = VoiceId.fromValue(voiceId)
            this.outputFormat = OutputFormat.Mp3
            this.sampleRate = MP3_SAMPLE_RATE.toString()
        }
        // Synthesize speech from given text via Amazon Polly
        val audioStream = client.synthesizeSpeech(synthesizeSpeechRequest) { synthesizeSpeechResponse ->
            synthesizeSpeechResponse.audioStream?.toByteArray()?.inputStream()
        }
        return audioStream ?: "".byteInputStream()
    }

    private fun <T : Any> execute(
        runnableTask: suspend () -> T,
        errorTransformer: (Throwable) -> PredictionsException,
        onResult: Consumer<T>,
        onError: Consumer<PredictionsException>
    ) {
        executor.execute {
            try {
                runBlocking {
                    val result = runnableTask()
                    onResult.accept(result)
                }
            } catch (error: Throwable) {
                val predictionsException = errorTransformer.invoke(error)
                onError.accept(predictionsException)
            }
        }
    }
}
