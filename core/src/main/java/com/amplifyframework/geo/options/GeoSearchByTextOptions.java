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
import androidx.annotation.Nullable;

import com.amplifyframework.geo.models.CountryCode;
import com.amplifyframework.geo.models.SearchArea;
import com.amplifyframework.util.Immutable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Stores options to use when searching search index by text query.
 */
public class GeoSearchByTextOptions {
    private final int maxResults;
    private final SearchArea searchArea;
    private final List<CountryCode> countries;

    /**
     * Constructs a search by text options using the provided builder.
     *
     * @param builder the builder instance with the options parameters set.
     */
    protected GeoSearchByTextOptions(Builder builder) {
        this.maxResults = builder.maxResults;
        this.searchArea = builder.searchArea;
        this.countries = builder.countries;
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
     * Returns the search area parameter.
     *
     * @return the search area parameter.
     */
    @Nullable
    public SearchArea getSearchArea() {
        return searchArea;
    }

    /**
     * Returns the list of countries.
     *
     * @return the list of countries.
     */
    @NonNull
    public List<CountryCode> getCountries() {
        return Immutable.of(countries);
    }

    /**
     * Returns a new builder instance for constructing {@link GeoSearchByTextOptions}.
     *
     * @return a new builder instance.
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a new {@link GeoSearchByTextOptions} instance with default values.
     *
     * @return a default instance.
     */
    @NonNull
    public static GeoSearchByTextOptions defaults() {
        return builder().build();
    }

    /**
     * Builder class for conveniently constructing {@link GeoSearchByTextOptions} instance.
     */
    public static class Builder {
        private static final int DEFAULT_MAX_RESULTS_LIMIT = 50;

        private int maxResults = DEFAULT_MAX_RESULTS_LIMIT;
        private SearchArea searchArea;
        private List<CountryCode> countries;

        /**
         * Instantiates a new builder for search options.
         */
        protected Builder() {
            // Default to filtering inside USA
            this.countries = Collections.singletonList(CountryCode.USA);
        }

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
         * Sets the search area and returns itself.
         *
         * @param searchArea the search area parameter.
         * @return this builder instance.
         */
        @NonNull
        public Builder searchArea(@NonNull SearchArea searchArea) {
            this.searchArea = Objects.requireNonNull(searchArea);
            return this;
        }

        /**
         * Sets the list of countries to search from and returns itself.
         * List cannot be empty.
         *
         * @param countries the list of countries to search from.
         * @return this builder instance.
         */
        @NonNull
        public Builder countries(@NonNull List<CountryCode> countries) {
            this.countries = Objects.requireNonNull(countries);
            if (countries.isEmpty()) {
                throw new IllegalArgumentException("Country filter cannot be empty.");
            }
            return this;
        }

        /**
         * Constructs a new instance of {@link GeoSearchByTextOptions} using this builder.
         *
         * @return a {@link GeoSearchByTextOptions} instance with the properties of this builder.
         */
        @NonNull
        public GeoSearchByTextOptions build() {
            return new GeoSearchByTextOptions(this);
        }
    }
}
