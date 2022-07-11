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

import aws.sdk.kotlin.services.comprehend.ComprehendClient
import aws.sdk.kotlin.services.comprehend.model.DominantLanguage
import aws.sdk.kotlin.services.comprehend.model.LanguageCode
import aws.sdk.kotlin.services.comprehend.model.SyntaxLanguageCode
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.amplifyframework.core.Consumer
import com.amplifyframework.predictions.PredictionsException
import com.amplifyframework.predictions.aws.AWSPredictionsPluginConfiguration
import com.amplifyframework.predictions.aws.adapter.EntityTypeAdapter
import com.amplifyframework.predictions.aws.adapter.SentimentTypeAdapter
import com.amplifyframework.predictions.aws.adapter.SpeechTypeAdapter
import com.amplifyframework.predictions.aws.configuration.InterpretTextConfiguration
import com.amplifyframework.predictions.models.Entity
import com.amplifyframework.predictions.models.EntityType
import com.amplifyframework.predictions.models.KeyPhrase
import com.amplifyframework.predictions.models.Language
import com.amplifyframework.predictions.models.LanguageType
import com.amplifyframework.predictions.models.Sentiment
import com.amplifyframework.predictions.models.SentimentType
import com.amplifyframework.predictions.models.Syntax
import com.amplifyframework.predictions.result.InterpretResult
import java.util.ArrayList
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking

/**
 * Predictions service for performing text interpretation.
 */
