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

package com.amplifyframework.predictions.aws.service;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.aws.AWSPredictionsPluginConfiguration;
import com.amplifyframework.predictions.aws.models.AWSVoiceType;
import com.amplifyframework.predictions.models.IdentifyAction;
import com.amplifyframework.predictions.models.LabelType;
import com.amplifyframework.predictions.models.LanguageType;
import com.amplifyframework.predictions.models.TextFormatType;
import com.amplifyframework.predictions.result.IdentifyResult;
import com.amplifyframework.predictions.result.InterpretResult;
import com.amplifyframework.predictions.result.TextToSpeechResult;
import com.amplifyframework.predictions.result.TranslateTextResult;

import aws.sdk.kotlin.services.comprehend.ComprehendClient;

import java.nio.ByteBuffer;

import aws.sdk.kotlin.services.polly.PollyClient;
import aws.sdk.kotlin.services.rekognition.RekognitionClient;
import aws.sdk.kotlin.services.textract.TextractClient;
import aws.sdk.kotlin.services.translate.TranslateClient;
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider;

/**
 * Predictions service that makes inferences via AWS cloud computing.
 */
public final class AWSPredictionsService {

    private final AWSPredictionsPluginConfiguration configuration;
    private final AWSPollyService pollyService;
    private final AWSTranslateService translateService;
    private final AWSRekognitionService rekognitionService;
    private final AWSTextractService textractService;
    private final AWSComprehendService comprehendService;

    /**
     * Constructs an instance of {@link AWSPredictionsService}.
     * @param configuration the configuration for AWS Predictions Plugin
     * @param credentialsProvider An instance of an AWSCredentialsProvider implementation to vend auth credentials
     */
    public AWSPredictionsService(
            @NonNull AWSPredictionsPluginConfiguration configuration,
            @NonNull CredentialsProvider credentialsProvider) {
        this.configuration = configuration;
        this.pollyService = new AWSPollyService(configuration, credentialsProvider);
        this.translateService = new AWSTranslateService(configuration, credentialsProvider);
        this.rekognitionService = new AWSRekognitionService(configuration, credentialsProvider);
        this.textractService = new AWSTextractService(configuration, credentialsProvider);
        this.comprehendService = new AWSComprehendService(configuration, credentialsProvider);
    }

    /**
     * Delegate to {@link AWSPollyService} to synthesize speech.
     * @param text the input text to convert to speech
     * @param voiceType the voice type to synthesize speech with
     * @param onSuccess triggered upon successful result
     * @param onError triggered upon encountering error
     */
    public void synthesizeSpeech(
            @NonNull String text,
            @NonNull AWSVoiceType voiceType,
            @NonNull Consumer<TextToSpeechResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        pollyService.synthesizeSpeech(text, voiceType, onSuccess, onError);
    }

    /**
     * Delegate to {@link AWSTranslateService} to translate text.
     * @param text the input text to translate
     * @param sourceLanguage the language to translate from.
     *                       Use configuration default if null
     * @param targetLanguage the language to translate to.
     *                       Use configuration default if null
     * @param onSuccess triggered upon successful result
     * @param onError triggered upon encountering error
     */
    public void translate(
            @NonNull String text,
            @NonNull LanguageType sourceLanguage,
            @NonNull LanguageType targetLanguage,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        translateService.translate(text, sourceLanguage, targetLanguage, onSuccess, onError);
    }

    /**
     * Delegate to {@link AWSRekognitionService} to detect labels.
     * @param type the type of labels to detect
     * @param imageData the image data
     * @param onSuccess triggered upon successful result
     * @param onError triggered upon encountering error
     */
    public void detectLabels(
            @NonNull IdentifyAction type,
            @NonNull ByteBuffer imageData,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        final LabelType labelType;
        try {
            labelType = getLabelType(type);
        } catch (PredictionsException error) {
            onError.accept(error);
            return;
        }
        rekognitionService.detectLabels(labelType, imageData, onSuccess, onError);
    }

