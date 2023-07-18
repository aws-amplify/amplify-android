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
import androidx.annotation.WorkerThread;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.result.InterpretResult;

import org.tensorflow.lite.Interpreter;

import java.util.HashMap;
import java.util.Map;

/**
 * Predictions service that uses TensorFlow Lite's
 * pre-trained models to make predictions offline.
 */
public final class TensorFlowPredictionsService {
    private static final Logger LOG = Amplify.Logging.logger(
        CategoryType.PREDICTIONS,
        "amplify:aws-predictions-tensorflow"
    );

    private final TensorFlowTextClassificationService textClassificationService;

    /**
     * Constructs an instance of {@link TensorFlowPredictionsService}.
     * @param context the Android context
     */
    public TensorFlowPredictionsService(@NonNull Context context) {
        this.textClassificationService = TensorFlowTextClassificationService.fromContext(context);
    }

    /**
     * Classifies a piece of text.
     * @param text Text to classify
     * @param onSuccess Invoked upon successful classification of the text
     * @param onError Invoked upon failure to classify the provided text, either
     *                due to issues with the text, or issues with the classification
     *                service
     */
    public void classify(
            @NonNull String text,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        textClassificationService.classify(text, onSuccess, onError);
    }

    /**
     * Load the models for each of the services. If any service fails
     * to properly load its assets, the error will be logged, but will
     * not be thrown.
     *
     * The plugin is NOT required to properly initialize any of its
     * services. It should only explicitly handle errors when an operation
     * is invoked from a service that is not initialized.
     */
    @WorkerThread
    public synchronized void loadAssets() {
        textClassificationService.loadIfNotLoaded();
    }

    /**
     * Terminates service and free up resources used by TensorFlow Lite.
     */
    @WorkerThread
    public synchronized void terminate() {
        textClassificationService.release();
    }

    /**
     * Return a map of TensorFlow Lite interpreters that are
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
