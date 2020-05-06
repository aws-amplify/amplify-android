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

import android.graphics.Bitmap;
import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.predictions.models.IdentifyAction;
import com.amplifyframework.predictions.models.LanguageType;
import com.amplifyframework.predictions.operation.IdentifyOperation;
import com.amplifyframework.predictions.operation.InterpretOperation;
import com.amplifyframework.predictions.operation.SpeechToTextOperation;
import com.amplifyframework.predictions.operation.TranslateTextOperation;
import com.amplifyframework.predictions.options.IdentifyOptions;
import com.amplifyframework.predictions.options.InterpretOptions;
import com.amplifyframework.predictions.options.SpeechToTextOptions;
import com.amplifyframework.predictions.options.TranslateTextOptions;
import com.amplifyframework.predictions.result.IdentifyResult;
import com.amplifyframework.predictions.result.InterpretResult;
import com.amplifyframework.predictions.result.SpeechToTextResult;
import com.amplifyframework.predictions.result.TranslateTextResult;

import java.io.InputStream;

/**
 * Defines the API that a consuming application uses to perform predictions.
 * Internally routes calls to the registered plugins of the category.
 */
public final class PredictionsCategory extends Category<PredictionsPlugin<?>> implements PredictionsCategoryBehavior {
    @NonNull
    @Override
    public CategoryType getCategoryType() {
        return CategoryType.PREDICTIONS;
    }

    @NonNull
    @Override
    public TranslateTextOperation<?> translateText(
            @NonNull String text,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().translateText(text, onSuccess, onError);
    }

    @NonNull
    @Override
    public TranslateTextOperation<?> translateText(
            @NonNull String text,
            @NonNull TranslateTextOptions options,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().translateText(text, options, onSuccess, onError);
    }

    @NonNull
    @Override
    public TranslateTextOperation<?> translateText(
            @NonNull String text,
            @NonNull LanguageType fromLanguage,
            @NonNull LanguageType toLanguage,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().translateText(text, fromLanguage,
                toLanguage, onSuccess, onError);
    }

    @NonNull
    @Override
    public TranslateTextOperation<?> translateText(
            @NonNull String text,
            @NonNull LanguageType fromLanguage,
            @NonNull LanguageType toLanguage,
            @NonNull TranslateTextOptions options,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().translateText(text, fromLanguage,
                toLanguage, options, onSuccess, onError);
    }

    @NonNull
    @Override
    public SpeechToTextOperation<?> convertSpeechToText(
            @NonNull InputStream speech,
            @NonNull Consumer<SpeechToTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().convertSpeechToText(speech, onSuccess, onError);
    }

    @NonNull
    @Override
    public SpeechToTextOperation<?> convertSpeechToText(
            @NonNull InputStream speech,
            @NonNull SpeechToTextOptions options,
            @NonNull Consumer<SpeechToTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().convertSpeechToText(speech, options, onSuccess, onError);
    }

    @NonNull
    @Override
    public IdentifyOperation<?> identify(
            @NonNull IdentifyAction actionType,
            @NonNull Bitmap image,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().identify(actionType, image, onSuccess, onError);
    }

    @NonNull
    @Override
    public IdentifyOperation<?> identify(
            @NonNull IdentifyAction actionType,
            @NonNull Bitmap image,
            @NonNull IdentifyOptions options,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().identify(actionType, image, options, onSuccess, onError);
    }

    @NonNull
    @Override
    public InterpretOperation<?> interpret(
            @NonNull String text,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().interpret(text, onSuccess, onError);
    }

    @NonNull
    @Override
    public InterpretOperation<?> interpret(
            @NonNull String text,
            @NonNull InterpretOptions options,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return getSelectedPlugin().interpret(text, options, onSuccess, onError);
    }
}
