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
import com.amplifyframework.predictions.operation.SpeechToTextOperation;
import com.amplifyframework.predictions.tensorflow.request.TensorFlowUnsupportedRequest;

import java.util.Objects;

/**
 * Operation to transcribe speech to text in an offline state is
 * currently not supported. This operation will immediately trigger
 * error callback upon being started.
 */
public final class TensorFlowSpeechToTextOperation
        extends SpeechToTextOperation<TensorFlowUnsupportedRequest> {
    private final Consumer<PredictionsException> onError;

    /**
     * Constructs an instance of {@link TensorFlowSpeechToTextOperation}.
     * @param onError lambda to execute upon task start
     */
    public TensorFlowSpeechToTextOperation(@NonNull Consumer<PredictionsException> onError) {
        super(new TensorFlowUnsupportedRequest());
        this.onError = Objects.requireNonNull(onError);
    }

    @Override
    public void start() {
        onError.accept(getRequest().getError());
    }
}
