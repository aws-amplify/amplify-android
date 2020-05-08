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
import com.amplifyframework.predictions.aws.request.AWSTranscribeRequest;
import com.amplifyframework.predictions.aws.service.AWSPredictionsService;
import com.amplifyframework.predictions.operation.SpeechToTextOperation;
import com.amplifyframework.predictions.result.SpeechToTextResult;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Operation that transcribes speech to text with cloud
 * resources via Amazon Transcribe.
 */
public final class AWSSpeechToTextOperation
        extends SpeechToTextOperation<AWSTranscribeRequest> {
    private final AWSPredictionsService predictionsService;
    private final ExecutorService executorService;
    private final Consumer<SpeechToTextResult> onSuccess;
    private final Consumer<PredictionsException> onError;

    /**
     * Constructs an instance of {@link AWSSpeechToTextOperation}.
     * @param predictionsService instance of AWS predictions service
     * @param executorService async task executor service
     * @param request predictions transcribe request
     * @param onSuccess lambda to execute upon task completion
     * @param onError lambda to execute upon task failure
     */
    public AWSSpeechToTextOperation(
            @NonNull AWSPredictionsService predictionsService,
            @NonNull ExecutorService executorService,
            @NonNull AWSTranscribeRequest request,
            @NonNull Consumer<SpeechToTextResult> onSuccess,
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
        executorService.execute(() -> onError.accept(new PredictionsException(
                "This operation is not supported.",
                "Operation is not currently supported by online mode."
        )));
    }
}
