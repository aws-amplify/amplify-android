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

import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Class that holds the line detection results for a
 * string of text for the predictions category.
 */
public final class IdentifiedLine extends IdentifiedText {

    private IdentifiedLine(
            @NonNull String text,
            @NonNull Rect boundingBox,
            @Nullable Polygon polygon,
            @Nullable Integer page
    ) {
        super(text, boundingBox, polygon, page);
    }

    /**
     * Gets a builder to help easily construct the
     * identified line object.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to help easily construct an instance
     * of {@link IdentifiedLine}.
     */
    public static final class Builder extends IdentifiedText.Builder<Builder, IdentifiedLine> {
        @NonNull
        @Override
        public IdentifiedLine build() {
            return new IdentifiedLine(
                    getText(),
                    getBoundingBox(),
                    getPolygon(),
                    getPage()
            );
        }
    }
}
