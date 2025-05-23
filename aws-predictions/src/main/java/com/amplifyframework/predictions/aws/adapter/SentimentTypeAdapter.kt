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
package com.amplifyframework.predictions.aws.adapter

import com.amplifyframework.predictions.models.SentimentType

/**
 * Utility to convert AWS Comprehend's sentiment type
 * into Amplify-compatible data structure
 * (i.e. [SentimentType]).
 */
object SentimentTypeAdapter {
    /**
     * Converts the sentiment string returned by AWS Comprehend
     * into a format supported by Amplify Predictions.
     * @param sentiment Sentiment type returned by AWS Comprehend
     * @return Amplify's [SentimentType] enum
     */
    @JvmStatic
    fun fromComprehend(sentiment: String): SentimentType =
        when (aws.sdk.kotlin.services.comprehend.model.SentimentType.fromValue(sentiment)) {
            aws.sdk.kotlin.services.comprehend.model.SentimentType.Positive -> SentimentType.POSITIVE
            aws.sdk.kotlin.services.comprehend.model.SentimentType.Negative -> SentimentType.NEGATIVE
            aws.sdk.kotlin.services.comprehend.model.SentimentType.Neutral -> SentimentType.NEUTRAL
            aws.sdk.kotlin.services.comprehend.model.SentimentType.Mixed -> SentimentType.MIXED
            else -> SentimentType.UNKNOWN
        }
}
