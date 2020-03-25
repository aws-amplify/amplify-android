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
 * Represents a selection from an image. It contains
 * the bounding geometry of a selection and an
 * indicator to regard its selection state.
 *
 * Defaults to deselected state until explicitly selected.
 */
public final class Selection {
    private final TargetBoundary boundary;

    private boolean selected;

    /**
     * Constructs an instance of {@link Selection} using
     * rectangular boundary.
     * @param box the bounding box
     */
    public Selection(@NonNull Rect box) {
        this(TargetBoundary.builder().box(box).build());
    }

    /**
     * Constructs an instance of {@link Selection} using
     * polygonal boundary.
     * @param polygon the bounding polygon
     */
    public Selection(@NonNull Polygon polygon) {
        this(TargetBoundary.builder().polygon(polygon).build());
    }

    /**
     * Constructs an instance of {@link Selection} using
     * target boundary.
     * @param boundary the target boundary
     */
    public Selection(@NonNull TargetBoundary boundary) {
        this.boundary = boundary;
        this.selected = false;
    }

    /**
     * Gets the bounding geometry.
     * @return the target boundary
     */
    @NonNull
    public TargetBoundary getBoundary() {
        return boundary;
    }

    /**
     * Gets the rectangular boundary. It fetches the
     * box portion of registered target boundary.
     * @return the bounding box
     */
    @Nullable
    public Rect getBox() {
        return boundary.getBox();
    }

    /**
     * Gets the polygonal boundary. It fetches the
     * polygon portion of registered target boundary.
     * @return the polygon
     */
    @Nullable
    public Polygon getPolygon() {
        return boundary.getPolygon();
    }

    /**
     * Returns true if this selection is selected.
     * @return true if this selection is selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Activates the selection flag.
     */
    public void select() {
        this.selected = true;
    }

    /**
     * Deactivates the selection flag.
     */
    public void deselect() {
        this.selected = false;
    }
}
