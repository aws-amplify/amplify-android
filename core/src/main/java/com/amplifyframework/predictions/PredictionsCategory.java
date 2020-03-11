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

package com.amplifyframework.predictions;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.predictions.models.IdentifyAction;
import com.amplifyframework.predictions.models.LanguageType;
import com.amplifyframework.predictions.operation.PredictionsIdentifyOperation;
import com.amplifyframework.predictions.operation.PredictionsInterpretOperation;
import com.amplifyframework.predictions.operation.PredictionsSpeechToTextOperation;
import com.amplifyframework.predictions.operation.PredictionsTextToSpeechOperation;
import com.amplifyframework.predictions.operation.PredictionsTranslateTextOperation;
import com.amplifyframework.predictions.options.PredictionsIdentifyOptions;
import com.amplifyframework.predictions.options.PredictionsInterpretOptions;
import com.amplifyframework.predictions.options.PredictionsSpeechToTextOptions;
import com.amplifyframework.predictions.options.PredictionsTextToSpeechOptions;
import com.amplifyframework.predictions.options.PredictionsTranslateTextOptions;
import com.amplifyframework.predictions.result.IdentifyResult;
import com.amplifyframework.predictions.result.InterpretResult;
import com.amplifyframework.predictions.result.SpeechToTextResult;
import com.amplifyframework.predictions.result.TextToSpeechResult;
import com.amplifyframework.predictions.result.TranslateTextResult;

import java.net.URL;

/**
 * Defines the Client API consumed by the application.
 * Internally routes the calls to the Predictions Category
 * plugins registered.
 */
public final class PredictionsCategory extends Category<PredictionsPlugin<?>> implements PredictionsCategoryBehavior {
    @NonNull
    @Override
    public CategoryType getCategoryType() {
        return CategoryType.PREDICTIONS;
    }

    @NonNull
    @Override
    public PredictionsTranslateTextOperation<?> translateText(
            @NonNull String text,
            @NonNull LanguageType language,
            @NonNull LanguageType targetLanguage,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().translateText(text, language, targetLanguage, onSuccess, onError);
    }

    @NonNull
    @Override
    public PredictionsTranslateTextOperation<?> translateText(
            @NonNull String text,
            @NonNull LanguageType language,
            @NonNull LanguageType targetLanguage,
            @NonNull PredictionsTranslateTextOptions options,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().translateText(text, language, targetLanguage, options, onSuccess, onError);
    }

    @NonNull
    @Override
    public PredictionsTextToSpeechOperation<?> convertTextToSpeech(
            @NonNull String text,
            @NonNull Consumer<TextToSpeechResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError) {
        return getSelectedPlugin().convertTextToSpeech(text, onSuccess, onError);
    }

    @NonNull
    @Override
    public PredictionsTextToSpeechOperation<?> convertTextToSpeech(
            @NonNull String text,
            @NonNull PredictionsTextToSpeechOptions options,
            @NonNull Consumer<TextToSpeechResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().convertTextToSpeech(text, options, onSuccess, onError);
    }

    @NonNull
    @Override
    public PredictionsSpeechToTextOperation<?> convertSpeechToText(
            @NonNull URL speech,
            @NonNull Consumer<SpeechToTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().convertSpeechToText(speech, onSuccess, onError);
    }

    @NonNull
    @Override
    public PredictionsSpeechToTextOperation<?> convertSpeechToText(
            @NonNull URL speech,
            @NonNull PredictionsSpeechToTextOptions options,
            @NonNull Consumer<SpeechToTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().convertSpeechToText(speech, options, onSuccess, onError);
    }

    @NonNull
    @Override
    public PredictionsIdentifyOperation<?> identify(
            @NonNull IdentifyAction type,
            @NonNull URL image,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().identify(type, image, onSuccess, onError);
    }

    @NonNull
    @Override
    public PredictionsIdentifyOperation<?> identify(
            @NonNull IdentifyAction type,
            @NonNull URL image,
            @NonNull PredictionsIdentifyOptions options,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().identify(type, image, options, onSuccess, onError);
    }

    @NonNull
    @Override
    public PredictionsInterpretOperation<?> interpret(
            @NonNull String text,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().interpret(text, onSuccess, onError);
    }

    @NonNull
    @Override
    public PredictionsInterpretOperation<?> interpret(
            @NonNull String text,
            @NonNull PredictionsInterpretOptions options,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().interpret(text, options, onSuccess, onError);
    }
}
