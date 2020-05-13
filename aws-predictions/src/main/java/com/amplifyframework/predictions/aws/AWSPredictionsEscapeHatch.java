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

package com.amplifyframework.predictions.aws;

import androidx.annotation.NonNull;

import com.amazonaws.services.comprehend.AmazonComprehendClient;
import com.amazonaws.services.polly.AmazonPollyClient;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.textract.AmazonTextractClient;
import com.amazonaws.services.translate.AmazonTranslateClient;

import java.util.Objects;

/**
 * An escape hatch for Predictions, which gives direct access to
 * the AWS service APIs. Use this to perform low-level operations
 * that are not exposed by Amplify, directly.
 */
public final class AWSPredictionsEscapeHatch {
    private final AmazonTranslateClient translate;
    private final AmazonPollyClient polly;
    private final AmazonRekognitionClient rekognition;
    private final AmazonTextractClient textract;
    private final AmazonComprehendClient comprehend;

    AWSPredictionsEscapeHatch(
            @NonNull AmazonTranslateClient translate,
            @NonNull AmazonPollyClient polly,
            @NonNull AmazonRekognitionClient rekognition,
            @NonNull AmazonTextractClient textract,
            @NonNull AmazonComprehendClient comprehend
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
    public AmazonTranslateClient getTranslateClient() {
        return translate;
    }

    /**
     * Return configured Amazon Polly client to access
     * low-level methods for speech synthesis.
     * @return the configured Amazon Polly client
     */
    @NonNull
    public AmazonPollyClient getPollyClient() {
        return polly;
    }

    /**
     * Return configured Amazon Rekognition client to access
     * low-level methods for image analysis.
     * @return the configured Amazon Rekognition client
     */
    @NonNull
    public AmazonRekognitionClient getRekognitionClient() {
        return rekognition;
    }

    /**
     * Return configured Amazon Textract client to access
     * low-level methods for document analysis.
     * @return the configured Amazon Textract client
     */
    @NonNull
    public AmazonTextractClient getTextractClient() {
        return textract;
    }

    /**
     * Return configured Amazon Comprehend client to access
     * low-level methods for text interpretation.
     * @return the configured Amazon Comprehend client
     */
    @NonNull
    public AmazonComprehendClient getComprehendClient() {
        return comprehend;
    }
}
