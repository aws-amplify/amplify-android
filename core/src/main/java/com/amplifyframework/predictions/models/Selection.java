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

import android.graphics.RectF;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Represents a selection from an image. It contains
 * the bounding geometry of a selection and an
 * indicator to regard its selection state.
 *
 * Defaults to deselected state until explicitly selected.
 */
public final class Selection {
    private final RectF box;
    private final Polygon polygon;

    private boolean selected;

    private Selection(RectF box, Polygon polygon) {
        this.box = box;
        this.polygon = polygon;
    }

    /**
     * Constructs an instance of {@link Selection} using
     * rectangular boundary.
     * @param box the bounding box
     * @return {@link Selection} instance containing a box
     */
    public static Selection fromBox(@NonNull RectF box) {
        return new Selection(Objects.requireNonNull(box), null);
    }

    /**
     * Constructs an instance of {@link Selection} using
     * polygonal boundary.
     * @param polygon the bounding polygon
     * @return {@link Selection} instance containing a polygon
     */
    public static Selection fromPolygon(@NonNull Polygon polygon) {
        return new Selection(null, Objects.requireNonNull(polygon));
    }

    /**
     * Gets the rectangular boundary. It fetches the
     * box portion of registered target boundary.
     * @return the bounding box
     */
    @Nullable
    public RectF getBox() {
        return box;
    }

    /**
     * Gets the polygonal boundary. It fetches the
     * polygon portion of registered target boundary.
     * @return the polygon
     */
    @Nullable
    public Polygon getPolygon() {
        return polygon;
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
