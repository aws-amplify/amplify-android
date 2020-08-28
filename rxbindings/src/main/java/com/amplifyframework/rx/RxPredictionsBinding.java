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

package com.amplifyframework.rx;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.predictions.PredictionsCategoryBehavior;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.models.IdentifyAction;
import com.amplifyframework.predictions.models.LanguageType;
import com.amplifyframework.predictions.options.IdentifyOptions;
import com.amplifyframework.predictions.options.InterpretOptions;
import com.amplifyframework.predictions.options.TextToSpeechOptions;
import com.amplifyframework.predictions.options.TranslateTextOptions;
import com.amplifyframework.predictions.result.IdentifyResult;
import com.amplifyframework.predictions.result.InterpretResult;
import com.amplifyframework.predictions.result.TextToSpeechResult;
import com.amplifyframework.predictions.result.TranslateTextResult;
import com.amplifyframework.rx.RxAdapters.VoidBehaviors;

import java.util.Objects;

import io.reactivex.rxjava3.core.Single;

final class RxPredictionsBinding implements RxPredictionsCategoryBehavior {
    private final PredictionsCategoryBehavior delegate;

    RxPredictionsBinding() {
        this(Amplify.Predictions);
    }

    @VisibleForTesting
    RxPredictionsBinding(@NonNull PredictionsCategoryBehavior delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public Single<TextToSpeechResult> convertTextToSpeech(@NonNull String text) {
        return toSingle((onResult, onError) ->
            delegate.convertTextToSpeech(text, onResult, onError));
    }

    @Override
    public Single<TextToSpeechResult> convertTextToSpeech(
            @NonNull String text,
            @NonNull TextToSpeechOptions options) {
        return toSingle((onResult, onError) ->
            delegate.convertTextToSpeech(text, options, onResult, onError));
    }

    @Override
    public Single<TranslateTextResult> translateText(@NonNull String text) {
        return toSingle((onResult, onError) -> delegate.translateText(text, onResult, onError));
    }

    @Override
    public Single<TranslateTextResult> translateText(
            @NonNull String text,
            @NonNull TranslateTextOptions options) {
        return toSingle((onResult, onError) -> delegate.translateText(text, options, onResult, onError));
    }

    @Override
    public Single<TranslateTextResult> translateText(
            @NonNull String text,
            @NonNull LanguageType fromLanguage,
            @NonNull LanguageType toLanguage) {
        return toSingle((onResult, onError) ->
            delegate.translateText(text, fromLanguage, toLanguage, onResult, onError));
    }

    @Override
    public Single<TranslateTextResult> translateText(
            @NonNull String text,
            @NonNull LanguageType fromLanguage,
            @NonNull LanguageType toLanguage,
            @NonNull TranslateTextOptions options) {
        return toSingle((onResult, onError) ->
            delegate.translateText(text, fromLanguage, toLanguage, options, onResult, onError));
    }

    @Override
    public Single<IdentifyResult> identify(@NonNull IdentifyAction actionType, @NonNull Bitmap image) {
        return toSingle((onResult, onError) -> delegate.identify(actionType, image, onResult, onError));
    }

    @Override
    public Single<IdentifyResult> identify(
            @NonNull IdentifyAction actionType,
            @NonNull Bitmap image,
            @NonNull IdentifyOptions options) {
        return toSingle((onResult, onError) ->
            delegate.identify(actionType, image, options, onResult, onError));
    }

    @Override
    public Single<InterpretResult> interpret(@NonNull String text) {
        return toSingle((onResult, onError) -> delegate.interpret(text, onResult, onError));
    }

    @Override
    public Single<InterpretResult> interpret(@NonNull String text, @NonNull InterpretOptions options) {
        return toSingle((onResult, onError) -> delegate.interpret(text, options, onResult, onError));
    }

    private static <T> Single<T> toSingle(VoidBehaviors.ResultEmitter<T, PredictionsException> behavior) {
        return VoidBehaviors.toSingle(behavior);
    }
}
