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

package com.amplifyframework.predictions.aws.operation;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.aws.request.AWSPollyRequest;
import com.amplifyframework.predictions.aws.service.AWSPredictionsService;
import com.amplifyframework.predictions.operation.TextToSpeechOperation;
import com.amplifyframework.predictions.result.TextToSpeechResult;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Operation that synthesize speech with cloud resources via
 * AWS Polly.
 */
public final class AWSTextToSpeechOperation
        extends TextToSpeechOperation<AWSPollyRequest> {
    private final AWSPredictionsService predictionsService;
    private final ExecutorService executorService;
    private final Consumer<TextToSpeechResult> onSuccess;
    private final Consumer<PredictionsException> onError;

    /**
     * Constructs an instance of {@link AWSTextToSpeechOperation}.
     * @param predictionsService instance of AWS predictions service
     * @param executorService async task executor service
     * @param request predictions translate request
     * @param onSuccess lambda to execute upon task completion
     * @param onError lambda to execute upon task failure
     */
    public AWSTextToSpeechOperation(
            @NonNull AWSPredictionsService predictionsService,
            @NonNull ExecutorService executorService,
            @NonNull AWSPollyRequest request,
            @NonNull Consumer<TextToSpeechResult> onSuccess,
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
        executorService.execute(() -> predictionsService.synthesizeSpeech(
                getRequest().getText(),
                getRequest().getVoiceType(),
                onSuccess,
                onError)
        );
    }
}
