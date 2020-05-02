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

package com.amplifyframework.testutils.sync;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;

import com.amplifyframework.predictions.PredictionsCategory;
import com.amplifyframework.predictions.PredictionsCategoryBehavior;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.models.IdentifyAction;
import com.amplifyframework.predictions.models.LanguageType;
import com.amplifyframework.predictions.options.IdentifyOptions;
import com.amplifyframework.predictions.options.InterpretOptions;
import com.amplifyframework.predictions.options.TranslateTextOptions;
import com.amplifyframework.predictions.result.IdentifyResult;
import com.amplifyframework.predictions.result.InterpretResult;
import com.amplifyframework.predictions.result.TranslateTextResult;
import com.amplifyframework.testutils.Await;

import java.util.concurrent.TimeUnit;

/**
 * A utility to perform synchronous calls to the {@link PredictionsCategory}.
 * This code is not well suited for production use, but is useful in test
 * code, where we want to make a series of sequential assertions after
 * performing various operations.
 */
public final class SynchronousPredictions {
    private static final long PREDICTIONS_OPERATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);
    private final PredictionsCategoryBehavior asyncDelegate;

    private SynchronousPredictions(PredictionsCategoryBehavior predictions) {
        this.asyncDelegate = predictions;
    }

    /**
     * Gets an instance of the Synchronous Predictions utility that
     * delegates tasks to the given asynchronous implementation of
     * Predictions behavior.
     * @param predictions an implementation of Predictions behavior
     * @return new instance of Synchronous Predictions
     */
    @NonNull
    public static synchronized SynchronousPredictions delegatingTo(@NonNull PredictionsCategoryBehavior predictions) {
        return new SynchronousPredictions(predictions);
    }

    /**
     * Translate given text synchronously using default options
     * and return the result of operation.
     * @param text the input text to translate
     * @return the result of translation containing translated text and
     *          the language of the translated text
     * @throws PredictionsException if translation fails or times out
     */
    @NonNull
    public TranslateTextResult translateText(
            @NonNull String text
    ) throws PredictionsException {
        return translateText(text, TranslateTextOptions.defaults());
    }

    /**
     * Translate given text synchronously and return the result of operation.
     * @param text the input text to translate
     * @param options interpret options
     * @return the result of translation containing translated text and
     *          the language of the translated text
     * @throws PredictionsException if translation fails or times out
     */
    @NonNull
    public TranslateTextResult translateText(
            @NonNull String text,
            @NonNull TranslateTextOptions options
    ) throws PredictionsException {
        return Await.<TranslateTextResult, PredictionsException>result(
            PREDICTIONS_OPERATION_TIMEOUT_MS,
            (onResult, onError) -> asyncDelegate.translateText(
                    text,
                    options,
                    onResult,
                    onError
            )
        );
    }

    /**
     * Translate given text synchronously using default options
     * and return the result of operation.
     * @param text the input text to translate
     * @param fromLanguage the language to translate from
     * @param toLanguage the language to translate to
     * @return the result of translation containing translated text and
     *          the language of the translated text
     * @throws PredictionsException if translation fails or times out
     */
    @NonNull
    public TranslateTextResult translateText(
            @NonNull String text,
            @NonNull LanguageType fromLanguage,
            @NonNull LanguageType toLanguage
    ) throws PredictionsException {
        return translateText(text, fromLanguage, toLanguage, TranslateTextOptions.defaults());
    }

    /**
     * Translate given text synchronously and return the result of operation.
     * @param text the input text to translate
     * @param fromLanguage the language to translate from
     * @param toLanguage the language to translate to
     * @param options interpret options
     * @return the result of translation containing translated text and
     *          the language of the translated text
     * @throws PredictionsException if translation fails or times out
     */
    @NonNull
    public TranslateTextResult translateText(
            @NonNull String text,
            @NonNull LanguageType fromLanguage,
            @NonNull LanguageType toLanguage,
            @NonNull TranslateTextOptions options
    ) throws PredictionsException {
        return Await.<TranslateTextResult, PredictionsException>result(
            PREDICTIONS_OPERATION_TIMEOUT_MS,
            (onResult, onError) -> asyncDelegate.translateText(
                    text,
                    fromLanguage,
                    toLanguage,
                    options,
                    onResult,
                    onError
            )
        );
    }

    /**
     * Analyze given image synchronously with default options and
     * return the result of operation.
     * @param actionType identification category type
     * @param image the input image to analyze
     * @return the result of image analysis
     * @throws PredictionsException if identify fails or times out
     */
    @NonNull
    public IdentifyResult identify(
            @NonNull IdentifyAction actionType,
            @NonNull Bitmap image
    ) throws PredictionsException {
        return identify(actionType, image, IdentifyOptions.defaults());
    }

    /**
     * Analyze given image synchronously and return the result of operation.
     * @param actionType identification category type
     * @param image the input image to detect analyze
     * @param options identify options
     * @return the result of image analysis
     * @throws PredictionsException if identify fails or times out
     */
    @NonNull
    public IdentifyResult identify(
            @NonNull IdentifyAction actionType,
            @NonNull Bitmap image,
            @NonNull IdentifyOptions options
    ) throws PredictionsException {
        return Await.<IdentifyResult, PredictionsException>result(
            PREDICTIONS_OPERATION_TIMEOUT_MS,
            (onResult, onError) -> asyncDelegate.identify(
                    actionType,
                    image,
                    options,
                    onResult,
                    onError
            )
        );
    }

    /**
     * Interpret given text synchronously using default options
     * and return the result of operation.
     * @param text the input text to analyze
     * @return the result of interpretation containing detected language,
     *          sentiment, key phrases, entities, syntax, and other features
     * @throws PredictionsException if interpret fails or times out
     */
    @NonNull
    public InterpretResult interpret(
            @NonNull String text
    ) throws PredictionsException {
        return interpret(text, InterpretOptions.defaults());
    }

    /**
     * Interpret given text synchronously and return the result of operation.
     * @param text the input text to analyze
     * @param options interpret options
     * @return the result of interpretation containing detected language,
     *          sentiment, key phrases, entities, syntax, and other features
     * @throws PredictionsException if interpret fails or times out
     */
    @NonNull
    public InterpretResult interpret(
            @NonNull String text,
            @NonNull InterpretOptions options
    ) throws PredictionsException {
        return Await.<InterpretResult, PredictionsException>result(
            PREDICTIONS_OPERATION_TIMEOUT_MS,
            (onResult, onError) -> asyncDelegate.interpret(
                    text,
                    options,
                    onResult,
                    onError
            )
        );
    }
}
