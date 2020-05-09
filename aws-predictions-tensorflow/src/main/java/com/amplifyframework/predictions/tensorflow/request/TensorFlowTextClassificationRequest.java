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

package com.amplifyframework.predictions.tensorflow.request;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Simple request instance for text classification operation.
 */
public final class TensorFlowTextClassificationRequest {
    private final String text;

    /**
     * Constructs an instance of {@link TensorFlowTextClassificationRequest}.
     * @param text the text to classify
     */
    public TensorFlowTextClassificationRequest(@NonNull String text) {
        this.text = Objects.requireNonNull(text);
    }

    /**
     * Gets the text for which classification is being requested.
     * @return Contents of text on which to run classification
     */
    @NonNull
    public String getText() {
        return text;
    }
}
