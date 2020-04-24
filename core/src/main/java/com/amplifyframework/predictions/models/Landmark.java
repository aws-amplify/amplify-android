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
 * Representation of specific entity landmark type and
 * all of the points that represent it in the input image.
 */
public final class Landmark {
    private final LandmarkType type;
    private final List<PointF> points;

    /**
     * Constructs an instance of {@link Landmark}.
     * @param type the landmark type
     * @param points the list of all the points that match
     */
    public Landmark(@NonNull LandmarkType type, @NonNull List<PointF> points) {
        this.type = Objects.requireNonNull(type);
        this.points = Objects.requireNonNull(points);
    }

    /**
     * Gets the type of landmark represented by this object.
     * @return the landmark type enum
     */
    @NonNull
    public LandmarkType getType() {
        return type;
    }

    /**
     * Gets the list of all the points that represents this
     * landmark type.
     * @return the points that fall into this landmark type
     */
    @NonNull
    public List<PointF> getPoints() {
        return Immutable.of(points);
    }
}
