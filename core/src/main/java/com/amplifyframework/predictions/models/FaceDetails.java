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
 * Holds the face detection results
 * for the predictions category.
 */
public final class FaceDetails {
    private final TargetBoundary boundary;
    private final AgeRange ageRange;
    private final PoseFeature pose;
    private final Gender gender;
    private final List<FacialFeature> facialFeatures;
    private final List<Emotion> emotions;
    private final List<BooleanFeature> features;

    private FaceDetails(final Builder builder) {
        this.boundary = builder.getBoundary();
        this.ageRange = builder.getAgeRange();
        this.pose = builder.getPose();
        this.gender = builder.getGender();
        this.facialFeatures = builder.getFacialFeatures();
        this.emotions = builder.getEmotions();
        this.features = builder.getFeatures();
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
     * Gets the detected pose feature.
     * @return the pose feature
     */
    @Nullable
    public PoseFeature getPose() {
        return pose;
    }

    /**
     * Gets the detected gender.
     * @return the gender type feature
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
    public List<FacialFeature> getFacialFeatures() {
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
     * Gets the list of boolean features.
     * @return the list of features
     */
    @NonNull
    public List<BooleanFeature> getFeatures() {
        return Immutable.of(features);
    }

    /**
     * Gets a builder for face detection result.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link FaceDetails}.
     */
    public static final class Builder {
        private TargetBoundary boundary;
        private AgeRange ageRange;
        private PoseFeature pose;
        private Gender gender;
        private List<FacialFeature> facialFeatures;
        private List<Emotion> emotions;
        private List<BooleanFeature> features;

        private Builder() {
            this.facialFeatures = Collections.emptyList();
            this.emotions = Collections.emptyList();
            this.features = Collections.emptyList();
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
         * Sets the pose feature and return this builder.
         * @param pose the pose
         * @return this builder instance
         */
        @NonNull
        public Builder pose(@Nullable PoseFeature pose) {
            this.pose = pose;
            return this;
        }

        /**
         * Sets the gender feature and return this builder.
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
         * Sets the list of boolean features and return this builder.
         * @param features the list of features
         * @return this builder instance
         */
        @NonNull
        public Builder features(@NonNull List<BooleanFeature> features) {
            this.features = Objects.requireNonNull(features);
            return this;
        }

        /**
         * Constructs a new instance of {@link FaceDetails} from
         * the values assigned to this builder.
         * @return An instance of {@link FaceDetails}
         */
        @NonNull
        public FaceDetails build() {
            return new FaceDetails(this);
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
        PoseFeature getPose() {
            return pose;
        }

        @Nullable
        Gender getGender() {
            return gender;
        }

        @NonNull
        List<FacialFeature> getFacialFeatures() {
            return Objects.requireNonNull(facialFeatures);
        }

        @NonNull
        List<Emotion> getEmotions() {
            return Objects.requireNonNull(emotions);
        }

        @NonNull
        List<BooleanFeature> getFeatures() {
            return Objects.requireNonNull(features);
        }
    }
}
