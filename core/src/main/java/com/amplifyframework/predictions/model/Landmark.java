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

package com.amplifyframework.predictions.model;

import android.graphics.Point;
import androidx.annotation.NonNull;

import java.util.List;
import java.util.Objects;

/**
 * Class that holds the landmark detection results
 * for the predictions category.
 */
public final class Landmark {
    private final LandmarkType type;
    private final List<Point> points;

    private Landmark(
            @NonNull LandmarkType type,
            @NonNull List<Point> points
    ) {
        this.type = type;
        this.points = points;
    }

    /**
     * Gets the type of detected landmark.
     * @return the landmark type
     */
    @NonNull
    public LandmarkType getType() {
        return type;
    }

    /**
     * Gets the list of points.
     * @return the points
     */
    @NonNull
    public List<Point> getPoints() {
        return points;
    }

    /**
     * Gets a builder to help easily construct a
     * landmark detection result object.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Landmark}.
     */
    public static class Builder {
        private LandmarkType type;
        private List<Point> points;

        /**
         * Sets the landmark type and return this builder.
         * @param type the landmark type
         * @return this builder instance
         */
        @NonNull
        public Builder type(@NonNull LandmarkType type) {
            this.type = Objects.requireNonNull(type);
            return this;
        }

        /**
         * Sets the points and return this builder.
         * @param points the list of points
         * @return this builder instance
         */
        @NonNull
        public Builder points(@NonNull List<Point> points) {
            this.points = Objects.requireNonNull(points);
            return this;
        }

        /**
         * Construct a new instance of {@link Landmark} from
         * the values assigned to this builder instance.
         * @return An instance of {@link Landmark}
         */
        @NonNull
        public Landmark build() {
            return new Landmark(
                    Objects.requireNonNull(type),
                    Objects.requireNonNull(points)
            );
        }
    }
}
