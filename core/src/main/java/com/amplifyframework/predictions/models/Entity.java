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
 * Class that holds the entity detection results
 * for the predictions category.
 */
public final class Entity {
    private final Rect boundingBox;
    private final List<Landmark> landmarks;
    private final AgeRange ageRange;
    private final List<Attribute> attributes;
    private final GenderAttribute gender;
    private final EntityMetadata metadata;
    private final List<Emotion> emotions;

    private Entity(
            @NonNull Rect boundingBox,
            @NonNull List<Landmark> landmarks,
            @Nullable AgeRange ageRange,
            @Nullable List<Attribute> attributes,
            @Nullable GenderAttribute gender,
            @NonNull EntityMetadata metadata,
            @Nullable List<Emotion> emotions
    ) {
        this.boundingBox = boundingBox;
        this.landmarks = landmarks;
        this.ageRange = ageRange;
        this.attributes = attributes;
        this.gender = gender;
        this.metadata = metadata;
        this.emotions = emotions;
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
     * @return the list of landmarks
     */
    @NonNull
    public List<Landmark> getLandmarks() {
        return landmarks;
    }

    /**
     * Gets the range of possible ages.
     * @return the range ages
     */
    @Nullable
    public AgeRange getAgeRange() {
        return ageRange;
    }

    /**
     * Gets the list of other attributes.
     * @return the list of attributes
     */
    @Nullable
    public List<Attribute> getAttributes() {
        return attributes;
    }

    /**
     * Gets the detected gender attribute.
     * @return the gender attribute
     */
    @Nullable
    public GenderAttribute getGender() {
        return gender;
    }

    /**
     * Gets the metadata.
     * @return the metadata
     */
    @NonNull
    public EntityMetadata getMetadata() {
        return metadata;
    }

    /**
     * Gets the list of detected emotions.
     * @return the list of emotions
     */
    @Nullable
    public List<Emotion> getEmotions() {
        return emotions;
    }

    /**
     * Gets a builder for entity.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Entity}.
     */
    public static class Builder {
        private Rect boundingBox;
        private List<Landmark> landmarks;
        private AgeRange ageRange;
        private List<Attribute> attributes;
        private GenderAttribute gender;
        private EntityMetadata metadata;
        private List<Emotion> emotions;

        /**
         * Sets the boundary and return this builder.
         * @param boundingBox the boundary
         * @return this builder instance
         */
        @NonNull
        public Builder boundingBox(@NonNull Rect boundingBox) {
            this.boundingBox = Objects.requireNonNull(boundingBox);
            return this;
        }

        /**
         * Sets the landmarks and return this builder.
         * @param landmarks the landmarks
         * @return this builder instance
         */
        @NonNull
        public Builder landmarks(@NonNull List<Landmark> landmarks) {
            this.landmarks = Objects.requireNonNull(landmarks);
            return this;
        }

        /**
         * Sets the age range and return this builder.
         * @param ageRange the age range
         * @return this builder instance
         */
        @NonNull
        public Builder ageRange(@Nullable AgeRange ageRange) {
            this.ageRange = ageRange;
            return this;
        }

        /**
         * Sets the attributes and return this builder.
         * @param attributes the attributes
         * @return this builder instance
         */
        @NonNull
        public Builder attributes(@Nullable List<Attribute> attributes) {
            this.attributes = attributes;
            return this;
        }

        /**
         * Sets the gender and return this builder.
         * @param gender the gender
         * @return this builder instance
         */
        @NonNull
        public Builder gender(@Nullable GenderAttribute gender) {
            this.gender = gender;
            return this;
        }

        /**
         * Sets the metadata and return this builder.
         * @param metadata the metadata
         * @return this builder instance
         */
        @NonNull
        public Builder metadata(@NonNull EntityMetadata metadata) {
            this.metadata = Objects.requireNonNull(metadata);
            return this;
        }

        /**
         * Sets the emotions and return this builder.
         * @param emotions the emotions
         * @return this builder instance
         */
        @NonNull
        public Builder emotions(@Nullable List<Emotion> emotions) {
            this.emotions = emotions;
            return this;
        }

        /**
         * Constructs a new instance of {@link Entity} from
         * the values assigned to this builder.
         * @return An instance of {@link Entity}
         */
        @NonNull
        public Entity build() {
            return new Entity(
                    Objects.requireNonNull(boundingBox),
                    Objects.requireNonNull(landmarks),
                    ageRange,
                    attributes,
                    gender,
                    Objects.requireNonNull(metadata),
                    emotions
            );
        }
    }
}
