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

import com.amplifyframework.predictions.models.LanguageType;

import java.util.Objects;

/**
 * The result of the call to translate text to another language.
 */
public final class TranslateTextResult {
    private final String translatedText;
    private final LanguageType targetLanguage;

    private TranslateTextResult(final Builder builder) {
        this.translatedText = builder.getTranslatedText();
        this.targetLanguage = builder.getTargetLanguage();
    }

    /**
     * Gets the translated text.
     * @return the translated text
     */
    @NonNull
    public String getTranslatedText() {
        return translatedText;
    }

    /**
     * Gets the language of translated text.
     * @return the language of translated text
     */
    @NonNull
    public LanguageType getTargetLanguage() {
        return targetLanguage;
    }

    /**
     * Return a new instance of builder to help easily construct
     * a new instance of result.
     * @return An unassigned instance of builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TranslateTextResult}.
     */
    public static final class Builder {
        private String translatedText;
        private LanguageType targetLanguage;

        /**
         * Sets the translated text and return this builder instance.
         * @param text The translated text
         * @return This builder instance
         */
        @NonNull
        public Builder translatedText(@NonNull String text) {
            this.translatedText = Objects.requireNonNull(text);
            return this;
        }

        /**
         * Sets the target language and return this builder instance.
         * @param language The target language
         * @return This builder instance
         */
        @NonNull
        public Builder targetLanguage(@NonNull LanguageType language) {
            this.targetLanguage = Objects.requireNonNull(language);
            return this;
        }

        /**
         * Return a result object containing translated text and
         * its corresponding language.
         * Throw if any of the properties is null.
         * @return An instance of {@link TranslateTextResult}
         */
        @NonNull
        public TranslateTextResult build() {
            return new TranslateTextResult(this);
        }

        @NonNull
        String getTranslatedText() {
            return Objects.requireNonNull(translatedText);
        }

        @NonNull
        LanguageType getTargetLanguage() {
            return Objects.requireNonNull(targetLanguage);
        }
    }
}
