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
import androidx.annotation.Nullable;

import com.amplifyframework.util.Immutable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Holds the entity detection results
 * for the predictions category.
 */
public final class EntityDetails {
    private final RectF box;
    private final Polygon polygon;
    private final AgeRange ageRange;
    private final PoseFeature pose;
    private final Gender gender;
    private final List<Landmark> landmarks;
    private final List<Emotion> emotions;
    private final List<BinaryFeature> features;

    private EntityDetails(final Builder builder) {
        this.box = builder.getBox();
        this.polygon = builder.getPolygon();
        this.ageRange = builder.getAgeRange();
        this.pose = builder.getPose();
        this.gender = builder.getGender();
        this.landmarks = builder.getLandmarks();
        this.emotions = builder.getEmotions();
        this.features = builder.getValues();
    }

    /**
     * Gets the rectangular target boundary if available.
     * @return the rectangular boundary
     */
    @Nullable
    public RectF getBox() {
        return box;
    }

    /**
     * Gets a more finely defined target boundary if available.
     * @return the polygonal boundary
     */
    @Nullable
    public Polygon getPolygon() {
        return polygon;
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
     * Gets the list of detected landmarks.
     * @return the list of landmarks
     */
    @NonNull
    public List<Landmark> getLandmarks() {
        return Immutable.of(landmarks);
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
    public List<BinaryFeature> getValues() {
        return Immutable.of(features);
    }

    /**
     * Gets a builder for entity detection result.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link EntityDetails}.
     */
    public static final class Builder {
        private RectF box;
        private Polygon polygon;
        private AgeRange ageRange;
        private PoseFeature pose;
        private Gender gender;
        private List<Landmark> landmarks;
        private List<Emotion> emotions;
        private List<BinaryFeature> features;

        private Builder() {
            this.landmarks = Collections.emptyList();
            this.emotions = Collections.emptyList();
            this.features = Collections.emptyList();
        }

        /**
         * Sets the bounding box and return this builder.
         * @param box the rectangular boundary
         * @return this builder instance
         */
        @NonNull
        public Builder box(@Nullable RectF box) {
            this.box = box;
            return this;
        }

        /**
         * Sets the bounding polygon and return this builder.
         * @param polygon the polygonal boundary
         * @return this builder instance
         */
        @NonNull
        public Builder polygon(@Nullable Polygon polygon) {
            this.polygon = polygon;
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
        public Builder features(@NonNull List<BinaryFeature> features) {
            this.features = Objects.requireNonNull(features);
            return this;
        }

        /**
         * Constructs a new instance of {@link EntityDetails} from
         * the values assigned to this builder.
         * @return An instance of {@link EntityDetails}
         */
        @NonNull
        public EntityDetails build() {
            return new EntityDetails(this);
        }

        @Nullable
        RectF getBox() {
            return box;
        }

        @Nullable
        Polygon getPolygon() {
            return polygon;
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
        List<Landmark> getLandmarks() {
            return Objects.requireNonNull(landmarks);
        }

        @NonNull
        List<Emotion> getEmotions() {
            return Objects.requireNonNull(emotions);
        }

        @NonNull
        List<BinaryFeature> getValues() {
            return Objects.requireNonNull(features);
        }
    }
}
