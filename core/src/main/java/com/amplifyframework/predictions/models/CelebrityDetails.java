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

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Detailed metadata on a celebrity detection result.
 */
public final class CelebrityDetails {
    private final Celebrity celebrity;
    private final RectF box;
    private final Polygon polygon;
    private final Pose pose;
    private final List<Landmark> landmarks;
    private final List<URL> urls;

    private CelebrityDetails(final Builder builder) {
        this.celebrity = builder.getCelebrity();
        this.box = builder.getBox();
        this.polygon = builder.getPolygon();
        this.pose = builder.getPose();
        this.landmarks = builder.getLandmarks();
        this.urls = builder.getUrls();
    }

    /**
     * Gets the celebrity.
     * @return the celebrity
     */
    @NonNull
    public Celebrity getCelebrity() {
        return celebrity;
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
     * Gets the detected pose.
     * @return the pose
     */
    @Nullable
    public Pose getPose() {
        return pose;
    }

    /**
     * Gets the detected face details of the celebrity.
     * @return the facial features
     */
    @NonNull
    public List<Landmark> getLandmarks() {
        return Immutable.of(landmarks);
    }

    /**
     * Gets the list of URLs that contain
     * more information about the celebrity.
     * @return the urls
     */
    @NonNull
    public List<URL> getUrls() {
        return Immutable.of(urls);
    }

    /**
     * Gets the builder to help easily construct the
     * metadata object.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CelebrityDetails}.
     */
    public static final class Builder {
        private Celebrity celebrity;
        private RectF box;
        private Polygon polygon;
        private Pose pose;
        private List<Landmark> landmarks;
        private List<URL> urls;

        private Builder() {
            this.landmarks = Collections.emptyList();
            this.urls = Collections.emptyList();
        }

        /**
         * Sets the celebrity and return this builder.
         * @param celebrity the celebrity's name
         * @return this builder instance
         */
        @NonNull
        public Builder celebrity(@NonNull Celebrity celebrity) {
            this.celebrity = Objects.requireNonNull(celebrity);
            return this;
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
         * Sets the pose and return this builder.
         * @param pose the pose
         * @return this builder instance
         */
        @NonNull
        public Builder pose(@Nullable Pose pose) {
            this.pose = pose;
            return this;
        }

        /**
         * Sets the landmarks and return this builder.
         * @param landmarks the celebrity's entity landmarks
         * @return this builder instance
         */
        @NonNull
        public Builder landmarks(@NonNull List<Landmark> landmarks) {
            this.landmarks = Objects.requireNonNull(landmarks);
            return this;
        }

        /**
         * Sets the urls and return this builder.
         * @param urls the urls
         * @return this builder instance
         */
        @NonNull
        public Builder urls(@NonNull List<URL> urls) {
            this.urls = Objects.requireNonNull(urls);
            return this;
        }

        /**
         * Create a new instance of {@link CelebrityDetails} using
         * the values assigned to this builder instance.
         * @return An instance of {@link CelebrityDetails}
         */
        @NonNull
        public CelebrityDetails build() {
            return new CelebrityDetails(this);
        }

        @NonNull
        Celebrity getCelebrity() {
            return Objects.requireNonNull(celebrity);
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
        Pose getPose() {
            return pose;
        }

        @NonNull
        List<Landmark> getLandmarks() {
            return Objects.requireNonNull(landmarks);
        }

        @NonNull
        List<URL> getUrls() {
            return Objects.requireNonNull(urls);
        }
    }
}
