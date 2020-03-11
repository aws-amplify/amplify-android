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

import java.util.List;
import java.util.Objects;

/**
 * Class that holds the label identification results
 * for the predictions category.
 */
public final class Label {
    private final String name;
    private final LabelMetadata metadata;
    private final List<Rect> boundingBoxes;

    private Label(
            @NonNull String name,
            @Nullable LabelMetadata metadata,
            @Nullable List<Rect> boundingBoxes
    ) {
        this.name = name;
        this.metadata = metadata;
        this.boundingBoxes = boundingBoxes;
    }

    /**
     * Gets the name of the label.
     * @return the label name
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Gets the label's metadata.
     * @return the metadata
     */
    @Nullable
    public LabelMetadata getMetadata() {
        return metadata;
    }

    /**
     * Gets the rectangular boundary.
     * @return the bounding box
     */
    @Nullable
    public List<Rect> getBoundingBoxes() {
        return boundingBoxes;
    }

    /**
     * Gets the builder for constructing label.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Label}.
     */
    public static class Builder {
        private String name;
        private LabelMetadata metadata;
        private List<Rect> boundingBoxes;

        /**
         * Sets the name and return this builder.
         * @param name the name
         * @return this builder instance
         */
        @NonNull
        public Builder name(@NonNull String name) {
            this.name = Objects.requireNonNull(name);
            return this;
        }

        /**
         * Sets the metadata and return this builder.
         * @param metadata the metadata
         * @return this builder instance
         */
        @NonNull
        public Builder metadata(@Nullable LabelMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Sets the list of boundaries and return this builder.
         * @param boundingBoxes the bounding boxes
         * @return this builder instance
         */
        @NonNull
        public Builder boundingBoxes(@Nullable List<Rect> boundingBoxes) {
            this.boundingBoxes = boundingBoxes;
            return this;
        }

        /**
         * Constructs a new instance of {@link Label} from the
         * values assigned to this builder instance.
         * @return An instance of {@link Label}
         */
        @NonNull
        public Label build() {
            return new Label(
                    Objects.requireNonNull(name),
                    metadata,
                    boundingBoxes
            );
        }
    }
}
