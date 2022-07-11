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

import aws.sdk.kotlin.services.translate.TranslateClient
import aws.sdk.kotlin.services.translate.TranslateClient.Companion.invoke
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.amplifyframework.core.Consumer
import com.amplifyframework.predictions.PredictionsException
import com.amplifyframework.predictions.aws.AWSPredictionsPluginConfiguration
import com.amplifyframework.predictions.models.LanguageType
import com.amplifyframework.predictions.result.TranslateTextResult
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking

/**
 * Predictions service for performing text translation.
 */
internal class AWSTranslateService(
    private val pluginConfiguration: AWSPredictionsPluginConfiguration,
    private val authCredentialsProvider: CredentialsProvider
) {
    val client: TranslateClient = TranslateClient {
        this.region = pluginConfiguration.defaultRegion
        this.credentialsProvider = authCredentialsProvider
    }

    private val executor = Executors.newCachedThreadPool()

    fun translate(
        textToTranslate: String,
        sourceLanguage: LanguageType,
        targetLanguage: LanguageType,
        onSuccess: Consumer<TranslateTextResult>,
        onError: Consumer<PredictionsException>
    ) {
        execute(
            {
                val source =
                    if (LanguageType.UNKNOWN != sourceLanguage) sourceLanguage else
                        pluginConfiguration.translateTextConfiguration.sourceLanguage
                val target =
                    if (LanguageType.UNKNOWN != targetLanguage) targetLanguage else
                        pluginConfiguration.translateTextConfiguration.targetLanguage
                val result = client.translateText {
                    this.text = textToTranslate
                    this.sourceLanguageCode = source.languageCode
                    this.targetLanguageCode = target.languageCode
                }
                val translation = result.translatedText
                val targetCode = result.targetLanguageCode
                val language = LanguageType.from(targetCode)
                TranslateTextResult.builder()
                    .translatedText(translation ?: "")
                    .targetLanguage(language)
                    .build()
            },
            { throwable ->
                PredictionsException(
                    "AWS Translate encountered an error while translating text.",
                    throwable,
                    "See attached exception for more details."
                )
            },
            onSuccess,
            onError
        )
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
