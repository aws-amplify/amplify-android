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

import com.amplifyframework.core.async.Result;
import com.amplifyframework.predictions.models.EntityDetectionResult;
import com.amplifyframework.predictions.models.KeyPhrase;
import com.amplifyframework.predictions.models.LanguageDetectionResult;
import com.amplifyframework.predictions.models.Sentiment;
import com.amplifyframework.predictions.models.SyntaxToken;

import java.util.List;

/**
 * The result of the call to interpret text.
 */
public final class InterpretResult implements Result {
    private final List<KeyPhrase> keyPhrases;
    private final Sentiment sentiment;
    private final List<EntityDetectionResult> entities;
    private final LanguageDetectionResult language;
    private final List<SyntaxToken> syntax;

    private InterpretResult(List<KeyPhrase> keyPhrases,
                            Sentiment sentiment,
                            List<EntityDetectionResult> entities,
                            LanguageDetectionResult language,
                            List<SyntaxToken> syntax) {
        this.keyPhrases = keyPhrases;
        this.sentiment = sentiment;
        this.entities = entities;
        this.language = language;
        this.syntax = syntax;
    }

    /**
     * Gets the associated key phrases.
     * @return The list of key phrases
     */
    @Nullable
    public List<KeyPhrase> getKeyPhrases() {
        return keyPhrases;
    }

    /**
     * Gets the associated sentiment.
     * @return The sentiment
     */
    @Nullable
    public Sentiment getSentiment() {
        return sentiment;
    }

    /**
     * Gets the associated entities.
     * @return The list of entities
     */
    @Nullable
    public List<EntityDetectionResult> getEntities() {
        return entities;
    }

    /**
     * Gets the associated language.
     * @return The detected language of text
     */
    @Nullable
    public LanguageDetectionResult getLanguage() {
        return language;
    }

    /**
     * Gets the associated syntax.
     * @return The list of syntax tokens
     */
    @Nullable
    public List<SyntaxToken> getSyntax() {
        return syntax;
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
    public static class Builder {
        private List<KeyPhrase> keyPhrases;
        private Sentiment sentiment;
        private List<EntityDetectionResult> entities;
        private LanguageDetectionResult language;
        private List<SyntaxToken> syntax;

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
         * Sets the list of entities and return this builder.
         * @param entities list of detected entities
         * @return this builder instance
         */
        @NonNull
        public Builder entities(@Nullable List<EntityDetectionResult> entities) {
            this.entities = entities;
            return this;
        }

        /**
         * Sets the language and return this builder.
         * @param language detected language of text
         * @return  this builder instance
         */
        @NonNull
        public Builder language(@Nullable LanguageDetectionResult language) {
            this.language = language;
            return this;
        }

        /**
         * Sets the list of syntax tokens and return this builder.
         * @param syntax list of syntax tokens
         * @return this builder instance
         */
        @NonNull
        public Builder syntax(@Nullable List<SyntaxToken> syntax) {
            this.syntax = syntax;
            return this;
        }

        /**
         * Construct a result instance with the properties of this
         * builder instance.
         * @return An instance of {@link InterpretResult}
         */
        @NonNull
        public InterpretResult build() {
            return new InterpretResult(
                    keyPhrases,
                    sentiment,
                    entities,
                    language,
                    syntax
            );
        }
    }
}
