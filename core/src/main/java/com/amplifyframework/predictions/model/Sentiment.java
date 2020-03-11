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

package com.amplifyframework.predictions.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * Class that holds the sentiment detection results for a
 * string of text for the predictions category.
 */
public final class Sentiment {
    private final SentimentType predominantSentiment;
    private final Map<SentimentType, Float> sentimentScores;

    private Sentiment(
            @NonNull SentimentType predominantSentiment,
            @Nullable Map<SentimentType, Float> sentimentScores
    ) {
        this.predominantSentiment = predominantSentiment;
        this.sentimentScores = sentimentScores;
    }

    /**
     * Gets the detected predominant sentiment of the text.
     * @return the predominant sentiment type
     */
    @NonNull
    public SentimentType getPredominantSentiment() {
        return predominantSentiment;
    }

    /**
     * Gets a map of associated sentiments and their confidence
     * scores.
     * @return the map of associated sentiments and scores
     */
    @Nullable
    public Map<SentimentType, Float> getSentimentScores() {
        return sentimentScores;
    }

    /**
     * Gets the builder to easily construct an instance of
     * sentiment object.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Sentiment}.
     */
    public static class Builder {
        private SentimentType predominantSentiment;
        private Map<SentimentType, Float> sentimentScores;

        /**
         * Sets the associated sentiment type and return this builder.
         * @param predominantSentiment the predominant sentiment type
         * @return this builder instance
         */
        @NonNull
        public Builder predominantSentiment(@NonNull SentimentType predominantSentiment) {
            this.predominantSentiment = Objects.requireNonNull(predominantSentiment);
            return this;
        }

        /**
         * Sets the map of sentiment types and their scores and return
         * this builder.
         * @param sentimentScores the sentiment score map
         * @return this builder instance
         */
        @NonNull
        public Builder sentimentScores(@Nullable Map<SentimentType, Float> sentimentScores) {
            this.sentimentScores = sentimentScores;
            return this;
        }

        /**
         * Constructs a new instance of {@link Sentiment} from
         * the values assigned to this builder.
         * @return An instance of {@link Sentiment}
         */
        @NonNull
        public Sentiment build() {
            return new Sentiment(
                    Objects.requireNonNull(predominantSentiment),
                    sentimentScores
            );
        }
    }
}
