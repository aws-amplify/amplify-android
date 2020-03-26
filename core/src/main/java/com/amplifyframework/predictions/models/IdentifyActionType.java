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

package com.amplifyframework.predictions.models;

import androidx.annotation.NonNull;

/**
 * The types of image identification supported by the
 * Predictions category.
 */
public enum IdentifyActionType implements IdentifyAction {
    /**
     * Identify a famous celebrity from an image.
     */
    DETECT_CELEBRITY,

    /**
     * Identify a specific label from an image.
     */
    DETECT_LABELS,

    /**w
     * Identify a face from an image.
     */
    DETECT_FACES,

    /**
     * Identify text from an image.
     */
    DETECT_TEXT;

    @NonNull
    @Override
    public final IdentifyActionType getType() {
        return this;
    }
}
