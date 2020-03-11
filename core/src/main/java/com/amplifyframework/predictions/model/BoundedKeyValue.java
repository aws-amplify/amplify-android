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

import android.graphics.Rect;
import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Class that holds the key-value detection results
 * for the predictions category.
 */
public final class BoundedKeyValue {
    private final String key;
    private final String value;
    private final Boolean isSelected;
    private final Rect boundingBox;
    private final Polygon polygon;

    private BoundedKeyValue(
            @NonNull String key,
            @NonNull String value,
            @NonNull Boolean isSelected,
            @NonNull Rect boundingBox,
            @NonNull Polygon polygon
    ) {
        this.key = key;
        this.value = value;
        this.isSelected = isSelected;
        this.boundingBox = boundingBox;
        this.polygon = polygon;
    }

    /**
     * Gets the detected key.
     * @return the key
     */
    @NonNull
    public String getKey() {
        return key;
    }

    /**
     * Gets the detected value.
     * @return the value
     */
    @NonNull
    public String getValue() {
        return value;
    }

    /**
     * Returns true if this key-value is selected.
     * @return true if this key-value is selected
     */
    @NonNull
    public Boolean isSelected() {
        return isSelected;
    }

    /**
     * Gets the rectangular boundary.
     * @return the rectangular bounding box
     */
    @NonNull
    public Rect getBoundingBox() {
        return boundingBox;
    }

    /**
     * Gets the polygonal boundary.
     * @return the polygon
     */
    @NonNull
    public Polygon getPolygon() {
        return polygon;
    }

    /**
     * Gets a builder to help easily construct
     * a bounded key-result object.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BoundedKeyValue}.
     */
    public static class Builder {
        private String key;
        private String value;
        private Boolean isSelected;
        private Rect boundingBox;
        private Polygon polygon;

        /**
         * Sets the key and return this builder.
         * @param key the key
         * @return this builder instance
         */
        @NonNull
        public Builder key(@NonNull String key) {
            this.key = Objects.requireNonNull(key);
            return this;
        }

        /**
         * Sets the value and return this builder.
         * @param value the value
         * @return this builder instance
         */
        @NonNull
        public Builder value(@NonNull String value) {
            this.value = Objects.requireNonNull(value);
            return this;
        }

        /**
         * Sets the flag for selected and return this builder.
         * @param isSelected the flag for whether this is selected
         * @return this builder instance
         */
        @NonNull
        public Builder isSelected(@NonNull Boolean isSelected) {
            this.isSelected = Objects.requireNonNull(isSelected);
            return this;
        }

        /**
         * Sets the rectangular boundary and return this builder.
         * @param boundingBox the bounding box
         * @return this builder instance
         */
        @NonNull
        public Builder boundingBox(@NonNull Rect boundingBox) {
            this.boundingBox = Objects.requireNonNull(boundingBox);
            return this;
        }

        /**
         * Sets the polygonal boundary and return this builder.
         * @param polygon the polygon
         * @return this builder instance
         */
        @NonNull
        public Builder polygon(@NonNull Polygon polygon) {
            this.polygon = Objects.requireNonNull(polygon);
            return this;
        }

        /**
         * Constructs a new instance of {@link BoundedKeyValue} using
         * the values assigned to this builder instance.
         * @return an instance of {@link BoundedKeyValue}
         */
        @NonNull
        public BoundedKeyValue build() {
            return new BoundedKeyValue(
                    Objects.requireNonNull(key),
                    Objects.requireNonNull(value),
                    Objects.requireNonNull(isSelected),
                    Objects.requireNonNull(boundingBox),
                    Objects.requireNonNull(polygon)
            );
        }
    }
}
