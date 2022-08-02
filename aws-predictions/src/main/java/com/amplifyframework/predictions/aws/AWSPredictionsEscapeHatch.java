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

package com.amplifyframework.predictions.aws;

import androidx.annotation.NonNull;

import aws.sdk.kotlin.services.comprehend.ComprehendClient;

import java.util.Objects;

import aws.sdk.kotlin.services.polly.PollyClient;
import aws.sdk.kotlin.services.rekognition.RekognitionClient;
import aws.sdk.kotlin.services.textract.TextractClient;
import aws.sdk.kotlin.services.translate.TranslateClient;

/**
 * An escape hatch for Predictions, which gives direct access to
 * the AWS service APIs. Use this to perform low-level operations
 * that are not exposed by Amplify, directly.
 */
public final class AWSPredictionsEscapeHatch {
    private final TranslateClient translate;
    private final PollyClient polly;
    private final RekognitionClient rekognition;
    private final TextractClient textract;
    private final ComprehendClient comprehend;

    AWSPredictionsEscapeHatch(
            @NonNull TranslateClient translate,
            @NonNull PollyClient polly,
            @NonNull RekognitionClient rekognition,
            @NonNull TextractClient textract,
            @NonNull ComprehendClient comprehend
    ) {
        this.translate = Objects.requireNonNull(translate);
        this.polly = Objects.requireNonNull(polly);
        this.rekognition = Objects.requireNonNull(rekognition);
        this.textract = Objects.requireNonNull(textract);
        this.comprehend = Objects.requireNonNull(comprehend);
    }

    /**
     * Return configured Amazon Translate client to access
     * low-level methods for text translation.
     * @return the configured Amazon Translate client
     */
    @NonNull
    public TranslateClient getTranslateClient() {
        return translate;
    }

    /**
     * Return configured Amazon Polly client to access
     * low-level methods for speech synthesis.
     * @return the configured Amazon Polly client
     */
    @NonNull
    public PollyClient getPollyClient() {
        return polly;
    }

    /**
     * Return configured Amazon Rekognition client to access
     * low-level methods for image analysis.
     * @return the configured Amazon Rekognition client
     */
    @NonNull
    public RekognitionClient getRekognitionClient() {
        return rekognition;
    }

    /**
     * Return configured Amazon Textract client to access
     * low-level methods for document analysis.
     * @return the configured Amazon Textract client
     */
    @NonNull
    public TextractClient getTextractClient() {
        return textract;
    }

    /**
     * Return configured Amazon Comprehend client to access
     * low-level methods for text interpretation.
     * @return the configured Amazon Comprehend client
     */
    @NonNull
    public ComprehendClient getComprehendClient() {
        return comprehend;
    }
}
