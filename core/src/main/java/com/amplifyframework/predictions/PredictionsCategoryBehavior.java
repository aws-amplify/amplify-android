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
 * Predictions category defines an abstract high-level behavior
 * of machine learning tools.
 */
public interface PredictionsCategoryBehavior {
    /**
     * Translate the text to the language specified.
     * @param text The text to translate
     * @param language The language of the given text
     * @param targetLanguage The language to which the text should be translated
     * @param onSuccess Triggered upon successful translation
     * @param onError Triggered upon encountering error
     * @return The predictions operation object that can be used to directly access
     *          the ongoing translation operation
     */
    @NonNull
    PredictionsTranslateTextOperation<?> translateText(
            @NonNull String text,
            @NonNull LanguageType language,
            @NonNull LanguageType targetLanguage,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    );

    /**
     * Translate the text to the language specified.
     * @param text The text to translate
     * @param language The language of the given text
     * @param targetLanguage The language to which the text should be translated
     * @param options Parameters to specific plugin behavior
     * @param onSuccess Triggered upon successful translation
     * @param onError Triggered upon encountering error
     * @return The predictions operation object that can be used to directly access
     *          the ongoing translation operation
     */
    @NonNull
    PredictionsTranslateTextOperation<?> translateText(
            @NonNull String text,
            @NonNull LanguageType language,
            @NonNull LanguageType targetLanguage,
            @NonNull PredictionsTranslateTextOptions options,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    );

    /**
     * Synthesize an audio output from the text input.
     * @param text The text to be synthesized to audio
     * @param onSuccess Triggered upon successful conversion
     * @param onError Triggered upon encountering error
     * @return The predictions operation object that can be used to directly access
     *          the ongoing conversion operation
     */
    @NonNull
    PredictionsTextToSpeechOperation<?> convertTextToSpeech(
            @NonNull String text,
            @NonNull Consumer<TextToSpeechResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    );

    /**
     * Synthesize an audio output from the text input.
     * @param text The text to be synthesized to audio
     * @param options Parameters to specific plugin behavior
     * @param onSuccess Triggered upon successful conversion
     * @param onError Triggered upon encountering error
     * @return The predictions operation object that can be used to directly access
     *          the ongoing conversion operation
     */
    @NonNull
    PredictionsTextToSpeechOperation<?> convertTextToSpeech(
            @NonNull String text,
            @NonNull PredictionsTextToSpeechOptions options,
            @NonNull Consumer<TextToSpeechResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    );

    /**
     * Transcribe a text output from the audio input.
     * @param speech The url of the audio to be transcribed
     * @param onSuccess Triggered upon successful conversion
     * @param onError Triggered upon encountering error
     * @return The predictions operation object that can be used to directly access
     *          the ongoing conversion operation
     */
    @NonNull
    PredictionsSpeechToTextOperation<?> convertSpeechToText(
            @NonNull URL speech,
            @NonNull Consumer<SpeechToTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    );

    /**
     * Transcribe a text output from the audio input.
     * @param speech The url of the audio to be transcribed
     * @param options Parameters to specific plugin behavior
     * @param onSuccess Triggered upon successful conversion
     * @param onError Triggered upon encountering error
     * @return The predictions operation object that can be used to directly access
     *          the ongoing conversion operation
     */
    @NonNull
    PredictionsSpeechToTextOperation<?> convertSpeechToText(
            @NonNull URL speech,
            @NonNull PredictionsSpeechToTextOptions options,
            @NonNull Consumer<SpeechToTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    );

    /**
     * Detect and identify a target from a given image.
     * @param type The type of image detection to be performed
     * @param image The image being identified
     * @param onSuccess Triggered upon successful conversion
     * @param onError Triggered upon encountering error
     * @return The predictions operation object that can be used to directly access
     *          the ongoing identification operation
     */
    @NonNull
    PredictionsIdentifyOperation<?> identify(
            @NonNull IdentifyAction type,
            @NonNull URL image,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    );

    /**
     * Detect and identify a target from a given image.
     * @param type The type of image detection to be performed
     * @param image The image being identified
     * @param options Parameters to specific plugin behavior
     * @param onSuccess Triggered upon successful conversion
     * @param onError Triggered upon encountering error
     * @return The predictions operation object that can be used to directly access
     *          the ongoing identification operation
     */
    @NonNull
    PredictionsIdentifyOperation<?> identify(
            @NonNull IdentifyAction type,
            @NonNull URL image,
            @NonNull PredictionsIdentifyOptions options,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    );

    /**
     * Interpret the text to detect and analyze associated sentiments, entities,
     * language, syntax, and key phrases.
     * @param text The text to interpret
     * @param onSuccess Triggered upon successful conversion
     * @param onError Triggered upon encountering error
     * @return The predictions operation object that can be used to directly access
     *          the ongoing interpretation operation
     */
    @NonNull
    PredictionsInterpretOperation<?> interpret(
            @NonNull String text,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    );

    /**
     * Interpret the text to detect and analyze associated sentiments, entities,
     * language, syntax, and key phrases.
     * @param text The text to interpret
     * @param options Parameters to specific plugin behavior
     * @param onSuccess Triggered upon successful conversion
     * @param onError Triggered upon encountering error
     * @return The predictions operation object that can be used to directly access
     *          the ongoing interpretation operation
     */
    @NonNull
    PredictionsInterpretOperation<?> interpret(
            @NonNull String text,
            @NonNull PredictionsInterpretOptions options,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    );
}
