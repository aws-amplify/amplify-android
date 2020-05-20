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
import com.amplifyframework.predictions.aws.request.AWSImageIdentifyRequest;
import com.amplifyframework.predictions.aws.service.AWSPredictionsService;
import com.amplifyframework.predictions.models.IdentifyAction;
import com.amplifyframework.predictions.operation.IdentifyOperation;
import com.amplifyframework.predictions.result.IdentifyResult;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Operation that identifies objects within an image via
 * Amazon Translate and Amazon Rekognition.
 */
public final class AWSIdentifyOperation
        extends IdentifyOperation<AWSImageIdentifyRequest> {
    private final AWSPredictionsService predictionsService;
    private final ExecutorService executorService;
    private final Consumer<IdentifyResult> onSuccess;
    private final Consumer<PredictionsException> onError;

    /**
     * Constructs an instance of {@link AWSIdentifyOperation}.
     * @param predictionsService instance of AWS predictions service
     * @param executorService async task executor service
     * @param actionType the type of identification action
     * @param request predictions identify request
     * @param onSuccess lambda to execute upon task completion
     * @param onError lambda to execute upon task failure
     */
    public AWSIdentifyOperation(
            @NonNull AWSPredictionsService predictionsService,
            @NonNull ExecutorService executorService,
            @NonNull IdentifyAction actionType,
            @NonNull AWSImageIdentifyRequest request,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        super(actionType, Objects.requireNonNull(request));
        this.predictionsService = Objects.requireNonNull(predictionsService);
        this.executorService = Objects.requireNonNull(executorService);
        this.onSuccess = Objects.requireNonNull(onSuccess);
        this.onError = Objects.requireNonNull(onError);
    }

    @Override
    public void start() {
        switch (getIdentifyAction().getType()) {
            case DETECT_CELEBRITIES:
                startCelebritiesDetection();
                return;
            case DETECT_LABELS:
                startLabelsDetection();
                return;
            case DETECT_ENTITIES:
                startEntitiesDetection();
                return;
            case DETECT_TEXT:
                startTextDetection();
                return;
            default:
        }
    }

    private void startCelebritiesDetection() {
        executorService.execute(() -> predictionsService.recognizeCelebrities(
                getRequest().getImageData(),
                onSuccess,
                onError
        ));
    }

    private void startLabelsDetection() {
        executorService.execute(() -> predictionsService.detectLabels(
                getIdentifyAction(),
                getRequest().getImageData(),
                onSuccess,
                onError
        ));
    }

    private void startEntitiesDetection() {
        executorService.execute(() -> predictionsService.detectEntities(
                getRequest().getImageData(),
                onSuccess,
                onError
        ));
    }

    private void startTextDetection() {
        executorService.execute(() -> predictionsService.detectText(
                getIdentifyAction(),
                getRequest().getImageData(),
                onSuccess,
                onError
        ));
    }
}
