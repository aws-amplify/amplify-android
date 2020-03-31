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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.PredictionsPlugin;
import com.amplifyframework.predictions.operation.InterpretOperation;
import com.amplifyframework.predictions.tensorflow.operation.TensorFlowInterpretOperation;
import com.amplifyframework.predictions.options.InterpretOptions;
import com.amplifyframework.predictions.tensorflow.request.TensorFlowTextClassificationRequest;
import com.amplifyframework.predictions.result.InterpretResult;
import com.amplifyframework.predictions.tensorflow.service.TensorFlowPredictionsService;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A plugin for Predictions category that uses models from
 * TensorFlow Lite to carry out tasks offline.
 */
public final class TensorFlowPredictionsPlugin extends PredictionsPlugin<TensorFlowPredictionsEscapeHatch> {
    private static final String TFL_PREDICTIONS_PLUGIN_KEY = "tflPredictionsPlugin";

    private final ExecutorService executorService;

    private TensorFlowPredictionsService predictionsService;

    public TensorFlowPredictionsPlugin() {
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
        this.predictionsService = new TensorFlowPredictionsService(context);
    }

    @Nullable
    @Override
    public TensorFlowPredictionsEscapeHatch getEscapeHatch() {
        return new TensorFlowPredictionsEscapeHatch(predictionsService.getInterpreters());
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
