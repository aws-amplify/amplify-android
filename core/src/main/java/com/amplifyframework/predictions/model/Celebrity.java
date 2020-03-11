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

import java.util.List;
import java.util.Objects;

/**
 * Class that holds the celebrity detection results
 * for the predictions category.
 */
public final class Celebrity {
    private final CelebrityMetadata metadata;
    private final Rect boundingBox;
    private final List<Landmark> landmarks;

    private Celebrity(
            @NonNull CelebrityMetadata metadata,
            @NonNull Rect boundingBox,
            @NonNull List<Landmark> landmarks
    ) {
        this.metadata = metadata;
        this.boundingBox = boundingBox;
        this.landmarks = landmarks;
    }

    /**
     * Gets the detected celebrity's metadata.
     * @return the celebrity metadata
     */
    @NonNull
    public CelebrityMetadata getMetadata() {
        return metadata;
    }

    /**
     * Gets the rectangular boundary.
     * @return the bounding box
     */
    @NonNull
    public Rect getBoundingBox() {
        return boundingBox;
    }

    /**
     * Gets the list of detected landmarks.
     * @return the landmarks
     */
    @NonNull
    public List<Landmark> getLandmarks() {
        return landmarks;
    }

    /**
     * Gets a builder to help easily construct
     * an instance of celebrity detection result.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Celebrity}.
     */
    public static class Builder {
        private CelebrityMetadata metadata;
        private Rect boundingBox;
        private List<Landmark> landmarks;

        /**
         * Sets the metadata and return this builder instance.
         * @param metadata the celebrity metadata
         * @return this builder instance
         */
        @NonNull
        public Builder metadata(@NonNull CelebrityMetadata metadata) {
            this.metadata = Objects.requireNonNull(metadata);
            return this;
        }

        /**
         * Sets the rectangular boundary and return this builder instance.
         * @param boundingBox the bounding box
         * @return this builder instance
         */
        @NonNull
        public Builder boundingBox(@NonNull Rect boundingBox) {
            this.boundingBox = Objects.requireNonNull(boundingBox);
            return this;
        }

        /**
         * Sets the landmarks and return this builder instance.
         * @param landmarks the landmarks
         * @return this builder instance
         */
        @NonNull
        public Builder landmarks(@NonNull List<Landmark> landmarks) {
            this.landmarks = Objects.requireNonNull(landmarks);
            return this;
        }

        /**
         * Create a new instance of {@link Celebrity} using the
         * values assigned to this builder instance.
         * @return An instance of {@link Celebrity}
         */
        @NonNull
        public Celebrity build() {
            return new Celebrity(
                    Objects.requireNonNull(metadata),
                    Objects.requireNonNull(boundingBox),
                    Objects.requireNonNull(landmarks)
            );
        }
    }
}
