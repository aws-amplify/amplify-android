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

package com.amplifyframework.geo.result;

import androidx.annotation.NonNull;

import com.amplifyframework.geo.models.Place;
import com.amplifyframework.util.Immutable;

import java.util.List;
import java.util.Objects;

/**
 * Results object containing a list of places from querying a search.
 */
public final class GeoSearchResult {
    private final List<Place> places;

    private GeoSearchResult(List<Place> places) {
        this.places = places;
    }

    /**
     * Constructs an immutable search result with given places.
     *
     * @param places the list of places from search.
     * @return a search result object.
     */
    @NonNull
    public static GeoSearchResult withPlaces(@NonNull List<Place> places) {
        return new GeoSearchResult(Objects.requireNonNull(places));
    }

    /**
     * Returns the list of places returned by search.
     *
     * @return the list of places.
     */
    @NonNull
    public List<Place> getPlaces() {
        return Immutable.of(places);
    }
}
