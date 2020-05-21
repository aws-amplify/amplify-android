/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.predictions.aws.service;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.aws.AWSPredictionsPluginConfiguration;
import com.amplifyframework.predictions.aws.adapter.EntityTypeAdapter;
import com.amplifyframework.predictions.aws.adapter.SentimentTypeAdapter;
import com.amplifyframework.predictions.aws.adapter.SpeechTypeAdapter;
import com.amplifyframework.predictions.aws.configuration.InterpretTextConfiguration;
import com.amplifyframework.predictions.models.Entity;
import com.amplifyframework.predictions.models.EntityType;
import com.amplifyframework.predictions.models.KeyPhrase;
import com.amplifyframework.predictions.models.Language;
import com.amplifyframework.predictions.models.LanguageType;
import com.amplifyframework.predictions.models.Sentiment;
import com.amplifyframework.predictions.models.SentimentType;
import com.amplifyframework.predictions.models.SpeechType;
import com.amplifyframework.predictions.models.Syntax;
import com.amplifyframework.predictions.result.InterpretResult;
import com.amplifyframework.util.UserAgent;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.comprehend.AmazonComprehendClient;
import com.amazonaws.services.comprehend.model.DetectDominantLanguageRequest;
import com.amazonaws.services.comprehend.model.DetectDominantLanguageResult;
import com.amazonaws.services.comprehend.model.DetectEntitiesRequest;
import com.amazonaws.services.comprehend.model.DetectEntitiesResult;
import com.amazonaws.services.comprehend.model.DetectKeyPhrasesRequest;
import com.amazonaws.services.comprehend.model.DetectKeyPhrasesResult;
import com.amazonaws.services.comprehend.model.DetectSentimentRequest;
import com.amazonaws.services.comprehend.model.DetectSentimentResult;
import com.amazonaws.services.comprehend.model.DetectSyntaxRequest;
import com.amazonaws.services.comprehend.model.DetectSyntaxResult;
import com.amazonaws.services.comprehend.model.DominantLanguage;
import com.amazonaws.services.comprehend.model.PartOfSpeechTag;
import com.amazonaws.services.comprehend.model.SentimentScore;

import java.util.ArrayList;
import java.util.List;

/**
 * Predictions service for performing text interpretation.
 */
final class AWSComprehendService {
    private static final int PERCENT = 100;

    private final AmazonComprehendClient comprehend;
    private final AWSPredictionsPluginConfiguration pluginConfiguration;

    AWSComprehendService(
            @NonNull AWSPredictionsPluginConfiguration pluginConfiguration,
            @NonNull AWSCredentialsProvider credentialsProvider) {
        this.comprehend = createComprehendClient(credentialsProvider);
        this.pluginConfiguration = pluginConfiguration;
    }

    private AmazonComprehendClient createComprehendClient(@NonNull AWSCredentialsProvider credentialsProvider) {
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setUserAgent(UserAgent.string());
        return new AmazonComprehendClient(credentialsProvider, configuration);
    }

    void comprehend(
            @NonNull String text,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        try {
            // First obtain the dominant language to begin analysis
            final Language dominantLanguage = fetchPredominantLanguage(text);
            final LanguageType language = dominantLanguage.getValue();

            // Actually analyze text in the context of dominant language
            final Sentiment sentiment = fetchSentiment(text, language);
            final List<KeyPhrase> keyPhrases = fetchKeyPhrases(text, language);
            final List<Entity> entities = fetchEntities(text, language);
            final List<Syntax> syntax = fetchSyntax(text, language);

            onSuccess.accept(InterpretResult.builder()
                    .language(dominantLanguage)
                    .sentiment(sentiment)
                    .keyPhrases(keyPhrases)
                    .entities(entities)
                    .syntax(syntax)
                    .build());
        } catch (PredictionsException exception) {
            onError.accept(exception);
        }
    }