internal class AWSComprehendService(
    private val pluginConfiguration: AWSPredictionsPluginConfiguration,
    private val authCredentialsProvider: CredentialsProvider
) {
    val client: ComprehendClient = ComprehendClient {
        this.region = pluginConfiguration.defaultRegion
        this.credentialsProvider = authCredentialsProvider
    }

    private val executor = Executors.newCachedThreadPool()

    companion object {
        private const val PERCENT = 100
    }

    fun comprehend(
        text: String,
        onSuccess: Consumer<InterpretResult>,
        onError: Consumer<PredictionsException>
    ) {
        execute(
            {
                // First obtain the dominant language to begin analysis
                val dominantLanguage = fetchPredominantLanguage(text)
                val language = dominantLanguage.value

                // Actually analyze text in the context of dominant language
                val sentiment = fetchSentiment(text, language)
                val keyPhrases = fetchKeyPhrases(text, language)
                val entities = fetchEntities(text, language)
                val syntax = fetchSyntax(text, language)
                InterpretResult.builder()
                    .language(dominantLanguage)
                    .sentiment(sentiment)
                    .keyPhrases(keyPhrases)
                    .entities(entities)
                    .syntax(syntax)
                    .build()
            },
            { throwable ->
                PredictionsException(
                    "AWS Comprehend encountered an error while interpreting text.",
                    throwable,
                    "See attached exception for more details."
                )
            },
            onSuccess,
            onError
        )
    }

    @Throws(PredictionsException::class)
    private suspend fun fetchPredominantLanguage(text: String): Language {
        // Language is a required field for other detections.
        // Always fetch language regardless of what configuration says.
        isResourceConfigured(InterpretTextConfiguration.InterpretType.LANGUAGE)

        // Detect dominant language from given text via AWS Comprehend
        val result = try {
            client.detectDominantLanguage {
                this.text = text
            }
        } catch (exception: Exception) {
            throw PredictionsException(
                "AWS Comprehend encountered an error while detecting dominant language.",
                exception,
                "See attached exception for more details."
            )
        }

        // Find the most dominant language from the list
        var dominantLanguage: DominantLanguage? = null
        result.languages?.forEach { language ->
            val dominantLanguageScore = dominantLanguage?.score
            val currentLanguageScore = language.score
            if (dominantLanguage == null) {
                dominantLanguage = language
            } else if (dominantLanguageScore != null && currentLanguageScore != null) {
                dominantLanguage = if (currentLanguageScore > dominantLanguageScore) {
                    language
                } else {
                    dominantLanguage
                }
            }
        }

        // Confirm that there was at least one detected language
        if (dominantLanguage == null) {
            throw PredictionsException(
                "AWS Comprehend did not detect any dominant language.",
                "Please verify the integrity of text being analyzed."
            )
        }
        val languageCode = dominantLanguage!!.languageCode
        val language = LanguageType.from(languageCode)
        val score = dominantLanguage!!.score
        val languageBuilder = Language.builder()
            .value(language)
        if (score != null) {
            languageBuilder.confidence(score * PERCENT)
        }
        return languageBuilder.build()
    }

    @Throws(PredictionsException::class)
    private suspend fun fetchSentiment(text: String, language: LanguageType): Sentiment? {
        // Skip if configuration specifies NOT sentiment
        if (!isResourceConfigured(InterpretTextConfiguration.InterpretType.SENTIMENT)) {
            return null
        }
        // Detect sentiment from given text via AWS Comprehend
        val result = try {
            client.detectSentiment {
                this.text = text
                this.languageCode = LanguageCode.fromValue(language.languageCode)
            }
        } catch (exception: Exception) {
            throw PredictionsException(
                "AWS Comprehend encountered an error while detecting sentiment.",
                exception,
                "See attached exception for more details."
            )
        }

        // Convert AWS Comprehend's detection result to Amplify-compatible format
        val comprehendSentiment = result.sentiment
        val sentimentScore = result.sentimentScore
        val predominantSentiment = SentimentTypeAdapter.fromComprehend(comprehendSentiment.toString())
        val score = when (predominantSentiment) {
            SentimentType.POSITIVE -> sentimentScore?.positive
            SentimentType.NEGATIVE -> sentimentScore?.negative
            SentimentType.NEUTRAL -> sentimentScore?.neutral
            SentimentType.MIXED -> sentimentScore?.mixed
            else -> 0f
        }
        if (score != null) {
            return Sentiment.builder()
                .value(predominantSentiment)
                .confidence(score * PERCENT)
                .build()
        }
        return null
    }

    @Throws(PredictionsException::class)
    private suspend fun fetchKeyPhrases(text: String, language: LanguageType): List<KeyPhrase>? {
        // Skip if configuration specifies NOT key phrase
        if (!isResourceConfigured(InterpretTextConfiguration.InterpretType.KEY_PHRASES)) {
            return null
        }
        // Detect key phrases from given text via AWS Comprehend
        val result = try {
            client.detectKeyPhrases {
                this.text = text
                this.languageCode = LanguageCode.fromValue(language.languageCode)
            }
        } catch (exception: Exception) {
            throw PredictionsException(
                "AWS Comprehend encountered an error while detecting key phrases.",
                exception,
                "See attached exception for more details."
            )
        }

        // Convert AWS Comprehend's detection result to Amplify-compatible format
        val keyPhrases: MutableList<KeyPhrase> = ArrayList()
        result.keyPhrases?.forEach { comprehendKeyPhrase ->
            val keyPhraseText = comprehendKeyPhrase.text
            val keyPhraseScore = comprehendKeyPhrase.score
            val keyPhraseOffset = comprehendKeyPhrase.beginOffset
            if (keyPhraseText != null && keyPhraseScore != null && keyPhraseOffset != null) {
                val amplifyKeyPhrase = KeyPhrase.builder()
                    .value(keyPhraseText)
                    .confidence(keyPhraseScore * PERCENT)
                    .targetText(keyPhraseText)
                    .startIndex(keyPhraseOffset)
                    .build()
                keyPhrases.add(amplifyKeyPhrase)
            }
        }
        return keyPhrases
    }

    @Throws(PredictionsException::class)
    private suspend fun fetchEntities(text: String, language: LanguageType): List<Entity>? {
        // Skip if configuration specifies NOT entities
        if (!isResourceConfigured(InterpretTextConfiguration.InterpretType.ENTITIES)) {
            return null
        }
        // Detect entities from given text via AWS Comprehend
        val result = try {
            client.detectEntities {
                this.text = text
                this.languageCode = LanguageCode.fromValue(language.languageCode)
            }
        } catch (exception: Exception) {
            throw PredictionsException(
                "AWS Comprehend encountered an error while detecting entities.",
                exception,
                "See attached exception for more details."
            )
        }

        // Convert AWS Comprehend's detection result to Amplify-compatible format
        val entities: MutableList<Entity> = ArrayList()
        result.entities?.forEach { comprehendEntity ->
            val entityType: EntityType =
                EntityTypeAdapter.fromComprehend(comprehendEntity.type.toString())
            val entityScore = comprehendEntity.score
            val entityText = comprehendEntity.text
            val entityOffset = comprehendEntity.beginOffset
            if (entityScore != null && entityText != null && entityOffset != null) {
                val amplifyEntity = Entity.builder()
                    .value(entityType)
                    .confidence(entityScore * PERCENT)
                    .targetText(entityText)
                    .startIndex(entityOffset)
                    .build()
                entities.add(amplifyEntity)
            }
        }
        return entities
    }

    @Throws(PredictionsException::class)
    private suspend fun fetchSyntax(text: String, language: LanguageType): List<Syntax>? {
        // Skip if configuration specifies NOT syntax
        if (!isResourceConfigured(InterpretTextConfiguration.InterpretType.SYNTAX)) {
            return null
        }
        // Detect syntax from given text via AWS Comprehend
        val result = try {
            client.detectSyntax {
                this.text = text
                this.languageCode = SyntaxLanguageCode.fromValue(language.languageCode)
            }
        } catch (exception: Exception) {
            throw PredictionsException(
                "AWS Comprehend encountered an error while detecting syntax.",
                exception,
                "See attached exception for more details."
            )
        }

        // Convert AWS Comprehend's detection result to Amplify-compatible format
        val syntaxTokens: MutableList<Syntax> = ArrayList()
        result.syntaxTokens?.forEach { comprehendSyntax ->
            val partOfSpeech = SpeechTypeAdapter.fromComprehend(comprehendSyntax.partOfSpeech?.tag.toString())
            val partOfSpeechScore = comprehendSyntax.partOfSpeech?.score
            val syntaxText = comprehendSyntax.text
            val syntaxOffset = comprehendSyntax.beginOffset
            if (partOfSpeechScore != null && syntaxText != null && syntaxOffset != null) {
                val amplifySyntax = Syntax.builder()
                    .id(comprehendSyntax.tokenId.toString())
                    .value(partOfSpeech)
                    .confidence(partOfSpeechScore * PERCENT)
                    .targetText(syntaxText)
                    .startIndex(syntaxOffset)
                    .build()
                syntaxTokens.add(amplifySyntax)
            }
        }
        return syntaxTokens
    }

    private fun isResourceConfigured(type: InterpretTextConfiguration.InterpretType): Boolean {
        // Check if text interpretation is configured
        val configuredType: InterpretTextConfiguration.InterpretType =
            pluginConfiguration.interpretTextConfiguration.type
        return if (InterpretTextConfiguration.InterpretType.ALL == configuredType) {
            // ALL catches every type
            true
        } else {
            // Otherwise check to see if they are equal
            configuredType == type
        }
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
                val predictionsException = if (error is PredictionsException) {
                    error
                } else {
                    errorTransformer.invoke(error)
                }
                onError.accept(predictionsException)
            }
        }
    }
}
