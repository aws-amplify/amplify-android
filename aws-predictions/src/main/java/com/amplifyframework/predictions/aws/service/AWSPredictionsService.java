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

package com.amplifyframework.predictions.aws.service;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.aws.AWSPredictionsPluginConfiguration;
import com.amplifyframework.predictions.result.InterpretResult;

import com.amazonaws.services.comprehend.AmazonComprehendClient;

/**
 * Predictions service that makes inferences via AWS cloud computing.
 */
public final class AWSPredictionsService {

    private final AWSComprehendService comprehendService;

    /**
     * Constructs an instance of {@link AWSPredictionsService}.
     * @param configuration the configuration for AWS Predictions Plugin
     * @throws PredictionsException if any service fails to initialize
     */
    public AWSPredictionsService(@NonNull AWSPredictionsPluginConfiguration configuration) throws PredictionsException {
        this.comprehendService = new AWSComprehendService(configuration.getInterpretConfiguration());
    }

    public void comprehend(
            @NonNull String text,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        comprehendService.comprehend(text, onSuccess, onError);
    }

    /**
     * Return configured Amazon Comprehend client for
     * direct access to AWS endpoint.
     * @return the configured Amazon Comprehend client
     */
    @NonNull
    public AmazonComprehendClient getComprehendClient() {
        return comprehendService.getClient();
    }
}