    private Language fetchPredominantLanguage(String text) throws PredictionsException {
        // Language is a required field for other detections.
        // Always fetch language regardless of what configuration says.
        isResourceConfigured(InterpretTextConfiguration.InterpretType.LANGUAGE);

        DetectDominantLanguageRequest request = new DetectDominantLanguageRequest()
                .withText(text);

        // Detect dominant language from given text via AWS Comprehend
        final DetectDominantLanguageResult result;
        try {
            result = comprehend.detectDominantLanguage(request);
        } catch (AmazonClientException serviceException) {
            throw new PredictionsException(
                    "AWS Comprehend encountered an error while detecting dominant language.",
                    serviceException,
                    "See attached service exception for more details."
            );
        }

        // Find the most dominant language from the list
        DominantLanguage dominantLanguage = null;
        for (DominantLanguage language : result.getLanguages()) {
            if (dominantLanguage == null
                    || language.getScore() > dominantLanguage.getScore()) {
                dominantLanguage = language;
            }
        }

        // Confirm that there was at least one detected language
        if (dominantLanguage == null) {
            throw new PredictionsException(
                    "AWS Comprehend did not detect any dominant language.",
                    "Please verify the integrity of text being analyzed."
            );
        }

        String languageCode = dominantLanguage.getLanguageCode();
        LanguageType language = LanguageType.from(languageCode);
        Float score = dominantLanguage.getScore();

        return Language.builder()
                .value(language)
                .confidence(score * PERCENT)
                .build();
    }

    private Sentiment fetchSentiment(String text, LanguageType language) throws PredictionsException {
        // Skip if configuration specifies NOT sentiment
        if (!isResourceConfigured(InterpretTextConfiguration.InterpretType.SENTIMENT)) {
            return null;
        }

        DetectSentimentRequest request = new DetectSentimentRequest()
                .withText(text)
                .withLanguageCode(language.getLanguageCode());

        // Detect sentiment from given text via AWS Comprehend
        final DetectSentimentResult result;
        try {
            result = comprehend.detectSentiment(request);
        } catch (AmazonClientException serviceException) {
            throw new PredictionsException(
                    "AWS Comprehend encountered an error while detecting sentiment.",
                    serviceException,
                    "See attached service exception for more details."
            );
        }

        // Convert AWS Comprehend's detection result to Amplify-compatible format
        String comprehendSentiment = result.getSentiment();
        SentimentScore sentimentScore = result.getSentimentScore();
        SentimentType predominantSentiment = SentimentTypeAdapter.fromComprehend(comprehendSentiment);
        final float score;
        switch (predominantSentiment) {
            case POSITIVE:
                score = sentimentScore.getPositive();
                break;
            case NEGATIVE:
                score = sentimentScore.getNegative();
                break;
            case NEUTRAL:
                score = sentimentScore.getNeutral();
                break;
            case MIXED:
                score = sentimentScore.getMixed();
                break;
            default:
                score = 0f;
        }

        return Sentiment.builder()
                .value(predominantSentiment)
                .confidence(score * PERCENT)
                .build();
    }

    private List<KeyPhrase> fetchKeyPhrases(String text, LanguageType language) throws PredictionsException {
        // Skip if configuration specifies NOT key phrase
        if (!isResourceConfigured(InterpretTextConfiguration.InterpretType.KEY_PHRASES)) {
            return null;
        }

        DetectKeyPhrasesRequest request = new DetectKeyPhrasesRequest()
                .withText(text)
                .withLanguageCode(language.getLanguageCode());

        // Detect key phrases from given text via AWS Comprehend
        final DetectKeyPhrasesResult result;
        try {
            result = comprehend.detectKeyPhrases(request);
        } catch (AmazonClientException serviceException) {
            throw new PredictionsException(
                    "AWS Comprehend encountered an error while detecting key phrases.",
                    serviceException,
                    "See attached service exception for more details."
            );
        }

        // Convert AWS Comprehend's detection result to Amplify-compatible format
        List<KeyPhrase> keyPhrases = new ArrayList<>();
        for (com.amazonaws.services.comprehend.model.KeyPhrase comprehendKeyPhrase : result.getKeyPhrases()) {
            KeyPhrase amplifyKeyPhrase = KeyPhrase.builder()
                    .value(comprehendKeyPhrase.getText())
                    .confidence(comprehendKeyPhrase.getScore() * PERCENT)
                    .targetText(comprehendKeyPhrase.getText())
                    .startIndex(comprehendKeyPhrase.getBeginOffset())
                    .build();
            keyPhrases.add(amplifyKeyPhrase);
        }

        return keyPhrases;
    }

