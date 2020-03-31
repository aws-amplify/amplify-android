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

package com.amplifyframework.predictions.tensorflow.operation;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.operation.InterpretOperation;
import com.amplifyframework.predictions.tensorflow.request.TFLiteTextClassificationRequest;
import com.amplifyframework.predictions.result.InterpretResult;
import com.amplifyframework.predictions.tensorflow.service.TFLitePredictionsService;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Operation that uses pre-trained Tensorflow lite model to
 * interpret text in an offline state.
 */
public final class TFLiteInterpretOperation
        extends InterpretOperation<TFLiteTextClassificationRequest> {
    private final TFLitePredictionsService predictionsService;
    private final ExecutorService executorService;
    private final Consumer<InterpretResult> onSuccess;
    private final Consumer<PredictionsException> onError;

    /**
     * Constructs an instance of {@link TFLiteInterpretOperation}.
     * @param predictionsService instance of tflite service
     * @param executorService async task executor service
     * @param request predictions interpret request
     * @param onSuccess lambda to execute upon task completion
     * @param onError lambda to execute upon task failure
     */
    public TFLiteInterpretOperation(
            @NonNull TFLitePredictionsService predictionsService,
            @NonNull ExecutorService executorService,
            @NonNull TFLiteTextClassificationRequest request,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        super(Objects.requireNonNull(request));
        this.predictionsService = Objects.requireNonNull(predictionsService);
        this.executorService = Objects.requireNonNull(executorService);
        this.onSuccess = Objects.requireNonNull(onSuccess);
        this.onError = Objects.requireNonNull(onError);
    }

    @Override
    public void start() {
        executorService.execute(() -> predictionsService.classify(
                getRequest().getText(),
                onSuccess,
                onError)
        );
    }
}
