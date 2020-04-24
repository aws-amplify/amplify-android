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
     * Bounded key value is a key-value pair in the
     * format of a question and answer from the input
     * image document.
     */
    BOUNDED_KEY_VALUE("BoundedKeyValue"),

    /**
     * Celebrity is a famous person that is detected
     * from the input image.
     */
    CELEBRITY("Celebrity"),

    /**
     * Emotion is the inferred mood or state of the
     * entity's mind detected from the input image.
     */
    EMOTION(EmotionType.class.getSimpleName()),

    /**
     * Entity is categorization of a specific phrase
     * detected from the input text.
     */
    ENTITY(EntityType.class.getSimpleName()),

    /**
     * Entity match is an identification of a known
     * entity from the input image.
     */
    ENTITY_MATCH("EntityMatch"),

    /**
     * Gender is an entity's sexual identity detected
     * from the input image.
     */
    GENDER(GenderBinaryType.class.getSimpleName()),

    /**
     * Identified text is graphical text that is
     * detected from the input image.
     */
    IDENTIFIED_TEXT("IdentifiedText"),

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

    private final String typeAlias;

    FeatureType(String typeAlias) {
        this.typeAlias = typeAlias;
    }

    /**
     * Gets the name of the feature type represented
     * by a given feature.
     * @return the name of the feature type
     */
    public String getAlias() {
        return typeAlias;
    }

    /**
     * Gets a feature type enum that represents a feature
     * with the provided name.
     * @param typeAlias the alias of the feature type
     * @return Enum representing a feature,
     *          null if no match is found.
     */
    @Nullable
    public static FeatureType fromAlias(String typeAlias) {
        try {
            return FeatureType.valueOf(typeAlias);
        } catch (IllegalArgumentException noMatchError) {
            return null;
        }
    }
}

