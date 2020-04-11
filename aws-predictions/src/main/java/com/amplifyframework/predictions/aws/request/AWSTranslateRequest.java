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

package com.amplifyframework.predictions.aws.request;

import androidx.annotation.NonNull;

import com.amplifyframework.predictions.models.LanguageType;

import java.util.Objects;

/**
 * Simple request instance for text translation operation.
 */
public final class AWSTranslateRequest {
    private final String text;
    private final LanguageType sourceLanguage;
    private final LanguageType targetLanguage;

    /**
     * Constructs an instance of {@link AWSTranslateRequest}.
     * @param text the text to translate
     * @param sourceLanguage the language to translate from
     * @param targetLanguage the language to translate to
     */
    public AWSTranslateRequest(
            @NonNull String text,
            @NonNull LanguageType sourceLanguage,
            @NonNull LanguageType targetLanguage
    ) {
        this.text = Objects.requireNonNull(text);
        this.sourceLanguage = Objects.requireNonNull(sourceLanguage);
        this.targetLanguage = Objects.requireNonNull(targetLanguage);
    }

    /**
     * Gets the text to translate.
     * @return the input text
     */
    @NonNull
    public String getText() {
        return text;
    }

    /**
     * Gets the language to translate from.
     * Null if relying on the configuration default.
     * @return the source language
     */
    @NonNull
    public LanguageType getSourceLanguage() {
        return sourceLanguage;
    }

    /**
     * Gets the language to translate to.
     * Null if relying on the configuration default.
     * @return the targetLanguage
     */
    @NonNull
    public LanguageType getTargetLanguage() {
        return targetLanguage;
    }
}
