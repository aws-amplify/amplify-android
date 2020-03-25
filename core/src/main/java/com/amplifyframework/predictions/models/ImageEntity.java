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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.util.Immutable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Holds the image entity detection results
 * for the predictions category.
 */
public final class ImageEntity {
    private final TargetBoundary boundary;
    private final AgeRange ageRange;
    private final PoseAttribute pose;
    private final Gender gender;
    private final List<FacialFeature> facialFeatures;
    private final List<Emotion> emotions;
    private final List<BooleanAttribute> attributes;

    private ImageEntity(final Builder builder) {
        this.boundary = builder.getBoundary();
        this.ageRange = builder.getAgeRange();
        this.pose = builder.getPose();
        this.gender = builder.getGender();
        this.facialFeatures = builder.getfacialFeatures();
        this.emotions = builder.getEmotions();
        this.attributes = builder.getAttributes();
    }

    /**
     * Gets the target boundary.
     * @return the bounding geometry
     */
    @NonNull
    public TargetBoundary getBoundary() {
        return boundary;
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
     * Gets the detected pose attribute.
     * @return the pose attribute
     */
    @Nullable
    public PoseAttribute getPose() {
        return pose;
    }

    /**
     * Gets the detected gender.
     * @return the gender type attribute
     */
    @Nullable
    public Gender getGender() {
        return gender;
    }

    /**
     * Gets the list of detected facial features.
     * @return the list of facial features
     */
    @NonNull
    public List<FacialFeature> getfacialFeatures() {
        return Immutable.of(facialFeatures);
    }

    /**
     * Gets the list of detected emotions.
     * @return the list of emotions
     */
    @NonNull
    public List<Emotion> getEmotions() {
        return Immutable.of(emotions);
    }

    /**
     * Gets the list of boolean attributes.
     * @return the list of attributes
     */
    @NonNull
    public List<BooleanAttribute> getAttributes() {
        return Immutable.of(attributes);
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
     * Builder for {@link ImageEntity}.
     */
    public static final class Builder {
        private TargetBoundary boundary;
        private AgeRange ageRange;
        private PoseAttribute pose;
        private Gender gender;
        private List<FacialFeature> facialFeatures;
        private List<Emotion> emotions;
        private List<BooleanAttribute> attributes;

        private Builder() {
            this.facialFeatures = Collections.emptyList();
            this.emotions = Collections.emptyList();
            this.attributes = Collections.emptyList();
        }

        /**
         * Sets the bounding geometry and return this builder.
         * @param boundary the boundary
         * @return this builder instance
         */
        @NonNull
        public Builder boundary(@NonNull TargetBoundary boundary) {
            this.boundary = Objects.requireNonNull(boundary);
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
         * Sets the pose attribute and return this builder.
         * @param pose the pose
         * @return this builder instance
         */
        @NonNull
        public Builder pose(@Nullable PoseAttribute pose) {
            this.pose = pose;
            return this;
        }

        /**
         * Sets the gender attribute and return this builder.
         * @param gender the gender
         * @return this builder instance
         */
        @NonNull
        public Builder gender(@Nullable Gender gender) {
            this.gender = gender;
            return this;
        }

        /**
         * Sets the facial features and return this builder.
         * @param facialFeatures the facial features
         * @return this builder instance
         */
        @NonNull
        public Builder facialFeatures(@NonNull List<FacialFeature> facialFeatures) {
            this.facialFeatures = Objects.requireNonNull(facialFeatures);
            return this;
        }

        /**
         * Sets the emotions and return this builder.
         * @param emotions the emotions
         * @return this builder instance
         */
        @NonNull
        public Builder emotions(@NonNull List<Emotion> emotions) {
            this.emotions = Objects.requireNonNull(emotions);
            return this;
        }

        /**
         * Sets the list of boolean attributes and return this builder.
         * @param attributes the list of attributes
         * @return this builder instance
         */
        @NonNull
        public Builder attributes(@NonNull List<BooleanAttribute> attributes) {
            this.attributes = Objects.requireNonNull(attributes);
            return this;
        }

        /**
         * Constructs a new instance of {@link ImageEntity} from
         * the values assigned to this builder.
         * @return An instance of {@link ImageEntity}
         */
        @NonNull
        public ImageEntity build() {
            return new ImageEntity(this);
        }

        @NonNull
        TargetBoundary getBoundary() {
            return Objects.requireNonNull(boundary);
        }

        @Nullable
        AgeRange getAgeRange() {
            return ageRange;
        }

        @Nullable
        PoseAttribute getPose() {
            return pose;
        }

        @Nullable
        Gender getGender() {
            return gender;
        }

        @NonNull
        List<FacialFeature> getfacialFeatures() {
            return Objects.requireNonNull(facialFeatures);
        }

        @NonNull
        List<Emotion> getEmotions() {
            return Objects.requireNonNull(emotions);
        }

        @NonNull
        List<BooleanAttribute> getAttributes() {
            return Objects.requireNonNull(attributes);
        }
    }
}