    private List<Entity> fetchEntities(String text, LanguageType language) throws PredictionsException {
        // Skip if configuration specifies NOT entities
        if (!isResourceConfigured(InterpretTextConfiguration.InterpretType.ENTITIES)) {
            return null;
        }

        DetectEntitiesRequest request = new DetectEntitiesRequest()
                .withText(text)
                .withLanguageCode(language.getLanguageCode());

        // Detect entities from given text via AWS Comprehend
        final DetectEntitiesResult result;
        try {
            result = comprehend.detectEntities(request);
        } catch (AmazonClientException serviceException) {
            throw new PredictionsException(
                    "AWS Comprehend encountered an error while detecting entities.",
                    serviceException,
                    "See attached service exception for more details."
            );
        }

        // Convert AWS Comprehend's detection result to Amplify-compatible format
        List<Entity> entities = new ArrayList<>();
        for (com.amazonaws.services.comprehend.model.Entity comprehendEntity : result.getEntities()) {
            EntityType entityType = EntityTypeAdapter.fromComprehend(comprehendEntity.getType());
            Entity amplifyEntity = Entity.builder()
                    .value(entityType)
                    .confidence(comprehendEntity.getScore() * PERCENT)
                    .targetText(comprehendEntity.getText())
                    .startIndex(comprehendEntity.getBeginOffset())
                    .build();
            entities.add(amplifyEntity);
        }

        return entities;
    }

    private List<Syntax> fetchSyntax(String text, LanguageType language) throws PredictionsException {
        // Skip if configuration specifies NOT syntax
        if (!isResourceConfigured(InterpretTextConfiguration.InterpretType.SYNTAX)) {
            return null;
        }

        DetectSyntaxRequest request = new DetectSyntaxRequest()
                .withText(text)
                .withLanguageCode(language.getLanguageCode());

        // Detect syntax from given text via AWS Comprehend
        final DetectSyntaxResult result;
        try {
            result = comprehend.detectSyntax(request);
        } catch (AmazonClientException serviceException) {
            throw new PredictionsException(
                    "AWS Comprehend encountered an error while detecting syntax.",
                    serviceException,
                    "See attached service exception for more details."
            );
        }

        // Convert AWS Comprehend's detection result to Amplify-compatible format
        List<Syntax> syntaxTokens = new ArrayList<>();
        for (com.amazonaws.services.comprehend.model.SyntaxToken comprehendSyntax : result.getSyntaxTokens()) {
            PartOfSpeechTag comprehendPartOfSpeech = comprehendSyntax.getPartOfSpeech();
            SpeechType partOfSpeech = SpeechTypeAdapter.fromComprehend(comprehendPartOfSpeech.getTag());
            Syntax amplifySyntax = Syntax.builder()
                    .id(comprehendSyntax.getTokenId().toString())
                    .value(partOfSpeech)
                    .confidence(comprehendPartOfSpeech.getScore() * PERCENT)
                    .targetText(comprehendSyntax.getText())
                    .startIndex(comprehendSyntax.getBeginOffset())
                    .build();
            syntaxTokens.add(amplifySyntax);
        }

        return syntaxTokens;
    }

    private boolean isResourceConfigured(InterpretTextConfiguration.InterpretType type) throws PredictionsException {
        // Check if text interpretation is configured
        InterpretTextConfiguration.InterpretType configuredType =
                pluginConfiguration.getInterpretTextConfiguration().getType();

        if (InterpretTextConfiguration.InterpretType.ALL.equals(configuredType)) {
            // ALL catches every type
            return true;
        } else {
            // Otherwise check to see if they are equal
            return configuredType.equals(type);
        }
    }

    @NonNull
    AmazonComprehendClient getClient() {
        return comprehend;
    }
}
