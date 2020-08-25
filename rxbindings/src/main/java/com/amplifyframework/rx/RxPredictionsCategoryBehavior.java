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

import io.reactivex.rxjava3.core.Single;

/**
 * An Rx-idiomatic expression of the {@link PredictionsCategoryBehavior}s.
 */
public interface RxPredictionsCategoryBehavior {
    /**
     * Converts the supplied text to speech.
     * @param text Text to convert
     * @return A single which emits {@link TextToSpeechResult} on success, or
     * {@link PredictionsException} on failure.
     */
    Single<TextToSpeechResult> convertTextToSpeech(
            @NonNull String text
    );

    /**
     * Converts the supplied text to speech.
     * @param text Text to convert
     * @param options Additional conversion optionss
     * @return A single which emits {@link TextToSpeechResult} on success, or
     * {@link PredictionsException} on failure.
     */
    Single<TextToSpeechResult> convertTextToSpeech(
            @NonNull String text,
            @NonNull TextToSpeechOptions options
    );

    /**
     * Translates a piece of text.
     * The target language is pulled from config.
     * @param text Text to translate
     * @return A single that emits {@link TranslateTextResult} on success,
     *         {@link PredictionsException} on failure
     */
    Single<TranslateTextResult> translateText(
            @NonNull String text
    );

    /**
     * Translates a piece of text.
     * The target language is pulled from config.
     * @param text Text to translate
     * @param options Text translation options
     * @return A single that emits {@link TranslateTextResult} on success,
     *         {@link PredictionsException} on failure
     */
    Single<TranslateTextResult> translateText(
            @NonNull String text,
            @NonNull TranslateTextOptions options
    );

    /**
     * Translates a piece of text.
     * @param text Text to translate
     * @param fromLanguage The language of the provided text
     * @param toLanguage The language to which to translate the text
     * @return A single that emits {@link TranslateTextResult} on success,
     *         {@link PredictionsException} on failure
     */
    Single<TranslateTextResult> translateText(
            @NonNull String text,
            @NonNull LanguageType fromLanguage,
            @NonNull LanguageType toLanguage
    );

    /**
     * Translates a piece of text.
     * @param text Text to translate
     * @param fromLanguage The language of the provided text
     * @param toLanguage The language to which to translate the text
     * @param options Additional text translation options
     * @return A single that emits {@link TranslateTextResult} on success,
     *         {@link PredictionsException} on failure
     */
    Single<TranslateTextResult> translateText(
            @NonNull String text,
            @NonNull LanguageType fromLanguage,
            @NonNull LanguageType toLanguage,
            @NonNull TranslateTextOptions options
    );

    /**
     * Identify features in a bitmap.
     * @param actionType Type of identification to run
     * @param image Image in which features will be detected
     * @return A Single which emits {@link IdentifyResult} on success, or
     *         {@link PredictionsException} on failure
     */
    Single<IdentifyResult> identify(
            @NonNull IdentifyAction actionType,
            @NonNull Bitmap image
    );

    /**
     * Identify features in a bitmap.
     * @param actionType Type of identification to run
     * @param image Image in which features will be detected
     * @param options Additional identification options
     * @return A Single which emits {@link IdentifyResult} on success, or
     *         {@link PredictionsException} on failure
     */
    Single<IdentifyResult> identify(
            @NonNull IdentifyAction actionType,
            @NonNull Bitmap image,
            @NonNull IdentifyOptions options
    );

    /**
     * Interpret a piece of text.
     * @param text Text to interpret
     * @return A single which emits {@link InterpretResult} on success,
     *         {@link PredictionsException} on failure
     */
    Single<InterpretResult> interpret(
            @NonNull String text
    );

    /**
     * Interpret a piece of text.
     * @param text Text to interpret
     * @param options Interpret options
     * @return A single which emits {@link InterpretResult} on success,
     *         {@link PredictionsException} on failure
     */
    Single<InterpretResult> interpret(
            @NonNull String text,
            @NonNull InterpretOptions options
    );
}
