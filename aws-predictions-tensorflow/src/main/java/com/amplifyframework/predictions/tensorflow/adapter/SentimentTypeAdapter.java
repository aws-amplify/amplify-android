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

package com.amplifyframework.predictions.tensorflow.adapter;

import androidx.annotation.NonNull;

import com.amplifyframework.predictions.models.SentimentType;

import java.util.Locale;

/**
 * Utility to convert third-party {@link SentimentType} equivalent
 * into Amplify-compatible data structure.
 */
public final class SentimentTypeAdapter {
    @SuppressWarnings("checkstyle:all") private SentimentTypeAdapter() {}

    /**
     * Converts the sentiment string returned by TensorFlow Lite
     * Interpreter into a format supported by Amplify Predictions.
     * @param sentiment Sentiment type returned by AWS Comprehend
     * @return Amplify's {@link SentimentType} enum
     */
    @NonNull
    public static SentimentType fromTensorFlow(@NonNull String sentiment) {
        switch (sentiment.toLowerCase(Locale.US)) {
            case "positive":
                return SentimentType.POSITIVE;
            case "negative":
                return SentimentType.NEGATIVE;
            default:
                return SentimentType.UNKNOWN;
        }
    }
}
