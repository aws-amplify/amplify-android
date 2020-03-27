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
 * Predictions category's text interpretation returns
 * inferences regarding specific portions of input text.
 *
 * {@link TargetText} contains details regarding the
 * parameters of the target text such as where it is
 * located.
 */
public final class TargetText {
    private final String text;
    private final int startIndex;
    private final int length;

    /**
     * Constructs a new instance of text position.
     * @param text the target text
     * @param startIndex the starting index of target text
     */
    public TargetText(
            @NonNull String text,
            int startIndex) {
        this.text = text;
        this.startIndex = startIndex;
        this.length = text.length();
    }

    /**
     * Gets the target text in question.
     * @return the target text
     */
    @NonNull
    public String getText() {
        return text;
    }

    /**
     * Gets the starting position of the target text
     * with respect to the full input text.
     * @return the starting index of target text
     */
    public int getStartIndex() {
        return startIndex;
    }

    /**
     * Gets the length of target text.
     * @return the length of target text
     */
    public int getLength() {
        return length;
    }
}
