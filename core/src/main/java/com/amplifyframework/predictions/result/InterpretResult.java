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

package com.amplifyframework.predictions.result;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.predictions.models.Entity;
import com.amplifyframework.predictions.models.Feature;
import com.amplifyframework.predictions.models.KeyPhrase;
import com.amplifyframework.predictions.models.Language;
import com.amplifyframework.predictions.models.Sentiment;
import com.amplifyframework.predictions.models.Syntax;
import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The result of the call to interpret text.
 */
public final class InterpretResult {
    private final Language language;
    private final Sentiment sentiment;
    private final List<KeyPhrase> keyPhrases;
    private final List<Entity> entities;
    private final List<Syntax> syntax;
    private final List<Feature<?>> features;

    private InterpretResult(final Builder builder) {
        this.language = builder.getLanguage();
        this.sentiment = builder.getSentiment();
        this.keyPhrases = builder.getKeyPhrases();
        this.entities = builder.getEntities();
        this.syntax = builder.getSyntax();
        this.features = builder.getFeatures();
    }

    /**
     * Gets the associated language.
     * @return The detected language of the text
     */
    @Nullable
    public Language getLanguage() {
        return language;
    }

    /**
     * Gets the associated sentiment.
     * @return The predominant sentiment of the text
     */
    @Nullable
    public Sentiment getSentiment() {
        return sentiment;
    }

    /**
     * Gets the key phrases detected within the text.
     * @return The key phrases of the text
     */
    @Nullable
    public List<KeyPhrase> getKeyPhrases() {
        return Immutable.of(keyPhrases);
    }

    /**
     * Gets the entities detected within the text.
     * @return The entities of the text
     */
    @Nullable
    public List<Entity> getEntities() {
        return Immutable.of(entities);
    }

    /**
     * Gets the syntax from the text.
     * @return The text syntax
     */
    @Nullable
    public List<Syntax> getSyntax() {
        return Immutable.of(syntax);
    }

    /**
     * Gets other associated features.
     * @return The list of features
     */
    @NonNull
    public List<Feature<?>> getFeatures() {
        return Immutable.of(features);
    }

    /**
     * Obtain an unassigned builder object for the result.
     * @return An unassigned builder object
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class to help easily build an instance of
     * {@link InterpretResult} with specific properties.
     */
    public static final class Builder {
        private Language language;
        private Sentiment sentiment;
        private List<KeyPhrase> keyPhrases;
        private List<Entity> entities;
        private List<Syntax> syntax;
        private List<Feature<?>> features;

        private Builder() {
            this.features = new ArrayList<>();
        }

        /**
         * Sets the language and return this builder.
         * @param language detected language of text
         * @return  this builder instance
         */
        @NonNull
        public Builder language(@Nullable Language language) {
            this.language = language;
            return this;
        }

        /**
         * Sets the sentiment and return this builder.
         * @param sentiment associated sentiment
         * @return this builder instance
         */
        @NonNull
        public Builder sentiment(@Nullable Sentiment sentiment) {
            this.sentiment = sentiment;
            return this;
        }

        /**
         * Sets the list of key phrases and return this builder.
         * @param keyPhrases list of key phrases
         * @return this builder instance
         */
        @NonNull
        public Builder keyPhrases(@Nullable List<KeyPhrase> keyPhrases) {
            this.keyPhrases = keyPhrases;
            return this;
        }

        /**
         * Sets the list of entities and return this builder.
         * @param entities list of detected text entities
         * @return this builder instance
         */
        @NonNull
        public Builder entities(@Nullable List<Entity> entities) {
            this.entities = entities;
            return this;
        }

        /**
         * Sets the list of syntax and return this builder.
         * @param syntax list of syntax
         * @return this builder instance
         */
        @NonNull
        public Builder syntax(@Nullable List<Syntax> syntax) {
            this.syntax = syntax;
            return this;
        }

        /**
         * Sets the list of features and return this builder.
         * @param features all other features of the result
         * @return this builder instance
         */
        @NonNull
        public Builder features(@NonNull List<Feature<?>> features) {
            this.features = Objects.requireNonNull(features);
            return this;
        }

        /**
         * Add an feature and return this builder.
         * @param feature feature to add
         * @return this builder instance
         */
        @NonNull
        public Builder feature(@NonNull Feature<?> feature) {
            this.features.add(Objects.requireNonNull(feature));
            return this;
        }

        /**
         * Construct a result instance with the properties of this
         * builder instance.
         * @return An instance of {@link InterpretResult}
         */
        @NonNull
        public InterpretResult build() {
            return new InterpretResult(this);
        }

        @Nullable
        Language getLanguage() {
            return language;
        }

        @Nullable
        Sentiment getSentiment() {
            return sentiment;
        }

        @Nullable
        List<KeyPhrase> getKeyPhrases() {
            return keyPhrases;
        }

        @Nullable
        List<Entity> getEntities() {
            return entities;
        }

        @Nullable
        List<Syntax> getSyntax() {
            return syntax;
        }

        @NonNull
        List<Feature<?>> getFeatures() {
            return features;
        }
    }
}
