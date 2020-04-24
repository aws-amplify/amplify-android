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
import com.amplifyframework.predictions.models.LabelType;
import com.amplifyframework.predictions.models.LanguageType;
import com.amplifyframework.predictions.result.IdentifyResult;
import com.amplifyframework.predictions.result.InterpretResult;
import com.amplifyframework.predictions.result.TranslateTextResult;

import com.amazonaws.services.comprehend.AmazonComprehendClient;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.translate.AmazonTranslateClient;

/**
 * Predictions service that makes inferences via AWS cloud computing.
 */
public final class AWSPredictionsService {

    private final AWSTranslateService translateService;
    private final AWSRekognitionService rekognitionService;
    private final AWSComprehendService comprehendService;

    /**
     * Constructs an instance of {@link AWSPredictionsService}.
     * @param configuration the configuration for AWS Predictions Plugin
     */
    public AWSPredictionsService(@NonNull AWSPredictionsPluginConfiguration configuration) {
        this.translateService = new AWSTranslateService(configuration);
        this.rekognitionService = new AWSRekognitionService(configuration);
        this.comprehendService = new AWSComprehendService(configuration);
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
     * @param image the Rekognition input image
     * @param onSuccess triggered upon successful result
     * @param onError triggered upon encountering error
     */
    public void detectLabels(
            @NonNull LabelType type,
            @NonNull Image image,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        rekognitionService.detectLabels(type, image, onSuccess, onError);
    }

    /**
     * Delegate to {@link AWSRekognitionService} to recognize celebrities.
     * @param image the Rekognition input image
     * @param onSuccess triggered upon successful result
     * @param onError triggered upon encountering error
     */
    public void recognizeCelebrities(
            @NonNull Image image,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        rekognitionService.recognizeCelebrities(image, onSuccess, onError);
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
     * Return configured Amazon Translate client for
     * direct access to AWS endpoint.
     * @return the configured Amazon Translate client
     */
    @NonNull
    public AmazonTranslateClient getTranslateClient() {
        return translateService.getClient();
    }

    /**
     * Return configured Amazon Rekognition client for
     * direct access to AWS endpoint.
     * @return the configured Amazon Rekognition client
     */
    @NonNull
    public AmazonRekognitionClient getRekognitionClient() {
        return rekognitionService.getClient();
    }

    /**
     * Return configured Amazon Comprehend client for
     * direct access to AWS endpoint.
     * @return the configured Amazon Comprehend client
     */
    @NonNull
    public AmazonComprehendClient getComprehendClient() {
        return comprehendService.getClient();
    }
}
