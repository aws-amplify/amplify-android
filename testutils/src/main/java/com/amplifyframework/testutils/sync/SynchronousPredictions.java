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

import androidx.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.predictions.PredictionsCategory;
import com.amplifyframework.predictions.PredictionsCategoryBehavior;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.options.InterpretOptions;
import com.amplifyframework.predictions.result.InterpretResult;
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

    private final PredictionsCategoryBehavior predictions;

    private SynchronousPredictions(PredictionsCategoryBehavior predictions) {
        this.predictions = predictions;
    }

    /**
     * Gets an instance of the Synchronous Predictions utility that
     * delegates tasks to Amplify.Predictions.
     * @return new instance of Synchronous Predictions
     */
    @NonNull
    public static synchronized SynchronousPredictions create() {
        return create(Amplify.Predictions);
    }

    /**
     * Gets an instance of the Synchronous Predictions utility that
     * delegates tasks to the given asynchronous implementation of
     * predictions behavior.
     * @return new instance of Synchronous Predictions
     */
    @NonNull
    public static synchronized SynchronousPredictions create(@NonNull PredictionsCategoryBehavior predictions) {
        return new SynchronousPredictions(predictions);
    }

    /**
     * Interpret given text synchronously and return the result of operation.
     * @param text the input text to analyze
     * @param options interpret options
     * @return the result of interpretation containing detected language,
     *          sentiment, key phrases, entities, syntax, and other features
     * @throws PredictionsException if interpret fail or times out
     */
    @NonNull
    public InterpretResult interpret(
            @NonNull String text,
            @NonNull InterpretOptions options
    ) throws PredictionsException {
        return Await.<InterpretResult, PredictionsException>result(
            PREDICTIONS_OPERATION_TIMEOUT_MS,
            (onResult, onError) -> predictions.interpret(
                    text,
                    options,
                    onResult,
                    onError
            )
        );
    }
}
