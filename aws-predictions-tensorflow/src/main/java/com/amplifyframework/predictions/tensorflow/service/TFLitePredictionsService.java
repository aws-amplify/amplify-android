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

package com.amplifyframework.predictions.tensorflow.service;

import android.content.Context;
import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.result.InterpretResult;

import org.tensorflow.lite.Interpreter;

import java.util.HashMap;
import java.util.Map;

/**
 * Predictions service that uses Tensorflow lite's
 * pre-trained models to make predictions offline.
 */
public final class TFLitePredictionsService {

    private final TFLiteTextClassificationService textClassificationService;

    /**
     * Constructs an instance of {@link TFLitePredictionsService}.
     * @param context Android context
     */
    public TFLitePredictionsService(@NonNull Context context) {
        this.textClassificationService = new TFLiteTextClassificationService(context);
    }

    public void classify(
            @NonNull String text,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        textClassificationService.classify(text, onSuccess, onError);
    }

    /**
     * Free up resources used by Tensorflow lite.
     */
    public void close() {
        textClassificationService.close();
    }

    /**
     * Return a map of Tensorflow lite interpreters that are
     * initialized with pre-trained models to fulfill their
     * respective services.
     * @return a map of service key to interpreter
     */
    public Map<String, Interpreter> getInterpreters() {
        Map<String, Interpreter> interpreters = new HashMap<>();
        interpreters.put(
                textClassificationService.getServiceKey(),
                textClassificationService.getInterpreter()
        );
        return interpreters;
    }
}
