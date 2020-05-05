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

package com.amplifyframework.predictions.operation;

import androidx.annotation.Nullable;

import com.amplifyframework.core.async.AmplifyOperation;
import com.amplifyframework.core.category.CategoryType;

/**
 *  Abstract representation of a operation that converts text to speech.
 *
 * @param <R> type of the request object
 */
public abstract class TextToSpeechOperation<R> extends AmplifyOperation<R> {
    /**
     * Constructs a new {@link TextToSpeechOperation}.
     * @param amplifyOperationRequest The request object of the operation
     */
    public TextToSpeechOperation(@Nullable R amplifyOperationRequest) {
        super(CategoryType.PREDICTIONS, amplifyOperationRequest);
    }
}
