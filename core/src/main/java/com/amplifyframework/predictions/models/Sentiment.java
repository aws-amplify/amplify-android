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

package com.amplifyframework.predictions.models;

import androidx.annotation.NonNull;

/**
 * Class that holds the sentiment detection results
 * for the predictions category.
 */
public final class Sentiment extends Attribute<SentimentType> {
    /**
     * Attribute type for {@link SentimentType}.
     */
    public static final String ATTRIBUTE_TYPE = SentimentType.class.getSimpleName();

    private Sentiment(final Builder builder) {
        super(builder);
    }

    @Override
    @NonNull
    public String getType() {
        return ATTRIBUTE_TYPE;
    }

    /**
     * Gets a builder to construct an Sentiments attribute.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Sentiment}.
     */
    public static final class Builder extends Attribute.Builder<Builder, Sentiment, SentimentType> {
        @Override
        @NonNull
        public Sentiment build() {
            return new Sentiment(this);
        }
    }
}
