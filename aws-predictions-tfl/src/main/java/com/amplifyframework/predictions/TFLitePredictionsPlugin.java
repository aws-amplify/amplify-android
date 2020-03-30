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

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.operation.InterpretOperation;
import com.amplifyframework.predictions.operation.TFLiteInterpretOperation;
import com.amplifyframework.predictions.options.InterpretOptions;
import com.amplifyframework.predictions.request.TFLiteTextClassificationRequest;
import com.amplifyframework.predictions.result.InterpretResult;
import com.amplifyframework.predictions.service.TFLitePredictionsService;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A plugin for Predictions category that uses models from
 * Tensorflow lite to carry out tasks offline.
 */
public final class TFLitePredictionsPlugin extends PredictionsPlugin<TFLitePredictionsEscapeHatch> {
    private static final String TFL_PREDICTIONS_PLUGIN_KEY = "tflPredictionsPlugin";

    private final ExecutorService executorService;

    private TFLitePredictionsService predictionsService;

    public TFLitePredictionsPlugin() {
        this.executorService = Executors.newCachedThreadPool();
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return TFL_PREDICTIONS_PLUGIN_KEY;
    }

    @Override
    public void configure(
            @NonNull JSONObject pluginConfiguration,
            @NonNull Context context
    ) throws AmplifyException {
        this.predictionsService = new TFLitePredictionsService(context);
    }

    @Nullable
    @Override
    public TFLitePredictionsEscapeHatch getEscapeHatch() {
        return new TFLitePredictionsEscapeHatch();
    }

    @NonNull
    @Override
    public InterpretOperation<?> interpret(
            @NonNull String text,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        final InterpretOptions options = InterpretOptions.defaultInstance();
        return interpret(text, options, onSuccess, onError);
    }

    @NonNull
    @Override
    public InterpretOperation<?> interpret(
            @NonNull String text,
            @NonNull InterpretOptions options,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        // Create interpret request for Tensorflow Lite interpreter
        TFLiteTextClassificationRequest request =
                new TFLiteTextClassificationRequest(text);

        TFLiteInterpretOperation operation = new TFLiteInterpretOperation(
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
