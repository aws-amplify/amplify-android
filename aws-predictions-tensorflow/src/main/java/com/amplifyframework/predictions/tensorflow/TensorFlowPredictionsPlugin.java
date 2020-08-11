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

package com.amplifyframework.predictions.tensorflow;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.PredictionsPlugin;
import com.amplifyframework.predictions.models.IdentifyAction;
import com.amplifyframework.predictions.models.LanguageType;
import com.amplifyframework.predictions.operation.IdentifyOperation;
import com.amplifyframework.predictions.operation.InterpretOperation;
import com.amplifyframework.predictions.operation.TextToSpeechOperation;
import com.amplifyframework.predictions.operation.TranslateTextOperation;
import com.amplifyframework.predictions.options.IdentifyOptions;
import com.amplifyframework.predictions.options.InterpretOptions;
import com.amplifyframework.predictions.options.TextToSpeechOptions;
import com.amplifyframework.predictions.options.TranslateTextOptions;
import com.amplifyframework.predictions.result.IdentifyResult;
import com.amplifyframework.predictions.result.InterpretResult;
import com.amplifyframework.predictions.result.TextToSpeechResult;
import com.amplifyframework.predictions.result.TranslateTextResult;
import com.amplifyframework.predictions.tensorflow.operation.TensorFlowIdentifyOperation;
import com.amplifyframework.predictions.tensorflow.operation.TensorFlowInterpretOperation;
import com.amplifyframework.predictions.tensorflow.operation.TensorFlowTextToSpeechOperation;
import com.amplifyframework.predictions.tensorflow.operation.TensorFlowTranslateTextOperation;
import com.amplifyframework.predictions.tensorflow.request.TensorFlowTextClassificationRequest;
import com.amplifyframework.predictions.tensorflow.service.TensorFlowPredictionsService;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A plugin for Predictions category that uses models from
 * TensorFlow Lite to carry out tasks offline.
 */
public final class TensorFlowPredictionsPlugin extends PredictionsPlugin<TensorFlowPredictionsEscapeHatch> {
    private static final String TENSOR_FLOW_PREDICTIONS_PLUGIN_KEY = "tensorFlowPredictionsPlugin";

    private final ExecutorService executorService;

    private TensorFlowPredictionsService predictionsService;

    /**
     * Construct an instance of Predictions Plugin that uses
     * pre-trained models from TensorFlow Lite to make inferences
     * offline.
     */
    public TensorFlowPredictionsPlugin() {
        this.executorService = Executors.newCachedThreadPool();
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return TENSOR_FLOW_PREDICTIONS_PLUGIN_KEY;
    }

    @Override
    public void configure(
            JSONObject pluginConfiguration,
            @NonNull Context context
    ) throws AmplifyException {
        this.predictionsService = new TensorFlowPredictionsService(context);
    }

    @WorkerThread
    @Override
    public void initialize(@NonNull Context context) {
        this.predictionsService.loadAssets();
    }

    @NonNull
    @Override
    public TensorFlowPredictionsEscapeHatch getEscapeHatch() {
        return new TensorFlowPredictionsEscapeHatch(predictionsService.getInterpreters());
    }

    @NonNull
    @Override
    public String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @NonNull
    @Override
    public TextToSpeechOperation<?> convertTextToSpeech(
            @NonNull String text,
            @NonNull Consumer<TextToSpeechResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return convertTextToSpeech(text, TextToSpeechOptions.defaults(), onSuccess, onError);
    }

    @NonNull
    @Override
    public TextToSpeechOperation<?> convertTextToSpeech(
            @NonNull String text,
            @NonNull TextToSpeechOptions options,
            @NonNull Consumer<TextToSpeechResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        TensorFlowTextToSpeechOperation operation =
                new TensorFlowTextToSpeechOperation(onError);
        operation.start();
        return operation;
    }

    @NonNull
    @Override
    public TranslateTextOperation<?> translateText(
            @NonNull String text,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return translateText(text, TranslateTextOptions.defaults(), onSuccess, onError);
    }

    @NonNull
    @Override
    public TranslateTextOperation<?> translateText(
            @NonNull String text,
            @NonNull TranslateTextOptions options,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        TensorFlowTranslateTextOperation operation =
                new TensorFlowTranslateTextOperation(onError);
        operation.start();
        return operation;
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
        return translateText(text, fromLanguage, toLanguage,
                TranslateTextOptions.defaults(), onSuccess, onError);
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
        TensorFlowTranslateTextOperation operation =
                new TensorFlowTranslateTextOperation(onError);
        operation.start();
        return operation;
    }

    @NonNull
    @Override
    public IdentifyOperation<?> identify(
            @NonNull IdentifyAction actionType,
            @NonNull Bitmap image,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return identify(actionType, image, IdentifyOptions.defaults(), onSuccess, onError);
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
        TensorFlowIdentifyOperation operation =
                new TensorFlowIdentifyOperation(actionType, onError);
        operation.start();
        return operation;
    }

    @NonNull
    @Override
    public InterpretOperation<?> interpret(
            @NonNull String text,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return interpret(text, InterpretOptions.defaults(), onSuccess, onError);
    }

    @NonNull
    @Override
    public InterpretOperation<?> interpret(
            @NonNull String text,
            @NonNull InterpretOptions options,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        // Create interpret request for TensorFlow Lite interpreter
        TensorFlowTextClassificationRequest request =
                new TensorFlowTextClassificationRequest(text);

        TensorFlowInterpretOperation operation = new TensorFlowInterpretOperation(
                predictionsService,
                executorService,
                request,
                onSuccess,
                onError
        );

        // Start operation and return
        operation.start();
        return operation;
    }
}
