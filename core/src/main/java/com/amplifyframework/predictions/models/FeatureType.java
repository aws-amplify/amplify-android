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

import androidx.annotation.Nullable;

/**
 * The list of features that are currently supported by
 * AWS Amplify. A feature can be detected from text or
 * image analysis.
 */
public enum FeatureType {
    /**
     * Entity is categorization of a specific phrase
     * detected from the input text.
     */
    ENTITY(EntityType.class.getSimpleName()),

    /**
     * Key phrase is a compound noun phrase detected
     * from the input text.
     */
    KEY_PHRASE("KeyPhrase"),

    /**
     * Language is the dominant language that is
     * detected from the input text.
     */
    LANGUAGE(LanguageType.class.getSimpleName()),

    /**
     * Sentiment is one's opinion or attitude detected
     * from the input text.
     * This feature is an output of text interpret.
     */
    SENTIMENT(SentimentType.class.getSimpleName()),

    /**
     * Syntax is the part of speech that is detected
     * from the input text.
     */
    SYNTAX(SpeechType.class.getSimpleName());

    private final String featureName;

    FeatureType(String featureName) {
        this.featureName = featureName;
    }

    /**
     * Gets the name of the feature type represented
     * by a given feature.
     * @return the name of the feature type
     */
    public String getFeatureName() {
        return featureName;
    }

    /**
     * Gets a feature type enum that represents a feature
     * with the provided name.
     * @param featureName the name of the feature
     * @return Enum representing a feature,
     *          null if no match is found.
     */
    @Nullable
    public static FeatureType fromName(String featureName) {
        try {
            return FeatureType.valueOf(featureName);
        } catch (IllegalArgumentException noMatchError) {
            return null;
        }
    }
}