    /**
     * Delegate to {@link AWSRekognitionService} to recognize celebrities.
     * @param imageData the image data
     * @param onSuccess triggered upon successful result
     * @param onError triggered upon encountering error
     */
    public void recognizeCelebrities(
            @NonNull ByteBuffer imageData,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        rekognitionService.recognizeCelebrities(imageData, onSuccess, onError);
    }

    /**
     * Delegate to {@link AWSRekognitionService} to detect entities.
     * @param imageData the image data
     * @param onSuccess triggered upon successful result
     * @param onError triggered upon encountering error
     */
    public void detectEntities(
            @NonNull ByteBuffer imageData,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        rekognitionService.detectEntities(imageData, onSuccess, onError);
    }

    /**
     * Delegate to {@link AWSRekognitionService} to detect plain text
     * or to {@link AWSTextractService} to detect document text.
     * @param type the type of text format to detect
     * @param imageData the image data
     * @param onSuccess triggered upon successful result
     * @param onError triggered upon encountering error
     */
    public void detectText(
            @NonNull IdentifyAction type,
            @NonNull ByteBuffer imageData,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        final TextFormatType textType;
        try {
            textType = getTextFormatType(type);
        } catch (PredictionsException error) {
            onError.accept(error);
            return;
        }

        if (TextFormatType.PLAIN.equals(textType)) {
            // Delegate to Amazon Rekognition for plain text detection
            rekognitionService.detectPlainText(imageData, onSuccess, onError);
        } else {
            // Delegate to Amazon Textract for document text detection
            textractService.detectDocumentText(textType, imageData, onSuccess, onError);
        }
    }

    /**
     * Delegate to {@link AWSComprehendService} to make text interpretation.
     * @param text the input text to interpret
     * @param onSuccess triggered upon successful result
     * @param onError triggered upon encountering error
     */
    public void comprehend(
            @NonNull String text,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        comprehendService.comprehend(text, onSuccess, onError);
    }

    /**
     * If {@link IdentifyAction} is an instance of {@link LabelType} and
     * cast if true. Otherwise check configuration for default action type.
     * Throw if label detection is not configured.
     */
    private LabelType getLabelType(IdentifyAction actionType) throws PredictionsException {
        try {
            return (LabelType) actionType;
        } catch (ClassCastException error) {
            return configuration.getIdentifyLabelsConfiguration().getType();
        }
    }

    /**
     * If {@link IdentifyAction} is an instance of {@link TextFormatType} and
     * cast if true. Otherwise check configuration for default action type.
     * Throw if text detection is not configured.
     */
    private TextFormatType getTextFormatType(IdentifyAction actionType) throws PredictionsException {
        try {
            return (TextFormatType) actionType;
        } catch (ClassCastException error) {
            return configuration.getIdentifyTextConfiguration().getFormat();
        }
    }

    /**
     * Return configured Amazon Translate client for
     * direct access to AWS endpoint.
     * @return the configured Amazon Translate client
     */
    @NonNull
    public TranslateClient getTranslateClient() {
        return translateService.getClient();
    }

    /**
     * Return configured Amazon Polly client for
     * direct access to AWS endpoint.
     * @return the configured Amazon Polly client
     */
    @NonNull
    public PollyClient getPollyClient() {
        return pollyService.getClient();
    }

    /**
     * Return configured Amazon Rekognition client for
     * direct access to AWS endpoint.
     * @return the configured Amazon Rekognition client
     */
    @NonNull
    public RekognitionClient getRekognitionClient() {
        return rekognitionService.getClient();
    }

    /**
     * Return configured Amazon Textract client for
     * direct access to AWS endpoint.
     * @return the configured Amazon Textract client
     */
    @NonNull
    public TextractClient getTextractClient() {
        return textractService.getClient();
    }

    /**
     * Return configured Amazon Comprehend client for
     * direct access to AWS endpoint.
     * @return the configured Amazon Comprehend client
     */
    @NonNull
    public ComprehendClient getComprehendClient() {
        return comprehendService.getClient();
    }
}
