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
        return Single.create(emitter ->
            delegate.convertTextToSpeech(text, emitter::onSuccess, emitter::onError));
    }

    @Override
    public Single<TextToSpeechResult> convertTextToSpeech(
            @NonNull String text,
            @NonNull TextToSpeechOptions options) {
        return Single.create(emitter ->
            delegate.convertTextToSpeech(text, options, emitter::onSuccess, emitter::onError));
    }

    @Override
    public Single<TranslateTextResult> translateText(@NonNull String text) {
        return Single.create(emitter ->
            delegate.translateText(text, emitter::onSuccess, emitter::onError));
    }

    @Override
    public Single<TranslateTextResult> translateText(
            @NonNull String text,
            @NonNull TranslateTextOptions options) {
        return Single.create(emitter ->
            delegate.translateText(text, options, emitter::onSuccess, emitter::onError));
    }

    @Override
    public Single<TranslateTextResult> translateText(
            @NonNull String text,
            @NonNull LanguageType fromLanguage,
            @NonNull LanguageType toLanguage) {
        return Single.create(emitter ->
            delegate.translateText(text, fromLanguage, toLanguage, emitter::onSuccess, emitter::onError));
    }

    @Override
    public Single<TranslateTextResult> translateText(
            @NonNull String text,
            @NonNull LanguageType fromLanguage,
            @NonNull LanguageType toLanguage,
            @NonNull TranslateTextOptions options) {
        return Single.create(emitter ->
            delegate.translateText(text, fromLanguage, toLanguage, options, emitter::onSuccess, emitter::onError));
    }

    @Override
    public Single<IdentifyResult> identify(@NonNull IdentifyAction actionType, @NonNull Bitmap image) {
        return Single.create(emitter ->
            delegate.identify(actionType, image, emitter::onSuccess, emitter::onError));
    }

    @Override
    public Single<IdentifyResult> identify(
            @NonNull IdentifyAction actionType,
            @NonNull Bitmap image,
            @NonNull IdentifyOptions options) {
        return Single.create(emitter ->
            delegate.identify(actionType, image, options, emitter::onSuccess, emitter::onError));
    }

    @Override
    public Single<InterpretResult> interpret(@NonNull String text) {
        return Single.create(emitter -> delegate.interpret(text, emitter::onSuccess, emitter::onError));
    }

    @Override
    public Single<InterpretResult> interpret(@NonNull String text, @NonNull InterpretOptions options) {
        return Single.create(emitter -> delegate.interpret(text, options, emitter::onSuccess, emitter::onError));
    }
}
