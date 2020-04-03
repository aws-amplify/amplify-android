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

package com.amplifyframework.predictions;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.operation.InterpretOperation;
import com.amplifyframework.predictions.options.InterpretOptions;
import com.amplifyframework.predictions.result.InterpretResult;

/**
 * The Predictions category includes functionality to convert and translate text,
 * perform text analysis, and detect features in an image, using Machine Learning.
 */
public interface PredictionsCategoryBehavior {
    /**
     * Interpret the text to detect and analyze associated sentiments, entities,
     * language, syntax, and key phrases.
     * @param text The text to interpret
     * @param onSuccess Triggered upon successful conversion
     * @param onError Triggered upon encountering error
     * @return The predictions operation object that can be used to directly access
     *          the ongoing interpretation operation
     */
    @NonNull
    InterpretOperation<?> interpret(
            @NonNull String text,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    );

    /**
     * Interpret the text to detect and analyze associated sentiments, entities,
     * language, syntax, and key phrases.
     * @param text The text to interpret
     * @param options Parameters to specific plugin behavior
     * @param onSuccess Triggered upon successful conversion
     * @param onError Triggered upon encountering error
     * @return The predictions operation object that can be used to directly access
     *          the ongoing interpretation operation
     */
    @NonNull
    InterpretOperation<?> interpret(
            @NonNull String text,
            @NonNull InterpretOptions options,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    );
}
