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

import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Holds the details of a detected label inside an image.
 * Each detected item has a label within a hierarchical taxonomy.
 * For example, an object labeled as "car" may have parental labels
 * of "vehicle" and "transportation".
 */
public final class Label extends ImageFeature<String> {
    /**
     * Feature type for {@link Label}.
     */
    public static final String FEATURE_TYPE = Label.class.getSimpleName();

    private final List<String> parentLabels;
    private final List<RectF> boxes;

    private Label(final Builder builder) {
        super(builder);
        this.parentLabels = builder.getParentLabels();
        this.boxes = builder.getBoxes();
    }

    @Override
    @NonNull
    public String getTypeAlias() {
        return FEATURE_TYPE;
    }

    /**
     * Returns the name of detected label. This is simply
     * returns the stored feature value.
     * @return the name of detected label
     */
    @NonNull
    public String getName() {
        return getValue();
    }

    /**
     * Gets the list of parent labels' names. They are all
     * of the ancestor labels of the detected label.
     * @return the list of names of parent labels
     */
    @NonNull
    public List<String> getParentLabels() {
        return parentLabels;
    }

    /**
     * Gets the list of bounding boxes containing objects for
     * this label.
     * @return the list of bounding boxes
     */
    @NonNull
    public List<RectF> getBoxes() {
        return boxes;
    }

    /**
     * Gets a builder to construct label feature.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Label}.
     */
    public static final class Builder extends ImageFeature.Builder<Builder, Label, String> {
        private List<String> parentLabels;
        private List<RectF> boxes;

        private Builder() {
            this.parentLabels = new ArrayList<>();
            this.boxes = new ArrayList<>();
        }

        /**
         * Sets the name of label as feature and return this builder.
         * @param name the label name
         * @return this builder instance
         */
        @NonNull
        public Builder name(@NonNull String name) {
            return super.value(name);
        }

        /**
         * Sets the list of parent labels and return this builder.
         * @param parentLabels the names of parent labels
         * @return this builder instance
         */
        @NonNull
        public Builder parentLabels(@NonNull List<String> parentLabels) {
            this.parentLabels = Objects.requireNonNull(parentLabels);
            return this;
        }

        /**
         * Sets the list of bounding boxes and return this builder.
         * @param boxes the list of bounding boxes
         * @return this builder instance
         */
        @NonNull
        public Builder boxes(@NonNull List<RectF> boxes) {
            this.boxes = Objects.requireNonNull(boxes);
            return this;
        }

        /**
         * Construct a new instance of {@link Label} from
         * the values assigned to this builder.
         * @return An instance of {@link Label}
         */
        @NonNull
        public Label build() {
            return new Label(this);
        }

        @NonNull
        List<String> getParentLabels() {
            return Objects.requireNonNull(Immutable.of(parentLabels));
        }

        @NonNull
        List<RectF> getBoxes() {
            return Objects.requireNonNull(Immutable.of(boxes));
        }
    }
}
