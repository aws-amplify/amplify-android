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
 * Specifies the type of label detection to perform.
 */
public enum LabelType implements IdentifyAction {
    /**
     * Simple label provides information about a single object
     * or scenery found in an image.
     */
    LABELS,

    /**
     * Moderation label provides information about a single
     * type of unsafe content found in an image.
     */
    MODERATION_LABELS,

    /**
     * Detect and identifies both types of labels in an image.
     */
    ALL;

    /**
     * {@link LabelType} explicitly specifies the identification
     * type as a form of label detection. It will always return
     * {@link IdentifyActionType#DETECT_LABELS}.
     * @return {@link IdentifyActionType#DETECT_LABELS}
     */
    @Override
    @NonNull
    public final IdentifyActionType getType() {
        return IdentifyActionType.DETECT_LABELS;
    }
}
