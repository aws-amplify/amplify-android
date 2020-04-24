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

package com.amplifyframework.predictions.aws.adapter;

import androidx.annotation.NonNull;

import com.amplifyframework.predictions.models.SentimentType;

/**
 * Utility to convert AWS Comprehend's sentiment type
 * into Amplify-compatible data structure
 * (i.e. {@link SentimentType}).
 */
public final class SentimentTypeAdapter {
    private SentimentTypeAdapter() {}

    /**
     * Converts the sentiment string returned by AWS Comprehend
     * into a format supported by Amplify Predictions.
     * @param sentiment Sentiment type returned by AWS Comprehend
     * @return Amplify's {@link SentimentType} enum
     */
    @NonNull
    public static SentimentType fromComprehend(@NonNull String sentiment) {
        com.amazonaws.services.comprehend.model.SentimentType type =
                com.amazonaws.services.comprehend.model.SentimentType.fromValue(sentiment);
        switch (type) {
            case POSITIVE:
                return SentimentType.POSITIVE;
            case NEGATIVE:
                return SentimentType.NEGATIVE;
            case NEUTRAL:
                return SentimentType.NEUTRAL;
            case MIXED:
                return SentimentType.MIXED;
            default:
                return SentimentType.UNKNOWN;
        }
    }
}
