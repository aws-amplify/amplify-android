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
 * Specifies the type of text detection to perform.
 */
public enum TextFormatType implements IdentifyAction {
    /**
     * Detect texts inside a fillable form.
     */
    FORM,

    /**
     * Detect textual information from an image presented in
     * the format of a 2-D table.
     */
    TABLE,

    /**
     * Detect regular text presented without any formatting.
     */
    PLAIN,

    /**
     * Detect and identifies all types of text from an image.
     */
    ALL;

    /**
     * {@link TextFormatType} explicitly specifies the identification
     * type as a form of optical character recognition. It will always
     * return {@link IdentifyActionType#DETECT_TEXT}.
     * @return {@link IdentifyActionType#DETECT_TEXT}
     */
    @NonNull
    @Override
    public final IdentifyActionType getType() {
        return IdentifyActionType.DETECT_TEXT;
    }
}
