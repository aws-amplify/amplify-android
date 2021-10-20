/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.geo.options;

import androidx.annotation.NonNull;

/**
 * Stores options to use when reverse-geocoding given coordinates.
 */
public class GeoSearchByCoordinatesOptions {
    private final int maxResults;

    /**
     * Constructs a search by coordinates options using the provided builder.
     *
     * @param builder the builder instance with the options parameters set.
     */
    protected GeoSearchByCoordinatesOptions(Builder builder) {
        this.maxResults = builder.maxResults;
    }

    /**
     * Returns the max results limit. Defaults to 50.
     *
     * @return the max results limit.
     */
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * Returns a new builder instance for constructing {@link GeoSearchByCoordinatesOptions}.
     *
     * @return a new builder instance.
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a new {@link GeoSearchByCoordinatesOptions} instance with default values.
     *
     * @return a default instance.
     */
    @NonNull
    public static GeoSearchByCoordinatesOptions defaults() {
        return builder().build();
    }

    /**
     * Builder class for conveniently constructing {@link GeoSearchByCoordinatesOptions} instance.
     */
    public static class Builder {
        private static final int DEFAULT_MAX_RESULTS_LIMIT = 50;

        private int maxResults = DEFAULT_MAX_RESULTS_LIMIT;

        /**
         * Sets the max results limit and returns itself.
         *
         * @param maxResults the max results to return for this search.
         * @return this builder instance.
         */
        @NonNull
        public Builder maxResults(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        /**
         * Constructs a new instance of {@link GeoSearchByCoordinatesOptions} using this builder.
         *
         * @return a {@link GeoSearchByCoordinatesOptions} instance with the properties of this builder.
         */
        @NonNull
        public GeoSearchByCoordinatesOptions build() {
            return new GeoSearchByCoordinatesOptions(this);
        }
    }
}
