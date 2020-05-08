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

import android.graphics.PointF;
import androidx.annotation.NonNull;

import com.amplifyframework.util.Immutable;

import java.util.List;
import java.util.Objects;

/**
 * A class to represent more complex shapes than a simple
 * rectangular box.
 */
public final class Polygon {
    private static final int MINIMUM_POINTS_REQUIRED = 3;

    private final List<PointF> points;

    private Polygon(List<PointF> points) {
        this.points = points;
    }

    /**
     * Gets the polygon vertices.
     * @return the polygon vertices
     */
    @NonNull
    public List<PointF> getPoints() {
        return Immutable.of(points);
    }

    /**
     * Construct a new immutable instance of polygon from a
     * list of points.
     * @param points a list of points representing the corners
     *               of a polygon object
     * @return A polygon instance
     */
    @NonNull
    public static Polygon fromPoints(@NonNull List<PointF> points) {
        Objects.requireNonNull(points);
        if (points.size() < MINIMUM_POINTS_REQUIRED) {
            throw new IllegalArgumentException("A polygon must " +
                    "contain at least three points.");
        }
        return new Polygon(points);
    }
}
